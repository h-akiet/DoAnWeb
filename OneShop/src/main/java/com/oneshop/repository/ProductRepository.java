package com.oneshop.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal; // Import BigDecimal
import java.util.List;
import java.util.Optional;

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
	
	@Transactional
    @Modifying
    @Query("UPDATE Product p SET p.category.categoryId = :newId WHERE p.category.categoryId = :oldId")
    int updateCategoryByCategoryId(@Param("oldId") Long oldCategoryId, @Param("newId") Long newCategoryId);
  
   // --- Cho Vendor ---
    Page<Product> findByShopId(Long shopId, Pageable pageable);
    Page<Product> findByShopIdAndNameContainingIgnoreCase(Long shopId, String name, Pageable pageable);
    long countByShopId(Long shopId);
    long countByCategoryId(Long categoryId);

    // --- Cho User (Public) ---
    /**
     * Tìm sản phẩm đã publish (có phân trang).
     */
    Page<Product> findByPublishedTrue(Pageable pageable); // <<< ĐÃ THÊM

    /**
     * Tìm sản phẩm theo ID VÀ đã publish.
     */
    Optional<Product> findByProductIdAndPublishedTrue(Long productId); // <<< ĐÃ THÊM

    /**
     * Tìm sản phẩm cùng category, khác ID, đã publish (có phân trang).
     */
    List<Product> findByCategory_IdAndProductIdNotAndPublishedTrue(Long categoryId, Long productId, Pageable pageable); // <<< ĐÃ THÊM

    /**
     * Lấy top N sản phẩm bán chạy nhất (đã publish).
     */
    @Query("SELECT p FROM Product p WHERE p.published = true ORDER BY p.salesCount DESC")
    Page<Product> findTopSellingPublished(Pageable pageable);

    /**
     * Lấy top N sản phẩm mới nhất (đã publish).
     */
    @Query("SELECT p FROM Product p WHERE p.published = true ORDER BY p.productId DESC")
    Page<Product> findTopNewestPublished(Pageable pageable);

    /**
     * Lấy top N sản phẩm giảm giá nhiều nhất (đã publish).
     */
    @Query("SELECT p FROM Product p WHERE p.published = true AND p.originalPrice IS NOT NULL AND p.originalPrice > p.price " +
           "ORDER BY ((p.originalPrice - p.price) / p.originalPrice) DESC")
    Page<Product> findTopDiscountedPublished(Pageable pageable); // <<< ĐÃ THÊM

    /**
     * Tìm kiếm và lọc sản phẩm public (nên dùng Specification thay thế).
     * Sửa kiểu giá thành BigDecimal.
     */
    @Query("SELECT p FROM Product p WHERE p.published = true " +
           "AND (?1 IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', ?1, '%'))) " +
           "AND (?2 IS NULL OR p.category.id = ?2) " +
           "AND (?3 IS NULL OR p.brand.brandId = ?3) " +
           "AND (?4 IS NULL OR p.price >= ?4) " + // BigDecimal
           "AND (?5 IS NULL OR p.price <= ?5)") // BigDecimal
    List<Product> searchAndFilterPublic(String name, Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice); // Sửa kiểu giá
}