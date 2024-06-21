package com.jonggae.yakku.customers.repository;

import com.jonggae.yakku.customers.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, String> {
    Optional<Authority> findByAuthorityName(String roleUser);

}
