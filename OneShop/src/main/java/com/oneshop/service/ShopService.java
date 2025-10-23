package com.oneshop.service.vendor;

import com.oneshop.dto.vendor.ShopDto;
import com.oneshop.entity.vendor.Shop;

import org.springframework.web.multipart.MultipartFile;

public interface ShopService {

    /**
     * Lấy thông tin shop bằng ID của chủ sở hữu (user)
     */
    Shop getShopByUserId(Long userId);

    /**
     * Cập nhật thông tin shop
     * @param shopId ID của shop cần cập nhật
     * @param shopDto Dữ liệu thông tin mới
     * @param logoFile File logo mới (có thể null)
     * @param bannerFile File banner mới (có thể null)
     */
    Shop updateShop(Long shopId, ShopDto shopDto, MultipartFile logoFile, MultipartFile bannerFile);
    
    /**
     * Đăng ký shop mới cho một User
     * (Đây là chức năng "Đăng ký shop" bạn đã yêu cầu)
     */
    Shop registerShop(ShopDto shopDto, Long userId);
}