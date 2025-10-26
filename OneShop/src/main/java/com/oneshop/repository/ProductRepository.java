package com.oneshop.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oneshop.entity.Product;
import com.oneshop.enums.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, Long> {
	List<Product> findBySalesCountGreaterThanOrderBySalesCountDesc(int minSales);

	List<Product> findTop6ByOrderBySalesCountDesc();

	List<Product> findTop6ByOrderByProductIdDesc();

	@Query("SELECT p FROM Product p WHERE (?1 IS NULL OR p.name LIKE %?1%) " + "AND (?2 IS NULL OR p.category.id = ?2) "
			+ "AND (?3 IS NULL OR p.price >= ?3) " + "AND (?4 IS NULL OR p.price <= ?4)")
	List<Product> searchAndFilter(String name, Long categoryId, Double minPrice, Double maxPrice);

	// ADMIN
	@Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId "
			+ "AND (:productCode IS NULL OR CAST(p.productId AS string) LIKE %:productCode%) "
			+ "AND (:status IS NULL OR p.status = :status) " + // ✅ Xử lý Trạng thái
			"AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " + // ✅ Xử lý Danh mục
			"AND (:brand IS NULL OR p.brand LIKE %:brand%)") // ✅ Xử lý Thương hiệu
	List<Product> findFilteredProducts(@Param("shopId") Long shopId, @Param("productCode") String productCode,
			@Param("status") ProductStatus status, @Param("categoryId") Long categoryId, @Param("brand") String brand);

	/**
	 * Lấy danh sách các thương hiệu duy nhất (Distinct Brands) để làm Combobox.
	 */
	@Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.brand <> ''")
	Set<String> findAllUniqueBrands();
}