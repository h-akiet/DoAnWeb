package com.oneshop.repository.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.vendor.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Tìm tất cả sản phẩm thuộc về một Shop (có phân trang)
    Page<Product> findByShopId(Long shopId, Pageable pageable);

    // Tìm sản phẩm theo Shop ID và Tên sản phẩm (hỗ trợ tìm kiếm)
    Page<Product> findByShopIdAndNameContainingIgnoreCase(Long shopId, String name, Pageable pageable);
    
    long countByShopId(Long shopId);
    long countByCategoryId(Long categoryId);
}