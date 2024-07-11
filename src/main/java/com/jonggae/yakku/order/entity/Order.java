package com.jonggae.yakku.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate; //주문 시간

    @Column(name = "order_status")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItemList = new ArrayList<>();

    private boolean isActive;

    //주문 확정용
    public void confirmOrder() {
        if (this.orderStatus != OrderStatus.PENDING_ORDER) {
            throw new IllegalStateException("Can only confirm pending orders.");
        }
        this.orderStatus = OrderStatus.ORDER_CREATED;
        this.isActive = false;
    }

    // 현재 활성화된 주문인가를 식별
    public boolean isActive() {
        return isActive;
    }

    public void updateOrderStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
    }

    public void validateOrderStatusForUpdate() {
        if (this.orderStatus != OrderStatus.PENDING_ORDER) {
            throw new RuntimeException("주문 상태가 대기중이 아니므로 주문 항목을 변경할 수 없습니다.");
        }
    }
    public void addOrderItem(OrderItem orderItem) {
        orderItemList.add(orderItem);
        orderItem.setOrder(this);
    }
}
