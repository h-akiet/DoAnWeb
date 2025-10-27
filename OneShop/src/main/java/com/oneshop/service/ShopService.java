package com.oneshop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oneshop.entity.Shop;
import com.oneshop.enums.ShopStatus;
import com.oneshop.repository.ShopRepository;
@Service
public class ShopService {

	private final ShopRepository shopRepository;

    @Autowired
    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Transactional
	public List<Shop> findAll() {
		return shopRepository.findAllWithVendor(); 
	}
    
    @Transactional
    public Shop findById(Long shopId) {
        return shopRepository.findById(shopId).orElse(null); 
    }
    public List<Shop> getApprovedShops() {
        return shopRepository.findByStatus(ShopStatus.APPROVED);
    }
    
    @Transactional
    public Shop updateShopCommissionRate(Long shopId, BigDecimal newRate) {
        Optional<Shop> shopOpt = shopRepository.findById(shopId);

        if (shopOpt.isPresent()) {
            Shop shop = shopOpt.get();
            
            // 1. Cập nhật tỉ lệ chiết khấu
            shop.setCommissionRate(newRate);
            
            // 2. Cập nhật cột lưu thời gian là thời điểm hiện tại
            shop.setCommissionUpdatedAt(LocalDateTime.now());
            
            return shopRepository.save(shop);
        }
        return null;
    }
}
