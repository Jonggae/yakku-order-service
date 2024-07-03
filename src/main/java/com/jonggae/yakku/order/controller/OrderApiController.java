package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.common.messageUtil.MessageUtil;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.messages.OrderApiMessages;
import com.jonggae.yakku.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;

    @GetMapping("/my-order")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> getOrderList(@RequestHeader("customerId") Long customerId,
                                                                       @RequestHeader("customerName") String customerName) {
        List<OrderDto> orderDto = orderService.getOrderList(customerId);
        String message = MessageUtil.getFormattedMessage(OrderApiMessages.ORDER_LIST_SUCCESS, customerName);
        return ApiResponseUtil.success(message, orderDto, 200);
    }
}