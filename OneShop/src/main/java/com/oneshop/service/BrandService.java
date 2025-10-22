package com.oneshop.service; // Or your service package

import com.oneshop.entity.Brand;
import com.oneshop.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

    /**
     * Gets all brands from the database.
     * @return A list of all Brand entities.
     */
    public List<Brand> findAll() {
        return brandRepository.findAll();
    }

    // Add other brand-related methods here if needed
}