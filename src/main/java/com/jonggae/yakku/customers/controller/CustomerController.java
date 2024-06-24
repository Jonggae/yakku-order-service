package com.jonggae.yakku.customers.controller;

import com.jonggae.yakku.common.apiResponse.ApiResponseDto;
import com.jonggae.yakku.common.apiResponse.ApiResponseUtil;
import com.jonggae.yakku.customers.dto.CustomerRequestDto;
import com.jonggae.yakku.customers.dto.CustomerResponseDto;
import com.jonggae.yakku.customers.dto.CustomerUpdateDto;
import com.jonggae.yakku.customers.service.CustomerService;
import com.jonggae.yakku.sercurity.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customerService;

    //todo: 회원가입 상황에 대응한 예외 처리 작성하기 , response 응답 형태 정리하기

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<String>> register(@RequestBody CustomerRequestDto requestDto) {
        customerService.register(requestDto);
        String message = "해당 메일주소로 확인 메일을 보냈습니다.";
        return ApiResponseUtil.success(message, requestDto.getEmail(), 200);
    }

    @GetMapping("/confirm")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> confirmCustomer(@RequestParam("token") String token) {
        CustomerResponseDto customerDto = customerService.confirmCustomer(token);
        String message = "회원 가입이 완료되었습니다.";
        return ApiResponseUtil.success(message,customerDto, 200);

    }
    @GetMapping("/my-page")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> myPage() {
        CustomerResponseDto getCustomerDto = customerService.getMyPage();
        String message = "회원 정보";
        return ApiResponseUtil.success(message, getCustomerDto, 200);
    }

    @PatchMapping("/my-page/update")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> updateCustomerInfo(@RequestBody CustomerUpdateDto updateDto, Authentication authentication) {
        String customerName = authentication.getName();
        CustomerResponseDto updatedCustomer = customerService.updateCustomerInfo(customerName, updateDto);
        String message = "회원 정보가 수정되었습니다.";
        return ApiResponseUtil.success(message, updatedCustomer, 200);
    }
}
