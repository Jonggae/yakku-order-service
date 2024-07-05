package com.jonggae.yakku.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonggae.yakku.exceptions.InvalidProductDataException;
import com.jonggae.yakku.exceptions.NotFoundOrderException;
import com.jonggae.yakku.exceptions.NotFoundOrderItemException;
import com.jonggae.yakku.exceptions.OrderProcessingException;
import com.jonggae.yakku.kafka.EventDto;
import com.jonggae.yakku.kafka.kafkaDto.ProductDto;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderItem;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.repository.OrderItemRepository;
import com.jonggae.yakku.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, CompletableFuture<ProductDto>> futureMap = new ConcurrentHashMap<>();

    public List<OrderDto> getOrderList(Long customerId) {
        return orderRepository.findAllByCustomerIdWithItems(customerId).stream()
                .map(OrderDto::from)
                .collect(Collectors.toList());
    }

    // 주문에 상품 추가하기
    public OrderDto addProductToOrder(Long customerId, Long productId, int quantity) {
        try {
            Order order = orderRepository.findPendingOrderByCustomerId(customerId)
                    .orElseGet(() -> createNewOrder(customerId));

            OrderItem orderItem = OrderItem.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .order(order)
                    .build();
            order.getOrderItemList().add(orderItem);

            Order savedOrder = orderRepository.save(order);

            if (savedOrder.getId() == null) {
                throw new IllegalStateException("Saved order has null id");
            }
            CompletableFuture<ProductDto> future = new CompletableFuture<>();
            requestProductInfo(productId, customerId, savedOrder.getId(), future);

            try {
                // 최대 10초 대기
                ProductDto productDto = future.get(10, TimeUnit.SECONDS);
                updateOrderItem(savedOrder.getId(), productDto);
                return OrderDto.from(orderRepository.findById(savedOrder.getId()).orElseThrow());
            } catch (ExecutionException e) {

                if (e.getCause() instanceof InvalidProductDataException) {
                    orderRepository.delete(savedOrder);  // 주문 삭제
                    throw new OrderProcessingException("상품 정보를 가져오는데 실패했습니다. 다시 시도해주세요.", e);
                }
                throw new OrderProcessingException("주문 처리 중 실행 오류가 발생했습니다.", e);
            } catch (TimeoutException e) {
                orderRepository.delete(savedOrder);  // 주문 삭제
                throw new OrderProcessingException("주문 처리 시간이 초과되었습니다. 다시 시도해주세요.", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingException("주문 처리 중 인터럽트 발생", e);
        }
    }

    private Order createNewOrder(Long customerId) {
        Order newOrder = new Order();
        newOrder.setCustomerId(customerId);
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setOrderStatus(OrderStatus.PENDING_ORDER);
        return newOrder;
    }

    private void requestProductInfo(Long productId, Long customerId, Long orderId, CompletableFuture<ProductDto> future) {
        if (orderId == null) {
            log.error("Cannot request product info with null orderId");
            future.completeExceptionally(new IllegalArgumentException("orderId cannot be null"));
            return;
        }

        EventDto eventDto = EventDto.builder()
                .eventType("PRODUCT_INFO_REQUEST")
                .orderId(orderId)
                .productId(productId)
                .customerId(customerId)
                .build();

//        ProductInfoRequest request = new ProductInfoRequest(productId, customerId, orderId);
        try {
            String value = objectMapper.writeValueAsString(eventDto);
            futureMap.put(orderId, future);
            log.info("Sending product info request for orderId: {}", orderId);
            kafkaTemplate.send("product-info-request", productId.toString(), value);
        } catch (JsonProcessingException e) {
            log.error("Error processing product info request", e);
            future.completeExceptionally(e);
        }
    }

    @Transactional
    @KafkaListener(topics = "product-info-response", groupId = "order-service")
    public void handleProductInfoResponse(String message) {
        try {
            EventDto eventDto = objectMapper.readValue(message, EventDto.class);
            if (eventDto.getOrderId() == null) {
                return;
            }
            CompletableFuture<ProductDto> future = futureMap.get(eventDto.getOrderId());
            if (future != null) {
                ProductDto productDto = objectMapper.convertValue(eventDto.getData(), ProductDto.class);
                future.complete(productDto);
                log.info("Completed future for orderId: {}", eventDto.getOrderId());
                updateOrderItem(eventDto.getOrderId(), productDto);
            } else {
                log.warn("No future found for orderId: {}", eventDto.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing product info response", e);
        }
    }

    @Transactional
    protected void updateOrderItem(Long orderId, ProductDto productDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(NotFoundOrderException::new);
        log.debug("Order found: {}, OrderItems: {}", orderId, order.getOrderItemList());

        OrderItem orderItem = order.getOrderItemList().stream()
                .peek(item -> log.debug("Checking OrderItem: productId={}, orderId={}", item.getProductId(), item.getOrder().getId()))
                .filter(item -> item.getProductId().equals(productDto.getId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("OrderItem not found for orderId: {} and productId: {}", orderId, productDto.getId());
                    return new NotFoundOrderItemException();
                });

        orderItem.setProductName(productDto.getProductName());
        orderItem.setPrice(productDto.getPrice() * orderItem.getQuantity());
        log.info("Updating OrderItem: {}", orderItem);
        orderRepository.save(order);
    }

    // 주문 확정
    @Transactional
    public OrderDto confirmOrder(Long customerId, Long orderId) {
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(NotFoundOrderException::new);
        existingOrder.validateOrderStatusForUpdate();
        existingOrder.confirmOrder();
        Order savedOrder = orderRepository.save(existingOrder);

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
        return OrderDto.from(savedOrder);
    }

    // 주문 취소
    @Transactional
    public OrderDto cancelOrderByCustomer(Long orderId, Long customerId) {
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

            return OrderDto.from(savedOrder);
        } else {
            throw new IllegalStateException("결제 완료나 주문 완료된 주문만 취소 가능합니다.");
        }
    }

    //주문 상태 업데이트 todo : 자동으로 진행될 수 있나요..?
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(NotFoundOrderException::new);
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

        return OrderDto.from(savedOrder);
    }

    // 주문 수량 변경
    @Transactional
    public OrderDto updateOrderItemQuantity(Long customerId, Long orderItemId, int newQuantity) {
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

        return OrderDto.from(order);
    }

    //주문 항목 삭제
    @Transactional
    public OrderDto deleteOrderItem(Long customerId, Long orderItemId) {
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

        return OrderDto.from(order);
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