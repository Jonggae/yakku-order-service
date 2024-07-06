package com.jonggae.yakku.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.exceptions.NotFoundOrderException;
import com.jonggae.yakku.exceptions.NotFoundOrderItemException;
import com.jonggae.yakku.kafka.EventDto;
import com.jonggae.yakku.order.controller.ProductClient;
import com.jonggae.yakku.order.dto.AddProductToOrderRequestDto;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.dto.ProductDto;
import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderItem;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.repository.OrderItemRepository;
import com.jonggae.yakku.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

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

        ProductDto productDto = productClient.getProductOrderInfo(requestDto.getProductId());

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

    // 주문 확정 -> 재고가 실제로 감소해야할까? 아니면 결제까지 이어져야 할까  Payment와 관련이 있을것
    @Transactional
    public void confirmOrder(Long customerId, Long orderId) {
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(NotFoundOrderException::new);
        existingOrder.validateOrderStatusForUpdate();
        existingOrder.confirmOrder();
        Order savedOrder = orderRepository.save(existingOrder);

        createNewOrder(customerId);

        EventDto eventDto = EventDto.builder()
                .eventType("ORDER_CONFIRMED")
                .orderId(orderId)
                .customerId(customerId)
                .data(Map.of("orderStatus", OrderStatus.PENDING_PAYMENT)).build();

        try {
            String eventMessage = objectMapper.writeValueAsString(eventDto);
            kafkaTemplate.send("order-events", eventMessage);
        } catch (JsonProcessingException e) {
            log.error("주문 과정에 문제가 생겼습니다.", e);
        }
        OrderDto.from(savedOrder);
    }

    // 주문 취소
    @Transactional
    public void cancelOrderByCustomer(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(NotFoundOrderException::new);

        if (order.getOrderStatus() == OrderStatus.PAID || order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
            order.updateOrderStatus(OrderStatus.CANCELLED);
            Order savedOrder = orderRepository.save(order);

            // 주문 취소 이벤트 발행
            EventDto eventDto = EventDto.builder()
                    .eventType("ORDER_CANCELLED")
                    .orderId(orderId)
                    .customerId(customerId)
                    .data(Map.of("orderStatus", OrderStatus.CANCELLED.name()))
                    .build();

            try {
                String eventMessage = objectMapper.writeValueAsString(eventDto);
                kafkaTemplate.send("order-events", eventMessage);
            } catch (JsonProcessingException e) {
                log.error("Error publishing order cancelled event", e);
            }

            OrderDto.from(savedOrder);
        } else {
            throw new IllegalStateException("결제 완료나 주문 완료된 주문만 취소 가능합니다.");
        }
    }

    //주문 상태 업데이트 todo : 자동으로 진행될 수 있나요..?
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(NotFoundOrderException::new);

        if (order.isActive() &&
                (newStatus == OrderStatus.PENDING_PAYMENT || newStatus == OrderStatus.CANCELLED)) {
            order.setActive(false);
            createNewOrder(order.getCustomerId());
        }

        order.updateOrderStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        // 주문 상태 변경 이벤트 발행
        EventDto eventDto = EventDto.builder()
                .eventType("ORDER_STATUS_UPDATED")
                .orderId(orderId)
                .customerId(order.getCustomerId())
                .data(Map.of("newStatus", newStatus.name()))
                .build();

        try {
            String eventMessage = objectMapper.writeValueAsString(eventDto);
            kafkaTemplate.send("order-events", eventMessage);
        } catch (JsonProcessingException e) {
            log.error("Error publishing order status updated event", e);
        }

        OrderDto.from(savedOrder);
    }

    // 주문 수량 변경
    @Transactional
    public void updateOrderItemQuantity(Long customerId, Long orderItemId, int newQuantity) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(NotFoundOrderItemException::new);

        Order order = orderItem.getOrder();
        order.validateOrderStatusForUpdate();

        orderItem.setQuantity(newQuantity);
        orderItemRepository.save(orderItem);

        // 주문 항목 수량 변경 이벤트 발행
        EventDto eventDto = EventDto.builder()
                .eventType("ORDER_ITEM_QUANTITY_UPDATED")
                .orderId(order.getId())
                .customerId(customerId)
                .productId(orderItem.getProductId())
                .data(Map.of("newQuantity", newQuantity))
                .build();

        try {
            String eventMessage = objectMapper.writeValueAsString(eventDto);
            kafkaTemplate.send("order-events", eventMessage);
        } catch (JsonProcessingException e) {
            log.error("Error publishing order item quantity updated event", e);
        }

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

        // 주문 항목 삭제 이벤트 발행
        EventDto eventDto = EventDto.builder()
                .eventType("ORDER_ITEM_DELETED")
                .orderId(order.getId())
                .customerId(customerId)
                .productId(orderItem.getProductId())
                .data(Map.of("quantity", orderItem.getQuantity()))
                .build();

        try {
            String eventMessage = objectMapper.writeValueAsString(eventDto);
            kafkaTemplate.send("order-events", eventMessage);
        } catch (JsonProcessingException e) {
            log.error("Error publishing order item deleted event", e);
        }

        OrderDto.from(order);
    }


