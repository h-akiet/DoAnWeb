package com.oneshop.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.ProductReview;
import com.oneshop.entity.Order;
import com.oneshop.entity.Product;

import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Optional<ProductReview> findByOrderAndProduct(Order order, Product product); // Kiểm tra review cho product trong order

    
    @Query("SELECT COALESCE(AVG(pr.rating), 0.0) FROM ProductReview pr WHERE pr.product.productId = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(pr) FROM ProductReview pr WHERE pr.product.productId = :productId")
    Integer countReviewsByProductId(@Param("productId") Long productId);
    
    List<ProductReview> findByProduct_ProductIdOrderByReviewDateDesc(Long productId);

}