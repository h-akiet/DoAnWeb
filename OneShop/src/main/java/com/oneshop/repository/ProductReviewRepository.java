package com.oneshop.repository;


import com.oneshop.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    /**
     * Tính toán điểm đánh giá trung bình cho một sản phẩm.
     * COALESCE(..., 0.0) sẽ trả về 0.0 nếu sản phẩm chưa có đánh giá nào (tránh lỗi null).
     * @param productId ID của sản phẩm cần tính.
     * @return Điểm rating trung bình.
     */
    @Query("SELECT COALESCE(AVG(pr.rating), 0.0) FROM ProductReview pr WHERE pr.product.productId = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    /**
     * Đếm tổng số lượt đánh giá cho một sản phẩm.
     * @param productId ID của sản phẩm cần đếm.
     * @return Tổng số review.
     */
    @Query("SELECT COUNT(pr) FROM ProductReview pr WHERE pr.product.productId = :productId")
    Integer countReviewsByProductId(@Param("productId") Long productId);

}