package com.oneshop.service.vendor.impl;

import com.oneshop.dto.vendor.ShopDto;
import com.oneshop.entity.vendor.Shop;
import com.oneshop.entity.vendor.User;
import com.oneshop.repository.vendor.ShopRepository;
import com.oneshop.repository.vendor.UserRepository;
import com.oneshop.service.vendor.FileStorageService;
import com.oneshop.service.vendor.ShopService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private ShopRepository shopRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public Shop getShopByUserId(Long userId) {
        // Sau này userId sẽ lấy từ user đang đăng nhập
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng này chưa có shop"));
    }

    @Override
    @Transactional
    public Shop updateShop(Long shopId, ShopDto shopDto, MultipartFile logoFile, MultipartFile bannerFile) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop"));

        // Cập nhật thông tin cơ bản
        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());

        // Xử lý upload logo mới
        if (logoFile != null && !logoFile.isEmpty()) {
            // Xóa logo cũ nếu có
            if (StringUtils.hasText(shop.getLogo())) {
                fileStorageService.delete(shop.getLogo());
            }
            // Lưu logo mới
            String logoFilename = fileStorageService.save(logoFile);
            shop.setLogo(logoFilename);
        }

        // Xử lý upload banner mới
        if (bannerFile != null && !bannerFile.isEmpty()) {
            // Xóa banner cũ nếu có
            if (StringUtils.hasText(shop.getBanner())) {
                fileStorageService.delete(shop.getBanner());
            }
            // Lưu banner mới
            String bannerFilename = fileStorageService.save(bannerFile);
            shop.setBanner(bannerFilename);
        }

        return shopRepository.save(shop);
    }
    
    @Override
    @Transactional
    public Shop registerShop(ShopDto shopDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        // Kiểm tra xem user này đã có shop chưa
        if(shopRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("Người dùng này đã đăng ký shop rồi");
        }

        Shop shop = new Shop();
        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());
        shop.setUser(user);
        
        return shopRepository.save(shop);
    }
}