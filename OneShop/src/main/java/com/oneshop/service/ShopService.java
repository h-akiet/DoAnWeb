package com.oneshop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oneshop.entity.Shop;
import com.oneshop.enums.ShopStatus;
import com.oneshop.repository.ShopRepository;
@Service
public class ShopService {

	private final ShopRepository shopRepository;

    @Autowired
    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Transactional
	public List<Shop> findAll() {
		return shopRepository.findAllWithVendor(); 
	}
    
    @Transactional
    public Shop findById(Long shopId) {
        return shopRepository.findById(shopId).orElse(null); 
    }
    public List<Shop> getApprovedShops() {
        return shopRepository.findByStatus(ShopStatus.APPROVED);
    }
    
    @Transactional
    public Shop updateShopCommissionRate(Long shopId, BigDecimal newRate) {
        Optional<Shop> shopOpt = shopRepository.findById(shopId);

        if (shopOpt.isPresent()) {
            Shop shop = shopOpt.get();
            
            // 1. Cập nhật tỉ lệ chiết khấu
            shop.setCommissionRate(newRate);
            
            // 2. Cập nhật cột lưu thời gian là thời điểm hiện tại
            shop.setCommissionUpdatedAt(LocalDateTime.now());
            
            return shopRepository.save(shop);
        }
        return null;
    }
}
// Code cũ==========================================
package com.oneshop.service;

import com.oneshop.dto.ShopDto;
import com.oneshop.entity.Shop;

import org.springframework.web.multipart.MultipartFile;
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
}
