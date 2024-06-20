package com.jonggae.yakku.customers.controller;

import com.jonggae.yakku.common.ApiResponseDto;
import com.jonggae.yakku.common.ApiResponseUtil;
import com.jonggae.yakku.customers.dto.CustomerDto;
import com.jonggae.yakku.customers.service.CustomerService;
import jdk.jfr.Frequency;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<CustomerDto>> register(@RequestBody CustomerDto requestDto) {
        CustomerDto registerdCustomerDto = customerService.register(requestDto);
        String message = "회원 가입 성공";
        return ApiResponseUtil.success(message, registerdCustomerDto, 200);
    }

    //todo: 시큐리티 구현 후 작성
    @GetMapping("/my-page")
    public ResponseEntity<ApiResponseDto<CustomerDto>> myPage() {
        CustomerDto getCustomerDto = customerService.getMyPage();
        String message = "회원 정보";
        return ApiResponseUtil.success(message, getCustomerDto, 200);
    }
}
