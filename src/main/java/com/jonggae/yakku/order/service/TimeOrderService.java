package com.jonggae.yakku.order.service;


import com.jonggae.yakku.exceptions.ConcurrentOrderException;
import com.jonggae.yakku.exceptions.ExternalServiceException;
import com.jonggae.yakku.exceptions.InsufficientStockException;
import com.jonggae.yakku.order.controller.PaymentClient;
import com.jonggae.yakku.order.controller.ProductClient;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.dto.PaymentRequestDto;
import com.jonggae.yakku.order.dto.PaymentResult;
import com.jonggae.yakku.order.dto.ProductDto;
import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderItem;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.feignDto.StockReservationRequestDto;
import com.jonggae.yakku.order.repository.OrderRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

// 실제 예약구매에서만 사용할 클래스

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeOrderService {

    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;


    private static final String LOCK_PREFIX = "lock:product:";
    private static final long LOCK_WAIT_TIME = 2000;
    private static final long LOCK_LEASE_TIME = 5000;

    @Transactional
    public OrderDto createTimeOrder(Long customerId, Long productId, Long quantity) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + productId);

        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new ConcurrentOrderException("다른 주문 처리 중");
            }

            return processOrder(customerId, productId, quantity);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentOrderException("락 획득 중 인터럽트 발생");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private OrderDto processOrder(Long customerId, Long productId, Long quantity) {
        try {
            // 상품 정보 조회
            ProductDto product = productClient.getProductOrderInfo(productId);
            //재고 확인 및 감소
            boolean stockReserved = productClient.reserveStock(productId, new StockReservationRequestDto(quantity));
            if (!stockReserved) {
                throw new InsufficientStockException("재고가 부족합니다");
            }

            //주문 생성
            Order order = createOrder(customerId, product, quantity);
            Order savedOrder = orderRepository.save(order);

            // 결제 처리
            PaymentResult paymentResult = processPayment(savedOrder.getId());

            // 주문 상태 업데이트
            updateOrderStatus(savedOrder, paymentResult, productId, quantity);

            Order updatedOrder = orderRepository.save(savedOrder);
            return OrderDto.from(updatedOrder);

        } catch (FeignException e) {
            log.error("외부 서비스 통신 간 오류", e);
            productClient.cancelStockReservation(productId, quantity);
            throw new ExternalServiceException("외부 서비스 통신 중 오류가 발생했습니다");
        }
    }

    private Order createOrder(Long customerId, ProductDto product, Long quantity) {
        Order order = Order.builder()
                .customerId(customerId)
                .orderDate(LocalDateTime.now())
                .orderStatus(OrderStatus.PENDING_ORDER)
                .orderItemList(new ArrayList<>())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(quantity)
                .price(product.getPrice())
                .order(order)
                .build();

        order.addOrderItem(orderItem);
        return order;
    }

    private PaymentResult processPayment(Long orderId) {
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(orderId);
        return paymentClient.processPayment(paymentRequestDto);
    }

    private void updateOrderStatus(Order order, PaymentResult paymentResult, Long productId, Long quantity) {
        if (paymentResult == PaymentResult.SUCCESS) {
            order.setOrderStatus(OrderStatus.PAYMENT_COMPLETE);
            productClient.confirmStockReservation(productId, quantity);
        } else {
            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            productClient.cancelStockReservation(productId, quantity);
        }
    }
}

