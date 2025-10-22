package com.oneshop.service; // Or your service package

import com.oneshop.entity.Category;
import com.oneshop.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Gets all categories from the database.
     * @return A list of all Category entities.
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    // Add other category-related methods here if needed (e.g., findById)
}