//    public Order createPendingOrder(Long customerId) {
//        Customer customer = customerRepository.findById(customerId)
//                .orElseThrow(NotFoundMemberException::new);
//
//        Order order = Order.builder()
//                .customerId(customer.getId()) //todo: id로 접근해야함
//                .orderDate(LocalDateTime.now())
//                .orderStatus(OrderStatus.PENDING_ORDER)
//                .build();
//        return orderRepository.save(order);
//    }
//    public OrderItemDto addOrderItem(Long customerId, OrderItemDto orderItemDto) {
//        customerRepository.findById(customerId)
//                .orElseThrow(NotFoundMemberException::new);
//
//        Order order = orderRepository.findByCustomerIdAndOrderStatus(customerId, OrderStatus.PENDING_ORDER)
//                .orElseGet(() -> createPendingOrder(customerId));
//
//        Product product = productRepository.findById(orderItemDto.getProductId())
//                .orElseThrow(NotFoundProductException::new);
//        // todo 재고 확인
//        if (!product.checkStock(orderItemDto.getQuantity())) {
//            throw new InsufficientStockException(orderItemDto.getProductName());
//        }
//
//        OrderItem orderItem = OrderItem.builder()
//                .order(order)
//                .product(product)
//                .quantity(orderItemDto.getQuantity())
//                .build();
//
//        orderItem = orderItemRepository.save(orderItem);
//        order.getOrderItemList().add(orderItem);
//        orderRepository.save(order);
//
//        return OrderItemDto.from(orderItem);
//    }

//    // 주문 확정 (재고 감소해야함)
//    @Transactional
//    public OrderDto confirmOrder(Long customerId, Long orderId) {
//        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, customerId)
//                .orElseThrow(NotFoundOrderException::new);
//        existingOrder.validateOrderStatusForUpdate();
//
//        validateAndReduceStock(existingOrder);
//        existingOrder.confirmOrder();
//
//        return OrderDto.from(orderRepository.save(existingOrder));
//    }
//
//    //주문 취소 - 재고 회복됨
//    public List<OrderDto> cancelOrderByCustomer(Long orderId, long customerId) {
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(NotFoundOrderException::new);
//
//        if (order.getOrderStatus() == OrderStatus.PAID || order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
//            order.getOrderItemList().forEach(item -> {
//                Product product = item.getProduct();
//                product.setStock(product.getStock() + item.getQuantity());
//                productRepository.save(product);
//            });
//            order.updateOrderStatus(OrderStatus.CANCELLED);
//            orderRepository.save(order);
//        } else {
//            throw new IllegalStateException("결제 완료나 주문 완료된 주문만 취소 가능합니다.");
//        }
//        return getOrderList(customerId);
//    }
//
//    // 재고 확인
//    private void validateAndReduceStock(Order order) {
//        for (OrderItem orderItem : order.getOrderItemList()) {
//            Product product = orderItem.getProduct();
//            if (!product.checkStock(orderItem.getQuantity())) {
//                throw new InsufficientStockException(product.getProductName());
//            }
//            product.decreaseStock(orderItem.getQuantity());
//            productRepository.save(product);
//        }
//    }
//
//    // 주문 상태 업데이트
//    public OrderDto updateOrderStatus(Long orderId, OrderStatusUpdateDto statusUpdateDto) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(NotFoundOrderException::new);
//        OrderStatus newStatus = statusUpdateDto.getStatus();
//        order.updateOrderStatus(newStatus);
//        return OrderDto.from(orderRepository.save(order));
//    }
//
//    // 미확정된 주문 수정하기 - 주문 수량 변경
//    public List<OrderDto> updateOrderItemQuantity(Long customerId, Long orderItemId, OrderItemDto orderItemDto) {
//        OrderItem orderItem = orderItemRepository.findById(orderItemId)
//                .orElseThrow(NotFoundOrderItemException::new);
//
//        Order order = orderItem.getOrder();
//        order.validateOrderStatusForUpdate();
//
//        orderItem.setQuantity(orderItemDto.getQuantity());
//        orderItemRepository.save(orderItem);
//        return getOrderList(customerId);
//    }
//
//    // 미확정 된 주문 내 상품 삭제하기
//    public List<OrderDto> deleteOrderItem(Long customerId, Long orderItemId) {
//        OrderItem orderItem = orderItemRepository.findById(orderItemId)
//                .orElseThrow(NotFoundOrderItemException::new);
//
//        Product product = orderItem.getProduct();
//        product.setStock(product.getStock() + orderItem.getQuantity());
//        productRepository.save(product);
//
//        orderItemRepository.delete(orderItem);
//        return getOrderList(customerId);
//    }
}