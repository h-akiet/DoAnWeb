package com.oneshop.service;

import com.oneshop.dto.ShopDto;
import com.oneshop.entity.Shop;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List; // <<< THÊM IMPORT NÀY

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
     * (Chức năng này hiện không dùng khi đăng ký vendor, sẽ dùng bởi Admin)
     */
    Shop registerShop(ShopDto shopDto, Long userId); // Giữ lại cho Admin

    // ===>>> THÊM PHƯƠNG THỨC NÀY <<<===
    /**
     * Lấy danh sách tất cả các Shop đang hoạt động (ví dụ: User của Shop đang active).
     * @return List<Shop>
     */
    List<Shop> findAllActiveShops();
    // ===>>> KẾT THÚC <<<===
    
 // ===>>> THÊM CÁC PHƯƠNG THỨC TỪ CLASS CŨ VÀO INTERFACE <<<===

    /**
     * Lấy tất cả shop, kèm theo thông tin Vendor (giả định có hàm findAllWithVendor trong repo)
     */
    List<Shop> findAll();

    /**
     * Tìm shop bằng ID
     */
    Shop findById(Long shopId);

    /**
     * Lấy danh sách các shop đã được duyệt
     */
    List<Shop> getApprovedShops();

    /**
     * Cập nhật tỉ lệ chiết khấu (commission rate) cho shop
     */
    Shop updateShopCommissionRate(Long shopId, BigDecimal newRate);

    // ===>>> KẾT THÚC THÊM PHƯƠNG THỨC <<<===
}
