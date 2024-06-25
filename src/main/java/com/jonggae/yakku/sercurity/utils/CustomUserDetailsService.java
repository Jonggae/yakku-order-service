package com.jonggae.yakku.sercurity.utils;

import com.jonggae.yakku.customers.entity.Customer;
import com.jonggae.yakku.customers.repository.CustomerRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String customerName) throws UsernameNotFoundException {
        return customerRepository.findOneWithAuthoritiesByCustomerName(customerName)
                .map(this::createCustomUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(customerName + "데이터베이스에서 찾을 수 없습니다."));
    }

    private CustomUserDetails createCustomUserDetails(Customer customer) {
        List<GrantedAuthority> grantedAuthorities = customer.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthorityName().name()))
                .collect(Collectors.toList());
        return new CustomUserDetails(customer, grantedAuthorities);
    }
}
