package com.oneshop.repository.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.vendor.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Các phương thức CRUD cơ bản là đủ dùng
}