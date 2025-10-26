package com.oneshop.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oneshop.entity.Shop;
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
}
