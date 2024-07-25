package com.jonggae.yakku.order.service;


import com.jonggae.yakku.exceptions.ConcurrentOrderException;
import com.jonggae.yakku.exceptions.ExternalServiceException;
import com.jonggae.yakku.exceptions.InsufficientStockException;
import com.jonggae.yakku.order.controller.PaymentClient;
import com.jonggae.yakku.order.controller.ProductClient;
import com.jonggae.yakku.order.dto.*;
import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderItem;
import com.jonggae.yakku.order.entity.OrderResult;
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

    private final ExecutorService executorService = Executors.newFixedThreadPool(100);
    private final AtomicBoolean stockExhausted = new AtomicBoolean(false);

    private static final String LOCK_PREFIX = "lock:product:";
    private static final long LOCK_WAIT_TIME = 1000;
    private static final long LOCK_LEASE_TIME = 5000;

    @Transactional
    public OrderResponse createTimeOrder(Long customerId, Long productId, Long quantity) {
        log.info("주문 시작 - CustomerId: {}, ProductId: {}, Quantity: {}", customerId, productId, quantity);

        if (stockExhausted.get()) {
            log.warn("재고 소진 - CustomerId: {}, ProductId: {}", customerId, productId);
            return new OrderResponse(null, OrderResult.STOCK_INSUFFICIENT);
        }

        ProductDto product = productClient.getProductOrderInfo(productId);
        RLock lock = redissonClient.getLock(LOCK_PREFIX + productId);
        boolean stockReserved = false;

        try {
            if (lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                try {
                    stockReserved = productClient.reserveStock(productId, new StockReservationRequestDto(quantity));
                    if (!stockReserved) {
                        log.warn("재고 부족 - CustomerId: {}, ProductId: {}, Quantity: {}", customerId, productId, quantity);
                        return new OrderResponse(null, OrderResult.STOCK_INSUFFICIENT);
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("동시 주문 충돌 - CustomerId: {}, ProductId: {}", customerId, productId);
                return new OrderResponse(null, OrderResult.CONCURRENT_ORDER_CONFLICT);
            }

            Order order = createOrder(customerId, product, quantity);
            Order savedOrder = orderRepository.save(order);
            log.info("주문 생성 - OrderId: {}, CustomerId: {}", savedOrder.getId(), customerId);

            PaymentResult paymentResult = processPayment(savedOrder.getId());
            log.info("결제 처리 결과 - OrderId: {}, Result: {}", savedOrder.getId(), paymentResult);

            OrderStatus finalStatus = updateOrderStatus(savedOrder, paymentResult, productId, quantity);
            log.info("주문 상태 업데이트 - OrderId: {}, FinalStatus: {}", savedOrder.getId(), finalStatus);

            Order updatedOrder = orderRepository.save(savedOrder);
            OrderDto orderDto = OrderDto.from(updatedOrder);
            OrderResult result = (finalStatus == OrderStatus.PAYMENT_COMPLETE) ? OrderResult.SUCCESS : OrderResult.PAYMENT_FAILED;

            return new OrderResponse(orderDto, result);

        } catch (InterruptedException e) {
            log.error("락 획득 중 인터럽트 발생 - CustomerId: {}, ProductId: {}", customerId, productId);
            Thread.currentThread().interrupt();
            return new OrderResponse(null, OrderResult.CONCURRENT_ORDER_CONFLICT);
        } catch (FeignException e) {
            log.error("외부 서비스 통신 오류 - CustomerId: {}, ProductId: {}", customerId, productId, e);
            if (stockReserved) {
                productClient.cancelStockReservation(productId, quantity);
            }
            return new OrderResponse(null, OrderResult.EXTERNAL_SERVICE_ERROR);
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

    private OrderStatus updateOrderStatus(Order order, PaymentResult paymentResult, Long productId, Long quantity) {
        if (paymentResult == PaymentResult.SUCCESS) {
            order.setOrderStatus(OrderStatus.PAYMENT_COMPLETE);
            productClient.confirmStockReservation(productId, quantity);
        } else {
            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            productClient.cancelStockReservation(productId, quantity);
        }
        return order.getOrderStatus();
    }

    public CompletableFuture<OrderResponse> createTimeOrderAsync(Long customerId, Long productId, Long quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createTimeOrder(customerId, productId, quantity);
            } catch (Exception e) {
                log.error("주문 처리 중 예외 발생", e);
                return new OrderResponse(null, OrderResult.EXTERNAL_SERVICE_ERROR);
            }
        }, executorService);
    }

    public void resetStockStatus() {
        stockExhausted.set(false);
    }

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

