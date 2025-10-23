package com.oneshop.repository.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.vendor.Promotion;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    // Tìm tất cả khuyến mãi của một Shop
    List<Promotion> findByShopId(Long shopId);
}