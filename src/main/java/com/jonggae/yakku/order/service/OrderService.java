package com.jonggae.yakku.order.service;

import com.jonggae.yakku.exceptions.InsufficientStockException;
import com.jonggae.yakku.exceptions.NotFoundOrderException;
import com.jonggae.yakku.exceptions.NotFoundOrderItemException;
import com.jonggae.yakku.exceptions.StockReservationFailException;
import com.jonggae.yakku.order.controller.PaymentClient;
import com.jonggae.yakku.order.controller.ProductClient;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.dto.PaymentRequestDto;
import com.jonggae.yakku.order.dto.PaymentResult;
import com.jonggae.yakku.order.dto.ProductDto;
import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderItem;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.feignDto.AddProductToOrderRequestDto;
import com.jonggae.yakku.order.feignDto.StockReservationRequestDto;
import com.jonggae.yakku.order.repository.OrderItemRepository;
import com.jonggae.yakku.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
// 일반 구매와 예약 구매를 따로 분리함

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;


    public List<OrderDto> getOrderList(Long customerId) {
        return orderRepository.findAllByCustomerIdWithItems(customerId).stream()
                .map(OrderDto::from)
                .collect(Collectors.toList());
    }

    // 주문에 상품 추가하기
    @Transactional
    public OrderDto addProductToOrder(Long customerId, AddProductToOrderRequestDto requestDto) {
        Order order = orderRepository.findActiveOrderByCustomerId(customerId)
                .orElseGet(() -> createNewOrder(customerId));

        if (order.getOrderStatus() != OrderStatus.PENDING_ORDER) {
            order = createNewOrder(customerId);
        }
        ProductDto productDto = productClient.getProductOrderInfo(requestDto.getProductId());
        Long currentStock = productClient.checkStock(requestDto.getProductId());

        //재고 확인만 함
        if (currentStock < requestDto.getQuantity()) {
            throw new InsufficientStockException("재고가 부족합니다.");
        }

            OrderItem orderItem = OrderItem.builder()
                    .productId(productDto.getId())
                    .productName(productDto.getProductName())
                    .quantity(requestDto.getQuantity())
                    .price(productDto.getPrice())
                    .order(order)
                    .build();

            order.getOrderItemList().add(orderItem);
            Order savedOrder = orderRepository.save(order);

            return OrderDto.from(savedOrder);
    }

    // 만약 주문함 자체가 없다면 생성 (주문 확정으로 인해 없을 수 있음)
    private Order createNewOrder(Long customerId) {
        return Order.builder()
                .customerId(customerId)
                .orderDate(LocalDateTime.now())
                .orderStatus(OrderStatus.PENDING_ORDER)
                .orderItemList(new ArrayList<>())
                .isActive(true)
                .build();
    }

    // 주문 확정 -> 재고가 실제로 감소하는 시점
    @Transactional
    public void confirmOrder(Long customerId, Long orderId) {
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(NotFoundOrderException::new);
        existingOrder.validateOrderStatusForUpdate();

        // 각 상품의 재고 감소
        for (OrderItem item: existingOrder.getOrderItemList()){
            boolean stockReserved = productClient.reserveStock(
                    item.getProductId(),
                    new StockReservationRequestDto(item.getQuantity())
            );
            if(!stockReserved){
                throw new StockReservationFailException("재고 예약 실패: 상품 ID " + item.getProductId());
            }
        }
        existingOrder.confirmOrder(); //실제 주문 상태를 변경하는 로직 (PENDING_ORDER 로)
        existingOrder.setActive(false); //현재의 활성화된 주문할 상품 목록이 아님을 표시
        Order savedOrder = orderRepository.save(existingOrder);

        // 결제 서비스로 요청 보내기
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto(savedOrder.getId());
        PaymentResult  paymentResult = paymentClient.processPayment(paymentRequestDto);

        if (paymentResult == PaymentResult.SUCCESS) {
            savedOrder.setOrderStatus(OrderStatus.PAYMENT_COMPLETE);
        } else {
            savedOrder.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            rollbackStock(savedOrder);
        }
        savedOrder = orderRepository.save(savedOrder);
        createNewOrder(customerId);
        OrderDto.from(savedOrder);
    }

    private void rollbackStock(Order order) {
        for (OrderItem item : order.getOrderItemList()) {
            StockReservationRequestDto requestDto = new StockReservationRequestDto(item.getQuantity());
            productClient.releaseStock(item.getProductId(), requestDto);
        }
    }

    // 주문 취소
    @Transactional
    public void cancelOrderByCustomer(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(NotFoundOrderException::new);

        if (order.getOrderStatus() == OrderStatus.PAYMENT_COMPLETE || order.getOrderStatus() == OrderStatus.ORDER_CREATED) {
            try {
                //재고 복구
                for (OrderItem item : order.getOrderItemList()) {
                    productClient.releaseStock(
                            item.getProductId(),
                            new StockReservationRequestDto(item.getQuantity())
                    );
                }
                order.updateOrderStatus(OrderStatus.CANCELLED);
                Order savedOrder = orderRepository.save(order);

                log.info("주문 취소 완료: orderId={}, customerId={}", orderId, customerId);

                OrderDto.from(savedOrder);
            } catch (Exception e) {
                log.error("주문 취소 중 오류 발생", e);
                throw new RuntimeException("주문 취소 오류: " + e.getMessage());
            }
        } else {
            throw new IllegalStateException("결제 완료나 주문 완료된 주문만 취소 가능합니다.");
        }
    }

    //주문 상태 업데이트 todo : 자동으로 진행될 수 있나요..?
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(NotFoundOrderException::new);

        if (order.isActive() && (newStatus == OrderStatus.ORDER_CREATED || newStatus == OrderStatus.CANCELLED)) {
            order.setActive(false);
            createNewOrder(order.getCustomerId());
        }

        order.updateOrderStatus(newStatus);
        Order savedOrder = orderRepository.save(order);


        OrderDto.from(savedOrder);
    }

    // 주문 수량 변경
    @Transactional
    public void updateOrderItemQuantity(Long customerId, Long orderItemId, Long newQuantity) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(NotFoundOrderItemException::new);

        Order order = orderItem.getOrder();
        order.validateOrderStatusForUpdate();

        orderItem.setQuantity(newQuantity);
        orderItemRepository.save(orderItem);

        OrderDto.from(order);
    }

    //주문 항목 삭제
    @Transactional
    public void deleteOrderItem(Long customerId, Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(NotFoundOrderItemException::new);

        Order order = orderItem.getOrder();
        order.getOrderItemList().remove(orderItem);
        orderItemRepository.delete(orderItem);

        OrderDto.from(order);
    }


}