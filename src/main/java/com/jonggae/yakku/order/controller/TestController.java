package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.feignDto.AddProductToOrderRequestDto;
import com.jonggae.yakku.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/test/orders")
@RequiredArgsConstructor
public class TestController {

    private final OrderService orderService;
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    private final Random random = new Random();

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto<OrderDto>> testAddProductToOrder(@RequestParam Long customerId, @RequestBody AddProductToOrderRequestDto request) {
        logger.info("테스트 주문 생성 시도 - CustomerId: {}, ProductId: {}, Quantity: {}",
                customerId, request.getProductId(), request.getQuantity());

        OrderDto orderDto = orderService.addProductToOrder(customerId, request);

        logger.info("테스트 주문 생성 성공 - OrderId: {}, CustomerId: {}", orderDto.getOrderId(), customerId);
        return ApiResponseUtil.success("테스트 주문이 생성되었습니다.", orderDto, 200);
    }

    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<ApiResponseDto<List<OrderDto>>> testConfirmOrder(
            @PathVariable Long orderId,
            @RequestParam Long customerId) {  // URL 파라미터로 customerId를 받음

        logger.info("테스트 주문 확정 시도 - OrderId: {}, CustomerId: {}", orderId, customerId);

        try {
            orderService.confirmOrder(customerId, orderId);
            List<OrderDto> updatedOrderList = orderService.getOrderList(customerId);

            OrderDto confirmedOrder = updatedOrderList.stream()
                    .filter(order -> order.getOrderId().equals(orderId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("확정된 주문을 찾을 수 없습니다."));

            String resultMessage;
            if (confirmedOrder.getStatus() == OrderStatus.PAYMENT_COMPLETE) {
                resultMessage = "테스트 주문 확정 및 결제 완료";
                logger.info("테스트 주문 확정 성공 - OrderId: {}, CustomerId: {}, 상태: 결제 완료", orderId, customerId);
            } else if (confirmedOrder.getStatus() == OrderStatus.PAYMENT_FAILED) {
                resultMessage = "테스트 주문 확정 실패: 결제 실패";
                logger.warn("테스트 주문 확정 실패 - OrderId: {}, CustomerId: {}, 상태: 결제 실패", orderId, customerId);
            } else {
                resultMessage = "테스트 주문 상태 불명확";
                logger.warn("테스트 주문 상태 불명확 - OrderId: {}, CustomerId: {}, 상태: {}", orderId, customerId, confirmedOrder.getStatus());
            }

            return ApiResponseUtil.success(resultMessage, updatedOrderList, 200);
        } catch (Exception e) {
            logger.error("테스트 주문 확정 중 오류 발생. OrderId: {}, CustomerId: {}", orderId, customerId, e);
            return ApiResponseUtil.error("주문 처리 중 오류가 발생했습니다.", 500, null, null);
        }
    }
}
