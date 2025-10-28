// src/main/java/com/oneshop/service/impl/CategoryServiceImpl.java
package com.oneshop.service.impl; // Đổi package sang impl

import com.oneshop.entity.Category;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.service.CategoryService; // Import interface

import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; // Import Sort
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException; // Import Exception

import java.util.List;
import java.util.Optional; // Import Optional

@Service // Giữ @Service
public class CategoryServiceImpl implements CategoryService { // Implement interface

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class); // Thêm Logger

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override // Thêm @Override
    @Transactional(readOnly = true) // Thêm readOnly
    public List<Category> findAll() {
        logger.debug("Fetching all categories");
        return categoryRepository.findAll(Sort.by("name")); // Sắp xếp
    }

    @Override // Thêm @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        logger.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id);
    }

    @Override // Thêm @Override
    @Transactional
    public Category saveCategory(Category category) {
        // Nên chuẩn hóa tên trước khi lưu
        if(category.getName() != null) {
            category.setName(category.getName().trim());
        } else {
             throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        logger.info("Saving category: {}", category.getName());
        // Kiểm tra trùng tên nếu là thêm mới hoặc tên thay đổi
        if (category.getId() == null || (category.getId() != null && !categoryRepository.findById(category.getId()).map(Category::getName).orElse("").equalsIgnoreCase(category.getName()))) {
            // Cần thêm hàm findByNameIgnoreCase trong repo
            if (categoryRepository.findByNameIgnoreCase(category.getName()).isPresent()) {
                throw new IllegalArgumentException("Tên danh mục '" + category.getName() + "' đã tồn tại.");
            }
        }
        return categoryRepository.save(category);
    }

    @Override // Thêm @Override
    @Transactional
    public void deleteCategory(Long id) {
        logger.warn("Attempting to delete category ID: {}", id);
        // Kiểm tra sản phẩm sử dụng
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            logger.warn("Cannot delete category {} used by {} products.", id, productCount);
            throw new RuntimeException("Không thể xóa danh mục đang được sử dụng bởi " + productCount + " sản phẩm.");
        }

        // Xóa category
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            logger.info("Category {} deleted successfully.", id);
        } else {
            logger.warn("Category {} not found for deletion.", id);
            throw new EntityNotFoundException("Không tìm thấy danh mục để xóa với ID: " + id);
        }
    }
}