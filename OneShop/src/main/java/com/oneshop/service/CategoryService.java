//package com.oneshop.service;
//
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.oneshop.entity.Category;
//import com.oneshop.repository.CategoryRepository;
//@Service
//public class CategoryService {
//	private final CategoryRepository categoryRepository;
//
//    @Autowired
//    public CategoryService(CategoryRepository categoryRepository) {
//        this.categoryRepository = categoryRepository;
//    }
//
//    @Transactional
//    public List<Category> findAll() {
//        return categoryRepository.findAll();
//    }
//    
//    @Transactional
//    public Optional<Category> findById(Long categoryId) {
//        return categoryRepository.findById(categoryId);
//    }
//    @Transactional 
//    public Category save(Category category) {
//        return categoryRepository.save(category);
//    }
//    
//    public static final Long UNCATEGORIZED_CATEGORY_ID = 1L; 
//
//    @Transactional
//    public void deleteById(Long categoryId) {
//        if (categoryId.equals(UNCATEGORIZED_CATEGORY_ID)) {
//            throw new UnsupportedOperationException("Attempted to delete the default category ID: " + categoryId);
//        }
//        categoryRepository.deleteById(categoryId);
//    }
//}
//// code cũ=====================
//// src/main/java/com/oneshop/service/CategoryService.java
package com.oneshop.service;

import com.oneshop.entity.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
	public static final Long UNCATEGORIZED_CATEGORY_ID = 1L;

    List<Category> findAll();

    Optional<Category> findById(Long id);

    Category saveCategory(Category category);

    void deleteCategory(Long id);
   //========
    Category save(Category category);
    void deleteById(Long Id);
}
