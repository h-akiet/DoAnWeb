package com.oneshop.service.impl;

import com.oneshop.dto.CartDto;
import com.oneshop.dto.PromotionDto;
import com.oneshop.entity.Promotion;
import com.oneshop.entity.PromotionTypeEntity; // <-- KHÔI PHỤC: Import Entity
import com.oneshop.entity.Shop;
import com.oneshop.repository.PromotionRepository;
import com.oneshop.repository.PromotionTypeRepository; // <-- KHÔI PHỤC: Import Repository
import com.oneshop.repository.ShopRepository;
import com.oneshop.service.CartService;
import com.oneshop.service.PromotionService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;

@Service
public class PromotionServiceImpl implements PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionServiceImpl.class);

    // === KHÔI PHỤC KEY CHO SESSION ===
    public static final String VOUCHER_CODE_SESSION_KEY = "appliedVoucherCode";
    public static final String VOUCHER_DISCOUNT_SESSION_KEY = "appliedDiscountAmount";
    public static final String VOUCHER_TYPE_CODE_SESSION_KEY = "appliedVoucherTypeCode";   // <-- ĐÃ THÊM
    public static final String VOUCHER_VALUE_SESSION_KEY = "appliedVoucherValue";
    // Đã xóa VOUCHER_TYPE_SESSION_KEY và VOUCHER_VALUE_SESSION_KEY
    // =================================

    @Autowired private PromotionRepository promotionRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private PromotionTypeRepository promotionTypeRepository; // <-- KHÔI PHỤC
    @Autowired private CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public List<Promotion> getPromotionsByShop(Long shopId) {
        logger.debug("Fetching promotions for shopId: {}", shopId);
        return promotionRepository.findByShopId(shopId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Promotion createPromotion(PromotionDto promotionDto, Long shopId) {
        logger.info("Creating new promotion for shopId: {}", shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy shop với ID: " + shopId));

        // === KHÔI PHỤC: Tìm PromotionTypeEntity bằng ID ===
        PromotionTypeEntity promotionType = promotionTypeRepository.findById(promotionDto.getPromotionTypeId()) // Dùng promotionTypeId
                .orElseThrow(() -> new IllegalArgumentException("Loại khuyến mãi không hợp lệ."));
        // ==================================================
        
        Promotion promotion = new Promotion();
        // Gán dữ liệu và validate
        mapDtoToEntity(promotionDto, promotion, promotionType, shop); // <-- Thêm promotionType vào tham số

        Promotion savedPromotion = promotionRepository.save(promotion);
        logger.info("Promotion created successfully with ID: {}", savedPromotion.getId());
        return savedPromotion;
    }

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        if (attr == null) {
            throw new RuntimeException("Không thể truy cập RequestContext. Service này có đang được gọi từ một HTTP request không?");
        }
        HttpServletRequest request = attr.getRequest();
        return request.getSession(true); // true = tạo nếu chưa có
    }

    @Override
    @Transactional(readOnly = true) // Chỉ đọc CSDL, không ghi
    public CartDto applyVoucher(String username, String voucherCode) throws IllegalArgumentException {
        // 1. Lấy giỏ hàng (DTO)
        CartDto cart = cartService.getCartForUser(username);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống, không thể áp dụng voucher.");
        }

        // 2. Tìm khuyến mãi trong CSDL (Đây là nơi lỗi xảy ra)
        LocalDate today = LocalDate.now();
        
        // DÒNG NÀY PHẢI ĐÚNG NHƯ SAU:
        Promotion promo = promotionRepository.findByDiscountCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                voucherCode, today, today)
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không hợp lệ hoặc đã hết hạn."));
        // KẾT THÚC DÒNG NÀY

        // 3. Kiểm tra điều kiện áp dụng
        if (!isPromotionApplicable(promo, cart, username)) { // <-- Sử dụng 'promo'
             throw new IllegalArgumentException("Voucher không đủ điều kiện áp dụng cho giỏ hàng này.");
        }

        // 4. Tính toán giảm giá
        BigDecimal discountAmount = calculateDiscount(promo, cart.getSubtotal()); // <-- Sử dụng 'promo'

        // 5. LƯU VÀO HTTP SESSION
        HttpSession session = getSession();
        session.setAttribute(VOUCHER_CODE_SESSION_KEY, promo.getDiscountCode()); // <-- Sử dụng 'promo'
        session.setAttribute(VOUCHER_DISCOUNT_SESSION_KEY, discountAmount);
        
        // SỬA LỖI TẠI ĐÂY: Bạn có thể đã dùng 'promo' trong các dòng dưới mà nó không tồn tại
        session.setAttribute(VOUCHER_TYPE_CODE_SESSION_KEY, promo.getType().getCode()); 
        session.setAttribute(VOUCHER_VALUE_SESSION_KEY, promo.getValue()); 
        

        // 6. Cập nhật DTO để trả về cho JavaScript
        cart.setAppliedVoucherCode(promo.getDiscountCode());
        cart.setDiscountAmount(discountAmount);
        
        cart.setAppliedVoucherTypeCode(promo.getType().getCode()); 
        cart.setAppliedVoucherValue(promo.getValue()); 
        
        cart.calculateTotals();

        return cart;
    }

     @Override
     @Transactional(readOnly = true)
     public CartDto removeVoucher(String username) {
         HttpSession session = getSession();
         
         // 1. XÓA KHỎI SESSION (XÓA CẢ TYPE VÀ VALUE)
         session.removeAttribute(VOUCHER_CODE_SESSION_KEY);
         session.removeAttribute(VOUCHER_DISCOUNT_SESSION_KEY);
         session.removeAttribute(VOUCHER_TYPE_CODE_SESSION_KEY);   // <-- ĐÃ THÊM
         session.removeAttribute(VOUCHER_VALUE_SESSION_KEY); // <-- ĐÃ THÊM
         
         // 2. Tải lại CartDto sạch (Service phải được cập nhật)
         return cartService.getCartForUser(username);
     }

     @Override
     public List<Promotion> findApplicablePromotions(CartDto cart, String username) {
         LocalDate today = LocalDate.now();
         List<Promotion> activePromotions = promotionRepository.findActivePromotions(today);

         return activePromotions.stream()
             .filter(promo -> isPromotionApplicable(promo, cart, username))
             .collect(Collectors.toList());
     }

     private boolean isPromotionApplicable(Promotion promo, CartDto cart, String username) {
         if (promo == null || cart == null || cart.getSubtotal() == null) {
             return false;
         }
         // Thêm các quy tắc của bạn ở đây
         return true; 
     }

    // === KHÔI PHỤC: Logic tính toán dùng TypeEntity.getCode() ===
    private BigDecimal calculateDiscount(Promotion promo, BigDecimal subtotal) {
        if (promo == null || promo.getType() == null) {
            return BigDecimal.ZERO;
        }

        // Dùng code của PromotionTypeEntity
        String typeCode = promo.getType().getCode(); 
        BigDecimal value = promo.getValue();
        
        // Cần kiểm tra code của bạn là gì (vd: PERCENT, FIXED, FREESHIP)
        if (value == null) {
             if ("FREE_SHIPPING".equalsIgnoreCase(typeCode)) {
                 return BigDecimal.ZERO;
             }
             return BigDecimal.ZERO;
        }

        if ("PERCENTAGE".equalsIgnoreCase(typeCode)) {
            BigDecimal discount = subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            return discount;
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(typeCode)) {
            return value.min(subtotal); // Giảm không quá tổng tiền
        } else if ("FREE_SHIPPING".equalsIgnoreCase(typeCode)) {
            return BigDecimal.ZERO; // Sẽ xử lý ở logic tính phí ship
        }

        return BigDecimal.ZERO;
    }
    // =========================================================

    @Override
    @Transactional
    public void deletePromotion(Long promotionId, Long shopId) {
        logger.warn("Attempting to delete promotion ID: {} for shop ID: {}", promotionId, shopId);
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi với ID: " + promotionId));

        if (promotion.getShop() == null || !promotion.getShop().getId().equals(shopId)) {
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
        logger.debug("Fetching active and upcoming promotions with pageable: {}", pageable);
        return promotionRepository.findByEndDateAfter(LocalDate.now().minusDays(1), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionByIdAndShopId(Long promotionId, Long shopId) {
        logger.debug("Fetching promotion ID: {} for shop ID: {}", promotionId, shopId);
        return promotionRepository.findById(promotionId)
                .filter(promo -> promo.getShop() != null && promo.getShop().getId().equals(shopId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Promotion updatePromotion(Long promotionId, PromotionDto promotionDto, Long shopId) {
        logger.info("Updating promotion ID: {} for shop ID: {}", promotionId, shopId);
        Promotion existingPromotion = getPromotionByIdAndShopId(promotionId, shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi hoặc bạn không có quyền sửa."));

        // === KHÔI PHỤC: Tìm PromotionTypeEntity bằng ID ===
        PromotionTypeEntity promotionType = promotionTypeRepository.findById(promotionDto.getPromotionTypeId()) // Dùng promotionTypeId
                .orElseThrow(() -> new IllegalArgumentException("Loại khuyến mãi không hợp lệ."));
        // ==================================================

        // Map dữ liệu từ DTO vào entity đang tồn tại
        mapDtoToEntity(promotionDto, existingPromotion, promotionType, existingPromotion.getShop()); // <-- Thêm promotionType vào tham số

        Promotion updatedPromotion = promotionRepository.save(existingPromotion);
        logger.info("Promotion ID: {} updated successfully.", updatedPromotion.getId());
        return updatedPromotion;
    }

    // --- KHÔI PHỤC: HÀM HELPER ĐỂ MAP DTO -> ENTITY ---
    private void mapDtoToEntity(PromotionDto dto, Promotion entity, PromotionTypeEntity type, Shop shop) {
        entity.setCampaignName(dto.getCampaignName().trim());
        entity.setDiscountCode(dto.getDiscountCode().trim());
        entity.setType(type); // Gán PromotionTypeEntity
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setShop(shop); 

        // Validate ngày
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        // Validate giá trị giảm dựa trên loại
        if (!"FREE_SHIPPING".equals(type.getCode())) { // Giả sử code là trường định danh loại
            if (dto.getDiscountValue() == null || dto.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
                 throw new IllegalArgumentException("Giá trị giảm không hợp lệ cho loại khuyến mãi '" + type.getName() + "'.");
            }
            if ("PERCENTAGE".equalsIgnoreCase(type.getCode()) && dto.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Giá trị giảm theo % không được lớn hơn 100.");
            }
             entity.setValue(dto.getDiscountValue());
        } else {
            entity.setValue(null); // FREESHIP không cần giá trị
        }
    }
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscountForOrder(CartDto cart, BigDecimal subtotal) {
        // Phương thức này chỉ tính số tiền giảm, KHÔNG xử lý Free Shipping.
        // Logic Free Shipping (shippingCost = 0) sẽ được xử lý trong OrderServiceImpl.
        
        if (cart.getAppliedVoucherCode() == null || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Lấy thông tin voucher đã được lưu trong CartDto
        String typeCode = cart.getAppliedVoucherTypeCode();
        BigDecimal value = cart.getAppliedVoucherValue();
        
        if (value == null) {
            // Chỉ Free Ship mới có thể có value null
            return BigDecimal.ZERO; 
        }
        
        // Đảm bảo Order Service đã được sửa để so sánh với các tên ENUM đầy đủ.
        
        if ("PERCENTAGE".equalsIgnoreCase(typeCode)) {
            // Tính % giảm: subtotal * (value / 100)
            BigDecimal discount = subtotal.multiply(value)
                                    .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
            
            // Trả về số tiền đã làm tròn về 0 chữ số thập phân
            return discount.setScale(0, java.math.RoundingMode.HALF_UP).max(BigDecimal.ZERO); 

        } else if ("FIXED_AMOUNT".equalsIgnoreCase(typeCode)) {
            // Giảm tiền cố định, không vượt quá tổng phụ
            return value.min(subtotal).setScale(0, java.math.RoundingMode.HALF_UP).max(BigDecimal.ZERO); 

        } else if ("FREE_SHIPPING".equalsIgnoreCase(typeCode)) {
            // Loại này không tạo ra số tiền giảm trên Subtotal
            return BigDecimal.ZERO; 
        }

        return BigDecimal.ZERO;
    }
    @Override
    @Transactional(readOnly = true)
    public Optional<Promotion> findByDiscountCode(String code) {
        // Giả sử bạn có PromotionRepository đã được autowired
        return promotionRepository.findByDiscountCode(code);
    }
    
}