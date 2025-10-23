package com.oneshop.service.vendor.impl;

import com.oneshop.entity.vendor.Category;
import com.oneshop.repository.vendor.CategoryRepository;
import com.oneshop.repository.vendor.ProductRepository;
import com.oneshop.service.vendor.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // === THÊM AUTOWIRED NÀY ===
    @Autowired
    private ProductRepository productRepository;
    // ========================

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional 
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        long productCount = productRepository.countByCategoryId(id); 
        if (productCount > 0) {
            throw new RuntimeException("Không thể xóa danh mục đang được sử dụng bởi " + productCount + " sản phẩm.");
        }

        categoryRepository.findById(id).ifPresentOrElse(
            category -> categoryRepository.delete(category),
            () -> { throw new RuntimeException("Không tìm thấy danh mục để xóa với ID: " + id); }
        );
    }
    // ================================
}