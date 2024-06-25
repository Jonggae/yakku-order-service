package com.jonggae.yakku.customers.dto;

import com.jonggae.yakku.customers.entity.UserRoleEnum;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityDto {
    public UserRoleEnum authorityName;
    //ROLE_USER, ROLE_ADMIN 존재
}
