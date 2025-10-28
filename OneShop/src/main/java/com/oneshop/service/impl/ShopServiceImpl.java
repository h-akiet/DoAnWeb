// src/main/java/com/oneshop/service/impl/ShopServiceImpl.java
package com.oneshop.service.impl;

import com.oneshop.dto.ShopDto;
import com.oneshop.entity.Shop;
import com.oneshop.entity.Shop.ShopStatus;
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

import java.util.List; // <<< THÊM IMPORT
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
    
}