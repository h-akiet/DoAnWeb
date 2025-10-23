package com.oneshop.service.vendor;

import java.util.List;

import com.oneshop.entity.vendor.PromotionTypeEntity;

public interface PromotionTypeService {
    List<PromotionTypeEntity> findAll();
    PromotionTypeEntity savePromotionType(PromotionTypeEntity promotionType);
    void deletePromotionType(Long id);
}