package com.oneshop.service;

import com.oneshop.entity.ProductReview;
import com.oneshop.repository.ProductReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductReviewService {

    @Autowired
    private ProductReviewRepository reviewRepository;

    public List<ProductReview> getReviewsByProductId(Long productId) {
        return reviewRepository.findByProduct_ProductIdOrderByReviewDateDesc(productId);
    }
}
