package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.common.messageUtil.MessageUtil;
import com.jonggae.yakku.order.dto.AddProductRequest;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.messages.OrderApiMessages;
import com.jonggae.yakku.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderApiController {


    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "order-request";

    private final OrderService orderService;


    @PostMapping
    public ResponseEntity<ApiResponseDto<OrderDto>> addProductToOrder(@RequestBody AddProductRequest request,
                                                                      @RequestHeader("customerId") Long customerId){
        OrderDto orderDto = orderService.addProductToOrder(customerId, request.getProductId(), request.getQuantity());
        return ApiResponseUtil.success("성공",orderDto,200);
    }

    //주문 조회
    @GetMapping("/my-order")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> getOrderList(@RequestHeader("customerId") Long customerId,
                                                                       @RequestHeader("customerName") String customerName) {
        List<OrderDto> orderDto = orderService.getOrderList(customerId);
        String message = MessageUtil.getFormattedMessage(OrderApiMessages.ORDER_LIST_SUCCESS, customerName);
        return ApiResponseUtil.success(message, orderDto, 200);
    }
}