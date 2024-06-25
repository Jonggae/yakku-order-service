package com.jonggae.yakku.order.service;

import com.jonggae.yakku.customers.entity.Customer;
import com.jonggae.yakku.customers.repository.CustomerRepository;
import com.jonggae.yakku.exceptions.*;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.dto.OrderItemDto;
import com.jonggae.yakku.order.dto.OrderStatusUpdateDto;
import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderItem;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.repository.OrderItemRepository;
import com.jonggae.yakku.order.repository.OrderRepository;
import com.jonggae.yakku.products.entity.Product;
import com.jonggae.yakku.products.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    /*주문은 많은 상태가 필요함.
     * 위시리스트와 연관지어서 바로 주문에 추가하거나, 위시리스트를 거쳐 주문에 추가할 수 있는 방법을 만들어보자
     * i) 어떤 방법을 통하든 결과적으로 확정되지 않은 주문이 하나 생성됨 (PendingOrder) */

    public Order createPendingOrder(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(NotFoundMemberException::new);

        Order order = Order.builder()
                .customer(customer)
                .orderDate(LocalDateTime.now())
                .orderStatus(OrderStatus.PENDING_ORDER)
                .build();
        return orderRepository.save(order);
    }

    public List<OrderDto> getOrderList(Long customerId) {
        return orderRepository.findAllByCustomerId(customerId).stream()
                .map(OrderDto::from)
                .toList();
    }

    public OrderItemDto addOrderItem(Long customerId, OrderItemDto orderItemDto) {
        customerRepository.findById(customerId)
                .orElseThrow(NotFoundMemberException::new);

        Order order = orderRepository.findByCustomerIdAndOrderStatus(customerId, OrderStatus.PENDING_ORDER)
                .orElseGet(() -> createPendingOrder(customerId));

        Product product = productRepository.findById(orderItemDto.getProductId())
                .orElseThrow(NotFoundProductException::new);
        // todo 재고 확인
        if (!product.checkStock(orderItemDto.getQuantity())) {
            throw new InsufficientStockException(orderItemDto.getProductName());
        }

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(orderItemDto.getQuantity())
                .build();

        orderItem = orderItemRepository.save(orderItem);
        order.getOrderItemList().add(orderItem);
        orderRepository.save(order);

        return OrderItemDto.from(orderItem);
    }

    // 주문 확정 (재고 감소해야함)
    @Transactional
    public OrderDto confirmOrder(Long customerId, Long orderId) {
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(NotFoundOrderException::new);
        existingOrder.validateOrderStatusForUpdate();

        validateAndReduceStock(existingOrder);
        existingOrder.confirmOrder();

        return OrderDto.from(orderRepository.save(existingOrder));
    }

    //주문 취소 - 재고 회복됨
    public List<OrderDto> cancelOrderByCustomer(Long orderId, long customerId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(NotFoundOrderException::new);

        if (order.getOrderStatus() == OrderStatus.PAID || order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
            order.getOrderItemList().forEach(item -> {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            });
            order.updateOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        } else {
            throw new IllegalStateException("결제 완료나 주문 완료된 주문만 취소 가능합니다.");
        }
        return getOrderList(customerId);
    }

    // 재고 확인
    private void validateAndReduceStock(Order order) {
        for (OrderItem orderItem : order.getOrderItemList()) {
            Product product = orderItem.getProduct();
            if (!product.checkStock(orderItem.getQuantity())) {
                throw new InsufficientStockException(product.getProductName());
            }
            product.decreaseStock(orderItem.getQuantity());
            productRepository.save(product);
        }
    }

    // 주문 상태 업데이트
    public OrderDto updateOrderStatus(Long orderId, OrderStatusUpdateDto statusUpdateDto){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(NotFoundOrderException::new);
        OrderStatus newStatus = statusUpdateDto.getStatus();
        order.updateOrderStatus(newStatus);
        return OrderDto.from(orderRepository.save(order));
    }

    // 미확정된 주문 수정하기 - 주문 수량 변경
    public List<OrderDto> updateOrderItemQuantity(Long customerId, Long orderItemId, OrderItemDto orderItemDto){
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(NotFoundOrderItemException::new);

        Order order = orderItem.getOrder();
        order.validateOrderStatusForUpdate();

        orderItem.setQuantity(orderItemDto.getQuantity());
        orderItemRepository.save(orderItem);
        return getOrderList(customerId);
    }

    // 미확정 된 주문 내 상품 삭제하기
    public List<OrderDto> deleteOrderItem(Long customerId, Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(NotFoundOrderItemException::new);

        Product product = orderItem.getProduct();
        product.setStock(product.getStock() + orderItem.getQuantity());
        productRepository.save(product);

        orderItemRepository.delete(orderItem);
        return getOrderList(customerId);
    }

    // 주문 객체 자체를 삭제하는 메서드 -> 사용하지 않음

    // 관리자가 모든 주문을 확인하는 용도 -> 보류
    public List<OrderDto> findAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(OrderDto::from)
                .collect(Collectors.toList());
    }
}