package com.oneshop.repository;

import com.oneshop.entity.Otp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByUserEmailAndType(String email, String type);
    List<Otp> findByTypeAndExpiresAtBefore(String type, LocalDateTime time);
}