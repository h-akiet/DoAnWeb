package com.oneshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.oneshop.entity.Otp;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByUserEmailAndType(String email, String type);
}