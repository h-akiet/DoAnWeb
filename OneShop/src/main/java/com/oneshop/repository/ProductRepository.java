package com.oneshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.oneshop.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySalesCountGreaterThanOrderBySalesCountDesc(int minSales);
    List<Product> findTop6ByOrderBySalesCountDesc();
    List<Product> findTop6ByOrderByProductIdDesc();

    @Query("SELECT p FROM Product p WHERE (?1 IS NULL OR p.name LIKE %?1%) " +
           "AND (?2 IS NULL OR p.category.id = ?2) " +
           "AND (?3 IS NULL OR p.price >= ?3) " +
           "AND (?4 IS NULL OR p.price <= ?4)")
    List<Product> searchAndFilter(String name, Long categoryId, Double minPrice, Double maxPrice);
}