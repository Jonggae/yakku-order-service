package com.jonggae.yakku.customers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerUpdateDto {

    private String email;
    private String password;
    private String address;
    private String addressDetail;

}
