package com.jonggae.yakku.customers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jonggae.yakku.customers.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    private Long id;
    private String customerName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) //보안관련 직렬화,역직렬화
    private String password;
    private String email;

    public static CustomerDto from(Customer customer) {

        return CustomerDto.builder()
                .id(customer.getId())
                .customerName(customer.getCustomerName())
                .email(customer.getEmail())
                .build();

    }
}
