// src/main/java/com/oneshop/service/impl/ShopServiceImpl.java
package com.oneshop.service.impl;

import com.oneshop.dto.ShopDto;
import com.oneshop.entity.Shop;
import com.oneshop.enums.ShopStatus;
import com.oneshop.entity.User;
import com.oneshop.repository.ShopRepository;
import com.oneshop.repository.UserRepository;
import com.oneshop.service.FileStorageService;
import com.oneshop.service.ShopService;

import org.slf4j.Logger; // <<< THÊM IMPORT
import org.slf4j.LoggerFactory; // <<< THÊM IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; // <<< THÊM IMPORT
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // <<< THÊM IMPORT
import java.util.Optional;
import java.util.stream.Collectors; // <<< THÊM IMPORT

@Service
public class ShopServiceImpl implements ShopService {

    private static final Logger logger = LoggerFactory.getLogger(ShopServiceImpl.class); // <<< THÊM LOGGER

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true) // <<< THÊM readOnly
    public Shop getShopByUserId(Long userId) {
        logger.debug("Fetching shop by userId: {}", userId); // <<< THÊM LOG
        // Sau này userId sẽ lấy từ user đang đăng nhập
        return shopRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng này chưa có shop"));
    }

    @Override
    @Transactional
    public Shop updateShop(Long shopId, ShopDto shopDto, MultipartFile logoFile, MultipartFile bannerFile) {
        logger.info("Updating shop with ID: {}", shopId); // <<< THÊM LOG
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop"));

        // Cập nhật thông tin cơ bản
        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());

        // Xử lý upload logo mới
        if (logoFile != null && !logoFile.isEmpty()) {
            logger.debug("Processing new logo file for shop {}", shopId); // <<< THÊM LOG
            // Xóa logo cũ nếu có
            if (StringUtils.hasText(shop.getLogo())) {
                 try { // <<< THÊM TRY-CATCH
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
            logger.debug("Processing new banner file for shop {}", shopId); // <<< THÊM LOG
            // Xóa banner cũ nếu có
            if (StringUtils.hasText(shop.getBanner())) {
                 try { // <<< THÊM TRY-CATCH
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
    @Transactional
    public Shop registerShop(ShopDto shopDto, Long userId) {
        logger.info("Registering new shop for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Kiểm tra xem user này đã có shop chưa
        if(shopRepository.findByUserId(userId).isPresent()) {
            logger.warn("User {} already has a shop. Registration aborted.", userId);
            throw new RuntimeException("Người dùng này đã đăng ký shop rồi");
        }

        Shop shop = new Shop();
        shop.setName(shopDto.getShopName());
        shop.setDescription(shopDto.getShopDescription());
        shop.setContactEmail(shopDto.getContactEmail());
        shop.setContactPhone(shopDto.getContactPhone());
        shop.setUser(user);
        shop.setStatus(ShopStatus.PENDING); // <<< GÁN TRẠNG THÁI PENDING KHI TẠO MỚI

        // Liên kết ngược lại từ User tới Shop (quan trọng)
        user.setShop(shop);

        Shop savedShop = shopRepository.save(shop);
        // userRepository.save(user); // Không cần save user nếu cascade hoạt động đúng
        logger.info("Shop {} registered successfully for user {} with status PENDING", savedShop.getId(), userId);
        return savedShop;
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
 // ===>>> THÊM TRIỂN KHAI CÁC PHƯƠNG THỨC MỚI TỪ INTERFACE HOP NHẤT <<<===

    @Override
    @Transactional
    public List<Shop> findAll() {
        logger.debug("Fetching all shops with vendor info.");
		// Giả định shopRepository.findAllWithVendor() tồn tại
		// Vì không có ShopRepository để kiểm tra, giữ nguyên như code cũ
		// Nếu không có, phải dùng shopRepository.findAll()
		return shopRepository.findAllWithVendor(); 
    }
    
    @Override
    @Transactional
    public Shop findById(Long shopId) {
        logger.debug("Fetching shop by ID: {}", shopId);
        // Thay đổi cách xử lý: dùng orElse(null) như code cũ
        return shopRepository.findById(shopId).orElse(null); 
    }

    @Override
    @Transactional
    public List<Shop> getApprovedShops() {
        logger.debug("Fetching all APPROVED shops.");
        return shopRepository.findByStatus(ShopStatus.APPROVED);
    }
    
    @Override
    @Transactional
    public Shop updateShopCommissionRate(Long shopId, BigDecimal newRate) {
        logger.info("Updating commission rate for shop ID: {} to {}", shopId, newRate);
        Optional<Shop> shopOpt = shopRepository.findById(shopId);

        if (shopOpt.isPresent()) {
            Shop shop = shopOpt.get();
            
            // 1. Cập nhật tỉ lệ chiết khấu
            shop.setCommissionRate(newRate);
            
            // 2. Cập nhật cột lưu thời gian là thời điểm hiện tại
            shop.setCommissionUpdatedAt(LocalDateTime.now());
            
            Shop updatedShop = shopRepository.save(shop);
            logger.info("Commission rate updated successfully for shop ID: {}", shopId);
            return updatedShop;
        }
        logger.warn("Shop with ID: {} not found for commission update.", shopId);
        return null;
    }
    
    // ===>>> KẾT THÚC THÊM TRIỂN KHAI <<<===
    
}