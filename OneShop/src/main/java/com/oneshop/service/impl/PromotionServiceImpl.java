package com.oneshop.service.impl;

import com.oneshop.dto.PromotionDto;
import com.oneshop.entity.Promotion;
import com.oneshop.entity.PromotionTypeEntity;
import com.oneshop.entity.Shop;
import com.oneshop.repository.PromotionRepository;
import com.oneshop.repository.PromotionTypeRepository;
import com.oneshop.repository.ShopRepository;
import com.oneshop.service.PromotionService;

import jakarta.persistence.EntityNotFoundException; // Thêm EntityNotFoundException
import org.slf4j.Logger; // Thêm Logger
import org.slf4j.LoggerFactory; // Thêm LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Thêm StringUtils

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional; // Thêm Optional

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;

@Service
public class PromotionServiceImpl implements PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionServiceImpl.class); // Thêm Logger

    @Autowired private PromotionRepository promotionRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private PromotionTypeRepository promotionTypeRepository;

    @Override
    @Transactional(readOnly = true) // Thêm readOnly
    public List<Promotion> getPromotionsByShop(Long shopId) {
        logger.debug("Fetching promotions for shopId: {}", shopId); // Thêm log
        return promotionRepository.findByShopId(shopId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // Thêm rollbackFor
    public Promotion createPromotion(PromotionDto promotionDto, Long shopId) {
        logger.info("Creating new promotion for shopId: {}", shopId); // Thêm log
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy shop với ID: " + shopId)); // Sửa lỗi

        PromotionTypeEntity promotionType = promotionTypeRepository.findById(promotionDto.getPromotionTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Loại khuyến mãi không hợp lệ.")); // Sửa lỗi

        // Kiểm tra trùng mã giảm giá (cho shop này) - Nên có unique constraint ở DB nữa
        // if (promotionRepository.existsByShopIdAndDiscountCodeIgnoreCase(shopId, promotionDto.getDiscountCode().trim())) {
        //     throw new IllegalArgumentException("Mã giảm giá '" + promotionDto.getDiscountCode().trim() + "' đã tồn tại cho shop này.");
        // }
        // ---> Cần thêm hàm existsByShopIdAndDiscountCodeIgnoreCase vào PromotionRepository nếu muốn check ở đây

        Promotion promotion = new Promotion();
        // Gán dữ liệu và validate
        mapDtoToEntity(promotionDto, promotion, promotionType, shop);

        Promotion savedPromotion = promotionRepository.save(promotion);
        logger.info("Promotion created successfully with ID: {}", savedPromotion.getId());
        return savedPromotion;
    }

    @Override
    @Transactional
    public void deletePromotion(Long promotionId, Long shopId) {
        logger.warn("Attempting to delete promotion ID: {} for shop ID: {}", promotionId, shopId); // Thêm log
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi với ID: " + promotionId)); // Sửa lỗi

        if (promotion.getShop() == null || !promotion.getShop().getId().equals(shopId)) { // Kiểm tra shop null
            logger.warn("Security violation: Shop {} tried to delete promotion {} of shop {}",
                        shopId, promotionId, promotion.getShop() != null ? promotion.getShop().getId() : "null");
            throw new SecurityException("Bạn không có quyền xóa khuyến mãi này");
        }

        promotionRepository.delete(promotion);
        logger.info("Promotion ID: {} deleted successfully.", promotionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Promotion> findActiveAndUpcomingPromotions(Pageable pageable) {
        logger.debug("Fetching active and upcoming promotions with pageable: {}", pageable); // Thêm log
        return promotionRepository.findByEndDateAfter(LocalDate.now().minusDays(1), pageable);
    }

    // --- CÁC PHƯƠNG THỨC MỚI ĐƯỢC THÊM ---

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionByIdAndShopId(Long promotionId, Long shopId) {
        logger.debug("Fetching promotion ID: {} for shop ID: {}", promotionId, shopId);
        return promotionRepository.findById(promotionId)
                .filter(promo -> promo.getShop() != null && promo.getShop().getId().equals(shopId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // Thêm rollbackFor
    public Promotion updatePromotion(Long promotionId, PromotionDto promotionDto, Long shopId) {
        logger.info("Updating promotion ID: {} for shop ID: {}", promotionId, shopId);
        Promotion existingPromotion = getPromotionByIdAndShopId(promotionId, shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi hoặc bạn không có quyền sửa."));

        PromotionTypeEntity promotionType = promotionTypeRepository.findById(promotionDto.getPromotionTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Loại khuyến mãi không hợp lệ."));

        // Kiểm tra nếu mã giảm giá thay đổi và bị trùng với mã khác của cùng shop
        String newDiscountCode = promotionDto.getDiscountCode().trim();
        // if (!existingPromotion.getDiscountCode().equalsIgnoreCase(newDiscountCode) &&
        //     promotionRepository.existsByShopIdAndDiscountCodeIgnoreCase(shopId, newDiscountCode)) {
        //     throw new IllegalArgumentException("Mã giảm giá '" + newDiscountCode + "' đã tồn tại cho shop này.");
        // }
        // ---> Cần thêm hàm existsByShopIdAndDiscountCodeIgnoreCase vào PromotionRepository

        // Map dữ liệu từ DTO vào entity đang tồn tại
        mapDtoToEntity(promotionDto, existingPromotion, promotionType, existingPromotion.getShop());

        Promotion updatedPromotion = promotionRepository.save(existingPromotion);
        logger.info("Promotion ID: {} updated successfully.", updatedPromotion.getId());
        return updatedPromotion;
    }

    // --- HÀM HELPER ĐỂ MAP DTO -> ENTITY ---
    private void mapDtoToEntity(PromotionDto dto, Promotion entity, PromotionTypeEntity type, Shop shop) {
        entity.setCampaignName(dto.getCampaignName().trim());
        entity.setDiscountCode(dto.getDiscountCode().trim());
        entity.setType(type);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setShop(shop); // Gán shop (cần thiết cho create, không đổi khi update)

        // Validate ngày
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        // Validate giá trị giảm dựa trên loại
        if (!"FREE_SHIPPING".equals(type.getCode())) { // Giả sử code là trường định danh loại
            if (dto.getDiscountValue() == null || dto.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
                 throw new IllegalArgumentException("Giá trị giảm không hợp lệ cho loại khuyến mãi '" + type.getName() + "'.");
            }
            // Thêm kiểm tra % không quá 100 nếu cần
            if ("PERCENTAGE".equals(type.getCode()) && dto.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Giá trị giảm theo % không được lớn hơn 100.");
            }
             entity.setValue(dto.getDiscountValue());
        } else {
            entity.setValue(null); // FREE_SHIPPING không cần giá trị
        }
    }
}