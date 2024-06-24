package com.jonggae.yakku.order.repository;

import com.jonggae.yakku.order.entity.Order;
import com.jonggae.yakku.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    void deleteAllByOrder(Order order);
}
