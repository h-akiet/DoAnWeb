package com.oneshop.repository;

import com.oneshop.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.oneshop.entity.vendor.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}