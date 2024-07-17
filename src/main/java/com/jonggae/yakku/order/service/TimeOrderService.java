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
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// 실제 예약구매에서만 사용할 클래스

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeOrderService {

    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final RedissonClient redissonClient;

    // 실패 요청 처리
    private final ExecutorService executorService = Executors.newFixedThreadPool(100);
    private final AtomicBoolean stockExhausted = new AtomicBoolean(false);

    private static final String LOCK_PREFIX = "lock:product:";
    private static final long LOCK_WAIT_TIME = 1000;
    private static final long LOCK_LEASE_TIME = 5000;

    @Transactional
    public OrderDto createTimeOrder(Long customerId, Long productId, Long quantity) {
        if (stockExhausted.get()) {
            throw new InsufficientStockException("재고가 모두 소진되었습니다");
        }
        // 상품 정보 조회 (락 없이 수행)
        ProductDto product = productClient.getProductOrderInfo(productId);

        RLock lock = redissonClient.getLock(LOCK_PREFIX + productId);
        boolean stockReserved = false;

        try {
            // 락 획득 시도 (재고 확인 및 감소에만 적용)
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                try {
                    stockReserved = productClient.reserveStock(productId, new StockReservationRequestDto(quantity));
                    if (!stockReserved) {
                        throw new InsufficientStockException("재고가 부족합니다");
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                throw new ConcurrentOrderException("다른 주문 처리 중");
            }
            if (!stockReserved) {
                stockExhausted.set(true);
                throw new InsufficientStockException("재고가 부족합니다");
            }

            // 주문 생성 (락 없이 수행)
            Order order = createOrder(customerId, product, quantity);
            Order savedOrder = orderRepository.save(order);

            // 결제 처리 (락 없이 수행)
            PaymentResult paymentResult = processPayment(savedOrder.getId());

            // 주문 상태 업데이트 (락 없이 수행)
            updateOrderStatus(savedOrder, paymentResult, productId, quantity);

            Order updatedOrder = orderRepository.save(savedOrder);
            return OrderDto.from(updatedOrder);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentOrderException("락 획득 중 인터럽트 발생");
        } catch (FeignException e) {
            log.error("외부 서비스 통신 간 오류", e);
            if (stockReserved) {
                productClient.cancelStockReservation(productId, quantity);
            }
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

    public CompletableFuture<OrderDto> createTimeOrderAsync(Long customerId, Long productId, Long quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createTimeOrder(customerId, productId, quantity);
            } catch (Exception e) {
                log.error("주문 처리 중 오류 발생", e);
                throw new CompletionException(e);
            }
        }, executorService);
    }

    public void resetStockStatus() {
        stockExhausted.set(false);
    }

    // ExecutorService 종료 메소드 추가
    @PreDestroy
    public void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}

