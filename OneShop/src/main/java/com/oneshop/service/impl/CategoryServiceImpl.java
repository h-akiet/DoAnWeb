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
    
 // ===>>> PHƯƠNG THỨC TỪ CLASS CŨ ĐƯỢC HỢP NHẤT VÀO INTERFACE <<<===
 
    @Override
    @Transactional 
    public Category save(Category category) { // <<< TRIỂN KHAI PHƯƠNG THỨC save(Category category) TỪ CLASS CŨ >>>
        // Logic chuẩn hóa và kiểm tra trùng tên (giống logic cũ của saveCategory)
        if(category.getName() != null) {
            category.setName(category.getName().trim());
        } else {
             throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        logger.info("Saving category: {}", category.getName());
        
        // Kiểm tra trùng tên nếu là thêm mới hoặc tên thay đổi
        boolean isNewOrNameChanged = category.getId() == null || 
                                     !categoryRepository.findById(category.getId())
                                                        .map(Category::getName)
                                                        .orElse("")
                                                        .equalsIgnoreCase(category.getName());
                                                        
        if (isNewOrNameChanged) {
            if (categoryRepository.findByNameIgnoreCase(category.getName()).isPresent()) {
                throw new IllegalArgumentException("Tên danh mục '" + category.getName() + "' đã tồn tại.");
            }
        }
        
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteById(Long Id) { // <<< TRIỂN KHAI PHƯƠNG THỨC deleteById(Long categoryId) TỪ CLASS CŨ >>>
        logger.warn("Attempting to delete category by ID (từ class cũ): {}", Id);
        
        // 1. Kiểm tra ID mặc định
        if (Id.equals(CategoryService.UNCATEGORIZED_CATEGORY_ID)) {
            logger.error("Attempted to delete the default category ID: {}", Id);
            throw new UnsupportedOperationException("Attempted to delete the default category ID: " + Id);
        }
        
        // 2. Kiểm tra sự tồn tại (để có thể log) và xóa
        if (categoryRepository.existsById(Id)) { // <<< DÙNG existsById thay vì kiểm tra Product count ở đây (vì đã có deleteCategory) >>>
             // Lưu ý: Logic này không kiểm tra productCount, nếu muốn kiểm tra, cần gọi deleteCategory(categoryId) hoặc thêm logic kiểm tra productCount ở đây. 
             // Giữ nguyên logic đơn giản như trong Class cũ để tuân thủ.
             categoryRepository.deleteById(Id);
             logger.info("Category {} deleted successfully by deleteById.", Id);
        } else {
             logger.warn("Category {} not found for deletion (deleteById).", Id);
        }
    }
}