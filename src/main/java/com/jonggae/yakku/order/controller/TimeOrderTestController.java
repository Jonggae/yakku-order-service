package com.jonggae.yakku.order.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.order.dto.OrderDto;
import com.jonggae.yakku.order.entity.OrderResult;
import com.jonggae.yakku.order.service.TimeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/test/time-orders")
@RequiredArgsConstructor
@Slf4j
public class TimeOrderTestController {

    private final TimeOrderService timeOrderService;
    private static final Logger logger = LoggerFactory.getLogger(TimeOrderTestController.class);

    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<ApiResponseDto<OrderDto>>> testCreateTimeOrder(
            @RequestParam Long customerId,
            @RequestParam Long productId,
            @RequestParam Long quantity) {
        log.info("테스트 예약 주문 요청 - CustomerId: {}, ProductId: {}, Quantity: {}", customerId, productId, quantity);

        return timeOrderService.createTimeOrderAsync(customerId, productId, quantity)
                .thenApply(response -> {
                    String resultMessage = getResultMessage(response.getResult());
                    log.info("테스트 예약 주문 결과 - CustomerId: {}, Result: {}", customerId, response.getResult());
                    return ApiResponseUtil.success(resultMessage, response.getOrderDto(), 200);
                })
                .exceptionally(e -> {
                    log.error("테스트 예약 주문 처리 중 예외 발생 - CustomerId: {}", customerId, e);
                    return ApiResponseUtil.error("예약 주문 처리 중 오류가 발생했습니다.", 500, null, null);
                });
    }

    private String getResultMessage(OrderResult result) {
        return switch (result) {
            case SUCCESS -> "테스트 예약 주문 생성 및 결제 완료";
            case PAYMENT_FAILED -> "테스트 예약 주문 생성 실패: 결제 실패";
            case STOCK_INSUFFICIENT -> "테스트 예약 주문 생성 실패: 재고 부족";
            case CONCURRENT_ORDER_CONFLICT -> "테스트 예약 주문 생성 실패: 동시 주문 충돌";
            case EXTERNAL_SERVICE_ERROR -> "테스트 예약 주문 생성 실패: 외부 서비스 오류";
            default -> "테스트 예약 주문 상태 불명확";
        };
    }

//        @PostMapping("/concurrent-test")
//        public CompletableFuture<ResponseEntity<ApiResponseDto<String>>> testConcurrentTimeOrders(
//                @RequestParam Long productId,
//                @RequestParam Long quantity,
//                @RequestParam int numberOfOrders) {
//            logger.info("동시 예약 주문 테스트 시작 - ProductId: {}, Quantity: {}, 주문 수: {}", productId, quantity, numberOfOrders);
//
//            List<CompletableFuture<String>> futures = new ArrayList<>();
//
//            for (int i = 0; i < numberOfOrders; i++) {
//                Long customerId = Long.valueOf(i + 1);  // 테스트용 고객 ID
//                futures.add(timeOrderService.createTimeOrderAsync(customerId, productId, quantity)
//                        .thenApply(orderDto -> "주문 성공 - CustomerId: " + customerId + ", OrderId: " + orderDto.getOrderId() + ", 상태: " + orderDto.getStatus())
//                        .exceptionally(e -> "주문 실패 - CustomerId: " + customerId + ", 오류: " + e.getMessage()));
//            }
//
//            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                    .thenApply(v -> futures.stream()
//                            .map(CompletableFuture::join)
//                            .collect(Collectors.toList()))
//                    .thenApply(results -> {
//                        logger.info("동시 예약 주문 테스트 완료 - 결과: {}", results);
//                        return ApiResponseUtil.success("동시 예약 주문 테스트 완료", String.join("\n", results), 200);
//                    });
//        }

    @PostMapping("/reset-stock")
    public ResponseEntity<ApiResponseDto<String>> resetStockStatus() {
        timeOrderService.resetStockStatus();
        return ApiResponseUtil.success("재고 상태가 리셋되었습니다.", null, 200);
    }

}
