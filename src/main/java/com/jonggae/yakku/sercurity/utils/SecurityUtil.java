package com.jonggae.yakku.sercurity.utils;

import com.jonggae.yakku.customers.entity.Customer;
import com.jonggae.yakku.customers.repository.CustomerRepository;
import com.jonggae.yakku.exceptions.NotFoundMemberException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final CustomerRepository customerRepository;
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtil.class);

    public Optional<String> getCurrentCustomerName() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            logger.debug("Security context에 인증 정보가 없습니다.");
            return Optional.empty();
        }

        String username = null;

        if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            username = springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            username = (String) authentication.getPrincipal();
        }
        return Optional.ofNullable(username);
    }
    public Long getCurrentCustomerId(Authentication authentication) {
        String customerName = authentication.getName();
        Customer customer = customerRepository.findByCustomerName(customerName)
                .orElseThrow(NotFoundMemberException::new);
        return customer.getId();
    }
}
