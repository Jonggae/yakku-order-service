package com.jonggae.yakku.customers.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "authority")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Authority {
    @Id
    @Column(name = "authority_name")
    @Enumerated(EnumType.STRING)
    private UserRoleEnum authorityName;
}
