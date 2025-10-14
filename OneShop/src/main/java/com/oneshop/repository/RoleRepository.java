package com.oneshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.oneshop.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}