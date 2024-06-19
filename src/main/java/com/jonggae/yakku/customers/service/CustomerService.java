package com.jonggae.yakku.customers.service;

import com.jonggae.yakku.customers.dto.CustomerDto;
import com.jonggae.yakku.customers.entity.Customer;
import com.jonggae.yakku.customers.repository.CustomerRepository;
import com.jonggae.yakku.exceptions.DuplicateCustomerException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerDto register(CustomerDto customerDto) {
        checkCustomerInfo(customerDto.getCustomerName(), customerDto.getEmail());
        Customer customer = Customer.builder()
                .customerName(customerDto.getCustomerName())
                .password(passwordEncoder.encode(customerDto.getPassword()))
                .email(customerDto.getEmail())
                .build();

        customer = customerRepository.save(customer);

        return CustomerDto.from(customer);
    }

    //todo: 시큐리티 구현 후 다시 작성
    public CustomerDto getMyPage() {
        return null;
    }

    private void checkCustomerInfo(String customerName, String email) {
        if (customerRepository.findByCustomerName(customerName).isPresent()){
            throw new DuplicateCustomerException("이미 가입된 이름입니다.");
        }
        if (customerRepository.findByEmail(email).isPresent()) {
            throw new DuplicateCustomerException("이미 사용중인 이메일 주소 입니다.");
        }
    }
}
