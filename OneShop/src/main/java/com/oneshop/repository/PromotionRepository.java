// src/main/java/com/oneshop/repository/PromotionRepository.java
package com.oneshop.repository;

import org.springframework.data.domain.Page; // <<< THÊM IMPORT NÀY
import org.springframework.data.domain.Pageable; // <<< THÊM IMPORT NÀY
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.Promotion;

import java.time.LocalDate; // <<< THÊM IMPORT NÀY
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    // Tìm tất cả khuyến mãi của một Shop
    List<Promotion> findByShopId(Long shopId);

    // ===>>> THÊM PHƯƠNG THỨC MỚI NÀY <<<===
    /**
     * Tìm các khuyến mãi chưa hết hạn (ngày kết thúc là hôm nay hoặc sau hôm nay)
     * Sắp xếp theo ngày bắt đầu giảm dần (mới nhất lên trước).
     */
    Page<Promotion> findByEndDateAfter(LocalDate date, Pageable pageable);
    // ===>>> KẾT THÚC <<<===
}