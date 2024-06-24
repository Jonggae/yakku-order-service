package com.jonggae.yakku.order.entity;

import com.jonggae.yakku.customers.entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate; //주문 시간

    @Column(name = "order_status")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @ManyToOne
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem>orderItemList = new ArrayList<>();

    //주문 확정용
    public void confirmOrder() {
        this.orderStatus = OrderStatus.PENDING_PAYMENT;
        this.orderDate = LocalDateTime.now();
    }

    public void updateOrderStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
    }

    public void validateOrderStatusForUpdate() {
        if (this.orderStatus != OrderStatus.PENDING_ORDER) {
            throw new RuntimeException("주문 상태가 대기중이 아니므로 주문 항목을 변경할 수 없습니다.");
        }
    }
}
