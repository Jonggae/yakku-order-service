package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.entity.OrderStatus;
import com.jonggae.yakku.order.service.TimeOrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/test/time-orders")
@RequiredArgsConstructor
public class TimeOrderTestController {

    private final TimeOrderService timeOrderService;
    private static final Logger logger = LoggerFactory.getLogger(TimeOrderTestController.class);

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDto<OrderDto>> testCreateTimeOrder(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Long quantity) {
        logger.info("테스트 예약 주문 생성 시도 - CustomerId: {}, ProductId: {}, Quantity: {}",
                customerId, productId, quantity);

        try {
            OrderDto orderDto = timeOrderService.createTimeOrder(customerId, productId, quantity);

            String resultMessage;
            if (orderDto.getStatus() == OrderStatus.PAYMENT_COMPLETE) {
                resultMessage = "테스트 예약 주문 생성 및 결제 완료";
                logger.info("테스트 예약 주문 생성 성공 - OrderId: {}, CustomerId: {}, 상태: 결제 완료", orderDto.getOrderId(), customerId);
            } else if (orderDto.getStatus() == OrderStatus.PAYMENT_FAILED) {
                resultMessage = "테스트 예약 주문 생성 실패: 결제 실패";
                logger.warn("테스트 예약 주문 생성 실패 - OrderId: {}, CustomerId: {}, 상태: 결제 실패", orderDto.getOrderId(), customerId);
            } else {
                resultMessage = "테스트 예약 주문 상태 불명확";
                logger.warn("테스트 예약 주문 상태 불명확 - OrderId: {}, CustomerId: {}, 상태: {}", orderDto.getOrderId(), customerId, orderDto.getStatus());
            }

            return ApiResponseUtil.success(resultMessage, orderDto, 200);
        } catch (Exception e) {
            logger.error("테스트 예약 주문 생성 중 오류 발생. CustomerId: {}, ProductId: {}, Quantity: {}", customerId, productId, quantity, e);
            return ApiResponseUtil.error("예약 주문 처리 중 오류가 발생했습니다.", 500, null, null);
        }
    }

    @PostMapping("/concurrent-test")
    public ResponseEntity<ApiResponseDto<String>> testConcurrentTimeOrders(
            @RequestParam Long productId,
            @RequestParam Long quantity,
            @RequestParam int numberOfOrders) {
        logger.info("동시 예약 주문 테스트 시작 - ProductId: {}, Quantity: {}, 주문 수: {}", productId, quantity, numberOfOrders);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfOrders);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfOrders; i++) {
            Long customerId = Long.valueOf(i + 1);  // 테스트용 고객 ID
            futures.add(executorService.submit(() -> {
                try {
                    OrderDto orderDto = timeOrderService.createTimeOrder(customerId, productId, quantity);
                    return "주문 성공 - CustomerId: " + customerId + ", OrderId: " + orderDto.getOrderId() + ", 상태: " + orderDto.getStatus();
                } catch (Exception e) {
                    return "주문 실패 - CustomerId: " + customerId + ", 오류: " + e.getMessage();
                }
            }));
        }

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("Future 처리 중 오류 발생: " + e.getMessage());
            }
        }

        executorService.shutdown();

        logger.info("동시 예약 주문 테스트 완료 - 결과: {}", results);
        return ApiResponseUtil.success("동시 예약 주문 테스트 완료", String.join("\n", results), 200);
    }


}
