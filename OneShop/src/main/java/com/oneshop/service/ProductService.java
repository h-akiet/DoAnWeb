package com.oneshop.service.vendor;

import com.oneshop.dto.vendor.ProductDto;
import com.oneshop.entity.vendor.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    /**
     * Lấy danh sách sản phẩm của một shop (phân trang)
     */
    Page<Product> getProductsByShop(Long shopId, Pageable pageable);

    /**
     * Lấy thông tin chi tiết một sản phẩm
     */
    Product getProductById(Long productId);

    /**
     * Thêm một sản phẩm mới
     * @param productDto Thông tin sản phẩm từ DTO
     * @param images Danh sách file ảnh
     * @param shopId ID của shop sở hữu
     */
    Product addProduct(ProductDto productDto, List<MultipartFile> images, Long shopId);

    /**
     * Cập nhật thông tin sản phẩm
     */
    Product updateProduct(Long productId, ProductDto productDto, List<MultipartFile> newImages);

    /**
     * Xóa một sản phẩm
     */
    void deleteProduct(Long productId);
    
    long countProductsByShop(Long shopId);
}