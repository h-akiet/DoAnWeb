package com.oneshop.service;

import com.oneshop.dto.PromotionDto;
import com.oneshop.entity.Promotion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromotionService {

    /**
     * Lấy tất cả khuyến mãi của một shop
     */
    List<Promotion> getPromotionsByShop(Long shopId);

    /**
     * Tạo một khuyến mãi mới
     */
    Promotion createPromotion(PromotionDto promotionDto, Long shopId);

    /**
     * (Tùy chọn) Xóa một khuyến mãi
     */
    void deletePromotion(Long promotionId, Long shopId);

    /**
     * Lấy danh sách các khuyến mãi còn hạn (phân trang).
     * @param pageable Thông tin phân trang (trang, kích thước, sắp xếp).
     * @return Page<Promotion>
     */
    Page<Promotion> findActiveAndUpcomingPromotions(Pageable pageable);
}