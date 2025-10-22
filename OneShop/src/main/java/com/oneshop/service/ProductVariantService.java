package com.oneshop.service; // Đặt trong package service

import com.oneshop.entity.ProductVariant;
import com.oneshop.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Thêm @Service

@Service // Chú thích đây là một Spring Service
public class ProductVariantService { // Đây là một class, không phải interface

    @Autowired
    private ProductVariantRepository productVariantRepository;

    /**
     * Tìm một biến thể sản phẩm (ProductVariant) bằng ID của nó.
     * @param variantId ID của biến thể cần tìm.
     * @return Đối tượng ProductVariant nếu tìm thấy, ngược lại trả về null.
     */
    public ProductVariant findVariantById(Long variantId) {
        // findById trả về một Optional<ProductVariant>
        // .orElse(null) có nghĩa là: nếu tìm thấy, trả về đối tượng;
        // nếu không, trả về null.
    	return productVariantRepository.findByVariantId(variantId);
    }
    
    // Bạn có thể thêm các phương thức public khác trực tiếp ở đây
    // ví dụ:
    // public List<ProductVariant> findByProductId(Long productId) {
    //     return productVariantRepository.findByProductId(productId); // (Cần thêm hàm này vào repo)
    // }
}