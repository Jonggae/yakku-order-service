package com.jonggae.yakku.sercurity.utils;

import com.jonggae.yakku.customers.entity.Customer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    @Getter
    private final Customer customer;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Customer customer, Collection<? extends GrantedAuthority> authorities) {
        this.customer = customer;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return customer.getPassword();
    }

    @Override
    public String getUsername() {
        return customer.getCustomerName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return customer.isEnabled();  // customer의 상태를 반영
    }

    public Long getCustomerId() {
        return customer.getId();
    }
}

