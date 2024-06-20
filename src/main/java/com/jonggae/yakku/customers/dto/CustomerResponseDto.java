package com.jonggae.yakku.customers.dto;

import com.jonggae.yakku.customers.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDto {

    private String customerName;
    private String email;
    private String address;
    private String addressDetail;

    public static CustomerResponseDto from(Customer customer) {
        return CustomerResponseDto.builder()
                .customerName(customer.getCustomerName())
                .email(customer.getEmail())
                .address(customer.getAddress().getAddress())
                .addressDetail(customer.getAddress().getAddressDetail())
                .build();

    }
}
