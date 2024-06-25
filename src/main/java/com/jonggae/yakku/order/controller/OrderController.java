package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.common.messageUtil.MessageUtil;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.dto.OrderItemDto;
import com.jonggae.yakku.order.dto.OrderStatusUpdateDto;
import com.jonggae.yakku.order.messages.OrderApiMessages;
import com.jonggae.yakku.order.service.OrderService;
import com.jonggae.yakku.sercurity.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtil securityUtil;

    // 내 주문 조회
    @GetMapping("/my-order")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> getOrderList() {
        String customerName = securityUtil.getCurrentCustomerName();
        Long customerId = securityUtil.getCurrentCustomerId();
        List<OrderDto> orderDto = orderService.getOrderList(customerId);
        String message = MessageUtil.getFormattedMessage(OrderApiMessages.ORDER_LIST_SUCCESS, customerName);
        return ApiResponseUtil.success(message, orderDto, 200);
    }

    // 주문 항목 추가
    @PostMapping("/my-order/items")
    public ResponseEntity<ApiResponseDto<OrderItemDto>> addOrderItem(@RequestBody OrderItemDto orderItemDto) {
        Long customerId = securityUtil.getCurrentCustomerId();
        OrderItemDto updatedItemDto = orderService.addOrderItem(customerId, orderItemDto);
        String message = MessageUtil.getMessage(OrderApiMessages.ORDER_ADD_SUCCESS);
        return ApiResponseUtil.success(message, updatedItemDto, 200);
    }

    // 주문 확정
    @PostMapping("/my-order/{orderId}/confirm")
    public ResponseEntity<ApiResponseDto<OrderDto>> confirmOrder(@PathVariable(name = "orderId") Long orderId) {
        Long customerId = securityUtil.getCurrentCustomerId();
        OrderDto confirmOrder = orderService.confirmOrder(customerId, orderId);
        String message = MessageUtil.getMessage(OrderApiMessages.ORDER_STATUS_UPDATE_SUCCESS);
        return ApiResponseUtil.success(message, confirmOrder, 200);
    }

    // 주문 상태 업데이트
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDto<OrderDto>> updateOrderStatus(@PathVariable(name = "orderId") Long orderId,
                                                                      @RequestBody OrderStatusUpdateDto statusUpdateDto) {
        OrderDto updatedOrder = orderService.updateOrderStatus(orderId, statusUpdateDto);
        String message = MessageUtil.getMessage(OrderApiMessages.ORDER_STATUS_UPDATE_SUCCESS);
        return ApiResponseUtil.success(message, updatedOrder, 200);
    }

    // 주문 항목 수량 변경
    @PatchMapping("/my-order/items/{orderItemId}")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> updateOrderItemQuantity(@PathVariable(name = "orderItemId") Long orderItemId,
                                                                                  @RequestBody OrderItemDto orderItemDto) {
        Long customerId = securityUtil.getCurrentCustomerId();
        List<OrderDto> updatedOrders = orderService.updateOrderItemQuantity(customerId, orderItemId, orderItemDto);
        String message = MessageUtil.getMessage(OrderApiMessages.ORDER_UPDATE_SUCCESS);
        return ApiResponseUtil.success(message, updatedOrders, 200);

    }

    // 주문 항목 삭제
    @DeleteMapping("/my-order/items/{orderItemId}")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> deleteOrderItem(@PathVariable(name = "orderItemId") Long orderItemId) {
        Long customerId = securityUtil.getCurrentCustomerId();
        List<OrderDto> orderDto = orderService.deleteOrderItem(customerId, orderItemId);
        String message = MessageUtil.getMessage(OrderApiMessages.ORDER_DELETE_SUCCESS);
        return ApiResponseUtil.success(message, orderDto, 200);
    }

    //주문 취소 (클라이언트)
    @PatchMapping("/cancel/{orderId}")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> cancelOrder(@PathVariable(name= "orderId") Long orderId) {
        Long customerId = securityUtil.getCurrentCustomerId();
        List<OrderDto> orderDto = orderService.cancelOrderByCustomer(orderId, customerId);
        String message = MessageUtil.getMessage(OrderApiMessages.ORDER_CANCEL_SUCCESS);
        return ApiResponseUtil.success(message, orderDto, 200);
    }

    // 서버쪽에서 주문을 삭제하는 것은 DB 내에서 지우므로 보류 (사용하지 않을듯)

}
