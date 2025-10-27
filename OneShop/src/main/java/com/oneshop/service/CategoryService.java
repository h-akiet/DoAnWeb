// src/main/java/com/oneshop/service/CategoryService.java
package com.oneshop.service;

import com.oneshop.entity.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {

    List<Category> findAll();

    Optional<Category> findById(Long id);

    Category saveCategory(Category category);

    void deleteCategory(Long id);
}