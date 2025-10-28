package com.oneshop.repository;

import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> findByUsernameOrEmailContaining(@Param("keyword") String keyword);
}

    // ✅ Thêm dòng này
    List<User> findByRole(Role role);
    @Query("SELECT u FROM User u WHERE u.username IN :usernames AND u.role = :role")
    List<User> findByUsernameInAndRole(@Param("usernames") List<String> usernames, @Param("role") Role role);
    List<User> findByRoleAndUsernameIn(Role role, List<String> usernames);
}

