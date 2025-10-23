package com.oneshop.service.vendor;

import com.oneshop.dto.vendor.PromotionDto;
import com.oneshop.entity.vendor.Promotion;

import java.util.List;

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
}