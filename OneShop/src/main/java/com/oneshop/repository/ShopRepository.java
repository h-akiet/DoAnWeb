package com.oneshop.repository.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.vendor.Shop;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    // Tìm Shop dựa trên User (chủ sở hữu)
    Optional<Shop> findByUserId(Long userId);
}