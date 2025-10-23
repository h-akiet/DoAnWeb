package com.oneshop.repository.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.vendor.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA sẽ tự hiểu: "Tìm một User dựa trên cột username"
    Optional<User> findByUsername(String username);

    // Tìm một User dựa trên email
    Optional<User> findByEmail(String email);
}