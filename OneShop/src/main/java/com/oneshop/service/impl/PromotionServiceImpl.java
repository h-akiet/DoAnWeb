package com.oneshop.service.vendor.impl;

import com.oneshop.dto.vendor.PromotionDto;
import com.oneshop.entity.vendor.Promotion;
import com.oneshop.entity.vendor.PromotionTypeEntity;
import com.oneshop.entity.vendor.Shop;
import com.oneshop.repository.vendor.PromotionRepository;
import com.oneshop.repository.vendor.PromotionTypeRepository;
import com.oneshop.repository.vendor.ShopRepository;
import com.oneshop.service.vendor.PromotionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // Thêm import BigDecimal
import java.util.List;

@Service
public class PromotionServiceImpl implements PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private ShopRepository shopRepository;

    // === THÊM AUTOWIRED NÀY ===
    @Autowired
    private PromotionTypeRepository promotionTypeRepository;
    // =========================

    @Override
    public List<Promotion> getPromotionsByShop(Long shopId) {
        return promotionRepository.findByShopId(shopId);
    }

    @Override
    @Transactional
    public Promotion createPromotion(PromotionDto promotionDto, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop"));

        // === THAY ĐỔI LOGIC LẤY TYPE ===
        PromotionTypeEntity promotionType = promotionTypeRepository.findById(promotionDto.getPromotionTypeId())
                .orElseThrow(() -> new RuntimeException("Loại khuyến mãi không hợp lệ"));
        // ==============================

        Promotion promotion = new Promotion();
        promotion.setCampaignName(promotionDto.getCampaignName());
        promotion.setDiscountCode(promotionDto.getDiscountCode());
        
        promotion.setType(promotionType); // Gán Entity

        // === THÊM VALIDATION CHO VALUE ===
        if (!"FREE_SHIPPING".equals(promotionType.getCode())) {
            if (promotionDto.getDiscountValue() == null || promotionDto.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
                 throw new IllegalArgumentException("Giá trị giảm không hợp lệ cho loại khuyến mãi này.");
            }
             promotion.setValue(promotionDto.getDiscountValue());
        } else {
            promotion.setValue(null); 
        }
        
        promotion.setStartDate(promotionDto.getStartDate());
        promotion.setEndDate(promotionDto.getEndDate());
        promotion.setShop(shop);

        return promotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public void deletePromotion(Long promotionId, Long shopId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        if (!promotion.getShop().getId().equals(shopId)) {
            throw new SecurityException("Bạn không có quyền xóa khuyến mãi này");
        }
        
        promotionRepository.delete(promotion);
    }
}