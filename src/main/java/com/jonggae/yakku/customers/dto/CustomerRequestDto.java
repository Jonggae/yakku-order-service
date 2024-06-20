package com.jonggae.yakku.customers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRequestDto {
    private String customerName;
    private String password;
    private String email;
    private String address;
    private String addressDetail;

}
