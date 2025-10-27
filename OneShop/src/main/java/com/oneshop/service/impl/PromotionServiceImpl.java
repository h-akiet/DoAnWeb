package com.oneshop.service.impl;

import com.oneshop.dto.PromotionDto;
import com.oneshop.entity.Promotion;
import com.oneshop.entity.PromotionTypeEntity;
import com.oneshop.entity.Shop;
import com.oneshop.repository.PromotionRepository;
import com.oneshop.repository.PromotionTypeRepository;
import com.oneshop.repository.ShopRepository;
import com.oneshop.service.PromotionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // Thêm import BigDecimal
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest; // <<< THÊM IMPORT
import org.springframework.data.domain.Pageable; // <<< THÊM IMPORT
import org.springframework.data.domain.Sort;
import java.time.LocalDate;

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
    
    @Override
    @Transactional(readOnly = true)
    public Page<Promotion> findActiveAndUpcomingPromotions(Pageable pageable) {
        // Tìm các khuyến mãi có ngày kết thúc là hôm nay hoặc sau hôm nay
        // (Trừ đi 1 ngày để bao gồm cả các khuyến mãi kết thúc hôm nay)
        // Repository đã hỗ trợ Pageable, chỉ cần truyền vào
        return promotionRepository.findByEndDateAfter(LocalDate.now().minusDays(1), pageable);
    }
}