package com.jonggae.yakku.customers.entity;

import com.jonggae.yakku.address.Address;
import com.jonggae.yakku.customers.dto.CustomerUpdateDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customers")

public class Customer {

    @Id
    @Column(name = "customer_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "customer_name", unique = true, nullable = false)
    private String customerName;

    @Column(name = "customer_password", nullable = false)
    private String password;

    @Column(name = "customer_email", unique = true, nullable = false)
    private String email;

    @ManyToMany
    @JoinTable(name = "customer_authority", joinColumns = {@JoinColumn(name = "customer_id", referencedColumnName = "customer_id")},
            inverseJoinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "authority_name")})
    private Set<Authority> authorities = new HashSet<>(); // nullPointerException 방지위해 초기화 함


    @Column(name = "activated_account") // 이후 softdelete에도 쓸수 있을듯, 활성화된 계정
    private boolean enabled;

    // todo: Customer 엔티티의 생성 일자는 필요한가?
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    public void updateCustomerInfo(CustomerUpdateDto updateDto, PasswordEncoder passwordEncoder) {
        if (updateDto.getEmail() != null) this.email = updateDto.getEmail();
        if (updateDto.getPassword() !=null) this.password = passwordEncoder.encode(updateDto.getPassword());
        if (updateDto.getAddress() != null && this.address!=null){
            this.address.updatedAddress(updateDto.getAddress(), updateDto.getAddressDetail());
        }
    }
}
