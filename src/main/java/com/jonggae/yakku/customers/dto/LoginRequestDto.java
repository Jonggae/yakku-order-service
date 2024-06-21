package com.jonggae.yakku.customers.dto;

import com.jonggae.yakku.customers.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

    private String customerName;
    private String password;

    public Customer toEntity() {
        return Customer.builder()
                .customerName(customerName)
                .password(password)
                .build();
    }
}
