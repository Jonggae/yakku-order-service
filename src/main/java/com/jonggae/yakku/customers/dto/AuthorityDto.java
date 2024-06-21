package com.jonggae.yakku.customers.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityDto {
    public String authorityName;
    //ROLE_USER, ROLE_ADMIN 존재
}
