package com.oneshop.service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneshop.entity.Product;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    public List<Product> getTopSellingProducts() {
        return productRepository.findBySalesCountGreaterThanOrderBySalesCountDesc(10);
    }

    public List<Product> searchAndFilter(String name, Long categoryId, Double minPrice, Double maxPrice) {
        return productRepository.searchAndFilter(name, categoryId, minPrice, maxPrice);
    }

    public List<com.oneshop.entity.Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    @Transactional(readOnly = true)
    public List<Product> findBestSellingProducts() {
        return productRepository.findTop6ByOrderBySalesCountDesc();
    }

    public List<Product> findNewestProducts() {
        return productRepository.findTop6ByOrderByProductIdDesc();
    }
    @Transactional(readOnly = true)
	public List<Product> findFeaturedProducts() {
		// TODO Auto-generated method stub
		return null;
	}
}