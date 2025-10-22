package com.oneshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>,JpaSpecificationExecutor<Product> {
    List<Product> findBySalesCountGreaterThanOrderBySalesCountDesc(int minSales);
    List<Product> findTop10ByOrderBySalesCountDesc();
    List<Product> findTop10ByOrderByProductIdDesc();
    List<Product> findByCategoryAndProductIdNot(Category category, Long productId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (?1 IS NULL OR p.name LIKE %?1%) " +
           "AND (?2 IS NULL OR p.category.id = ?2) " +
           "AND (?3 IS NULL OR p.price >= ?3) " +
           "AND (?4 IS NULL OR p.price <= ?4)")
    List<Product> searchAndFilter(String name, Long categoryId, Double minPrice, Double maxPrice);
	
	@Query("SELECT p FROM Product p WHERE p.originalPrice IS NOT NULL AND p.originalPrice > p.price " +
	           "ORDER BY ((p.originalPrice - p.price) / p.originalPrice) DESC LIMIT 10")
	 List<Product> findTop10ByBestDiscount();
	List<Product> findTop10ByCategoryAndProductIdNot(Category category, Long productId);
}