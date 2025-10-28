// src/main/java/com/oneshop/service/impl/ShopServiceImpl.java
package com.oneshop.service.impl;

import com.oneshop.dto.ShopDto;
import com.oneshop.entity.Role; // <<< THÊM IMPORT ROLE
import com.oneshop.entity.Shop;
import com.oneshop.entity.Shop.ShopStatus;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository; // <<< THÊM IMPORT ROLE REPOSITORY
import com.oneshop.repository.ShopRepository;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.FileStorageService;
import com.oneshop.service.ShopService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl implements ShopService {

    private static final Logger logger = LoggerFactory.getLogger(ShopServiceImpl.class);

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired // <<< INJECT ROLE REPOSITORY
    private RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Shop getShopByUserId(Long userId) {
        logger.debug("Fetching shop by userId: {}", userId);
        // Sau này userId sẽ lấy từ user đang đăng nhập
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng này chưa có shop"));
    }

    @Override
    @Transactional
    public Shop updateShop(Long shopId, ShopDto shopDto, MultipartFile logoFile, MultipartFile bannerFile) {
        logger.info("Updating shop with ID: {}", shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop"));

        // Cập nhật thông tin cơ bản
        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());

        // Xử lý upload logo mới
        if (logoFile != null && !logoFile.isEmpty()) {
            logger.debug("Processing new logo file for shop {}", shopId);
            // Xóa logo cũ nếu có
            if (StringUtils.hasText(shop.getLogo())) {
                 try {
                    fileStorageService.delete(shop.getLogo());
                    logger.debug("Deleted old logo: {}", shop.getLogo());
                 } catch (Exception e) {
                     logger.error("Error deleting old logo file {}: {}", shop.getLogo(), e.getMessage());
                 }
            }
            // Lưu logo mới
            String logoFilename = fileStorageService.save(logoFile);
            shop.setLogo(logoFilename);
            logger.debug("Saved new logo as: {}", logoFilename);
        }

        // Xử lý upload banner mới
        if (bannerFile != null && !bannerFile.isEmpty()) {
            logger.debug("Processing new banner file for shop {}", shopId);
            // Xóa banner cũ nếu có
            if (StringUtils.hasText(shop.getBanner())) {
                 try {
                    fileStorageService.delete(shop.getBanner());
                    logger.debug("Deleted old banner: {}", shop.getBanner());
                 } catch (Exception e) {
                      logger.error("Error deleting old banner file {}: {}", shop.getBanner(), e.getMessage());
                 }
            }
            // Lưu banner mới
            String bannerFilename = fileStorageService.save(bannerFile);
            shop.setBanner(bannerFilename);
             logger.debug("Saved new banner as: {}", bannerFilename);
        }

        return shopRepository.save(shop);
    }

    @Override
    @Transactional // Đảm bảo toàn bộ quá trình là một transaction
    public Shop registerShop(ShopDto shopDto, Long userId) {
        logger.info("Registering new shop for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        // Kiểm tra xem user này đã có shop chưa
        if(shopRepository.findByUserId(userId).isPresent()) {
            logger.warn("User {} already has a shop. Registration aborted.", userId);
            throw new RuntimeException("Người dùng này đã đăng ký shop rồi");
        }

        // === BƯỚC 1: TẠO VÀ LƯU SHOP ===
        Shop shop = new Shop();
        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());
        shop.setUser(user);
        shop.setStatus(ShopStatus.PENDING); // Trạng thái chờ duyệt

        Shop savedShop = shopRepository.save(shop);
        logger.info("Shop {} registered successfully for user {} with status PENDING", savedShop.getId(), userId);

        // === BƯỚC 2: CẬP NHẬT ROLE CHO USER ===
        try {
            // Tìm Role VENDOR (giả sử bạn đã có Role này trong DB, nếu không cần tạo trước)
            Role vendorRole = roleRepository.findByName(Role.RoleName.VENDOR)
                    .orElseThrow(() -> {
                        logger.error("FATAL: VENDOR role not found in database!");
                        return new RuntimeException("Lỗi hệ thống: Không tìm thấy vai trò VENDOR.");
                    });

            // Cập nhật role cho user
            user.setRole(vendorRole);
            user.setShop(savedShop); // Liên kết shop vừa tạo với user

            // Lưu lại user với role mới
            userRepository.save(user);
            logger.info("Updated user {} role to VENDOR successfully.", userId);

        } catch (Exception e) {
            // Nếu có lỗi khi cập nhật role, transaction sẽ rollback việc tạo shop
            logger.error("Error updating user role to VENDOR for userId {}: {}", userId, e.getMessage(), e);
            // Ném lại lỗi để transaction rollback
            throw new RuntimeException("Không thể cập nhật vai trò người dùng sau khi đăng ký shop.", e);
        }

        return savedShop; // Trả về shop đã được lưu
    }


    @Override
    @Transactional(readOnly = true)
    public List<Shop> findAllActiveShops() {
        logger.debug("Finding all active (APPROVED) shops.");
        // Cập nhật để chỉ lấy shop đã duyệt (nếu cần cho trang liên hệ)
        // return shopRepository.findByStatus(ShopStatus.APPROVED, Sort.by("name"));
        // Hoặc giữ nguyên nếu trang liên hệ muốn hiển thị cả shop chờ duyệt
        return shopRepository.findAll(Sort.by("name"));
    }

}