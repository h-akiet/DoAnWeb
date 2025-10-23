package com.oneshop.service.vendor;

import java.util.List;

import com.oneshop.entity.vendor.Category;

public interface CategoryService {
    List<Category> findAll();
    Category saveCategory(Category category);
    void deleteCategory(Long id);
}