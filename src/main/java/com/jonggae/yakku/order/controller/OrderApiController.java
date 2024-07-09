package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.common.messageUtil.MessageUtil;
import com.jonggae.yakku.order.feignDto.AddProductToOrderRequestDto;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.messages.OrderApiMessages;
import com.jonggae.yakku.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderApiController {
    public static final String ORDER_ADD_SUCCESS = OrderApiMessages.ORDER_ADD_SUCCESS;
    public static final String ORDER_CONFIRM_SUCCESS = OrderApiMessages.ORDER_CONFIRM_SUCCESS;
    public static final String ORDER_STATUS_UPDATE_SUCCESS = OrderApiMessages.ORDER_STATUS_UPDATE_SUCCESS;
    public static final String ORDER_UPDATE_SUCCESS = OrderApiMessages.ORDER_UPDATE_SUCCESS;
    public static final String ORDER_DELETE_SUCCESS = OrderApiMessages.ORDER_DELETE_SUCCESS;
    public static final String ORDER_CANCEL_SUCCESS = OrderApiMessages.ORDER_CANCEL_SUCCESS;

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderDto>> addProductToOrder(@RequestBody AddProductToOrderRequestDto request,
                                                                      @RequestHeader("customerId") Long customerId) {
        OrderDto orderDto = orderService.addProductToOrder(customerId,request);
        return ApiResponseUtil.success(ORDER_ADD_SUCCESS, orderDto, 200);
    }

    //주문 조회
    @GetMapping("/my-order")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> getOrderList(@RequestHeader("customerId") Long customerId,
                                                                       @RequestHeader("customerName") String customerName) {
        List<OrderDto> orderDto = orderService.getOrderList(customerId);
        String message = MessageUtil.getFormattedMessage(OrderApiMessages.ORDER_LIST_SUCCESS, customerName);
        return ApiResponseUtil.success(message, orderDto, 200);
    }

    // 주문 확정
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> confirmOrder(
            @RequestHeader("customerId") Long customerId,
            @PathVariable Long orderId) {
        orderService.confirmOrder(customerId, orderId);
        List<OrderDto> updatedOrderList = orderService.getOrderList(customerId);
        return ApiResponseUtil.success(ORDER_CONFIRM_SUCCESS, updatedOrderList, 200);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> cancelOrder(
            @RequestHeader("customerId") Long customerId,
            @PathVariable Long orderId) {
        orderService.cancelOrderByCustomer(orderId, customerId);
        List<OrderDto> updatedOrderList = orderService.getOrderList(customerId);
        return ApiResponseUtil.success(ORDER_CANCEL_SUCCESS, updatedOrderList, 200);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> updateOrderStatus(
            @RequestHeader("customerId") Long customerId,
            @PathVariable Long orderId,
            @RequestParam OrderStatus newStatus) {
        orderService.updateOrderStatus(orderId, newStatus);
        List<OrderDto> updatedOrderList = orderService.getOrderList(customerId);
        return ApiResponseUtil.success(ORDER_STATUS_UPDATE_SUCCESS, updatedOrderList, 200);
    }

    @PutMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> updateOrderItemQuantity(
            @RequestHeader("customerId") Long customerId,
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @RequestParam Long newQuantity) {
        orderService.updateOrderItemQuantity(customerId, orderItemId, newQuantity);
        List<OrderDto> updatedOrderList = orderService.getOrderList(customerId);
        return ApiResponseUtil.success(ORDER_UPDATE_SUCCESS, updatedOrderList, 200);
    }

    @DeleteMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> deleteOrderItem(
            @RequestHeader("customerId") Long customerId,
            @PathVariable Long orderId,
            @PathVariable Long orderItemId) {
        orderService.deleteOrderItem(customerId, orderItemId);
        List<OrderDto> updatedOrderList = orderService.getOrderList(customerId);
        return ApiResponseUtil.success(ORDER_DELETE_SUCCESS, updatedOrderList, 200);
    }
}