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
import com.jonggae.yakku.order.repository.OrderItemRepository;
import com.jonggae.yakku.order.repository.OrderRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

// 실제 예약구매에서만 사용할 클래스

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeOrderService {

    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;

    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public OrderDto createTimeOrder(Long customerId, Long productId, Long quantity) {
        String lockKey = "lock:product:" + productId;

        try {
            boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(10)));
            if (!locked) {
                throw new ConcurrentOrderException("다른 주문 처리 중");
            }

            try {
                //재고 확인 및 감소
                boolean stockReserved = productClient.reserveStock(productId, new StockReservationRequestDto(quantity));
                if (!stockReserved) {
                    throw new InsufficientStockException("재고가 부족합니다");
                }
                // 상품 정보 조회
                ProductDto product = productClient.getProductOrderInfo(productId);

                // 주문 생성
                Order order = Order.builder()
                        .customerId(customerId)
                        .orderDate(LocalDateTime.now())
                        .orderStatus(OrderStatus.PENDING_ORDER)
                        .orderItemList(new ArrayList<>())  // 명시적으로 빈 리스트 설정
                        .build();

                OrderItem orderItem = OrderItem.builder()
                        .productId(productId)
                        .productName(product.getProductName())
                        .quantity(quantity)
                        .price(product.getPrice())
                        .order(order)
                        .build();

                order.addOrderItem(orderItem);
                Order savedOrder = orderRepository.save(order);

                // 결제 처리
                PaymentRequestDto paymentRequestDto = new PaymentRequestDto(savedOrder.getId());
                PaymentResult paymentResult = paymentClient.processPayment(paymentRequestDto);

                // 주문 상태 업데이트
                if (paymentResult == PaymentResult.SUCCESS) {
                    savedOrder.setOrderStatus(OrderStatus.PAYMENT_COMPLETE);
                    productClient.confirmStockReservation(productId, quantity);
                } else {
                    savedOrder.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                    productClient.cancelStockReservation(productId, quantity);
                }

                Order updatedOrder = orderRepository.save(savedOrder);
                return OrderDto.from(updatedOrder);
            } catch (FeignException e) {
                log.error("외부 서비스 통신 간 오류",e);
                productClient.cancelStockReservation(productId, quantity);
                throw new ExternalServiceException("외부 서비스 통신 중 오류가 발생했습니다");
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
