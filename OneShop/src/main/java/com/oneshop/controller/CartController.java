package com.oneshop.controller;

import com.oneshop.dto.ApplyVoucherRequest;
import com.oneshop.dto.CartDto;
import com.oneshop.dto.UpdateCartRequest; // DTO cho AJAX cập nhật
import com.oneshop.entity.Promotion;
import com.oneshop.service.CartService;
import com.oneshop.service.PromotionService;

import org.slf4j.Logger; // Thêm Logger
import org.slf4j.LoggerFactory; // Thêm LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Thêm RedirectAttributes

import java.util.List;
import java.util.Map;

@Controller
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class); // Thêm Logger

    @Autowired
    private CartService cartService;
    
    @Autowired
    private PromotionService promotion;

    /** Lấy trang giỏ hàng (GET) */
    @GetMapping("/cart")
    public String viewCart(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             logger.debug("User not authenticated, redirecting to login.");
             return "redirect:/login";
        }
        String username = authentication.getName();
        logger.debug("Viewing cart for user: {}", username);
        CartDto cart = cartService.getCartForUser(username);

      
        List<Promotion> promos = promotion.findApplicablePromotions(cart, username);
        // Use the key expected by your cart.html template
        model.addAttribute("applicableVouchers", promos); // Or "applicablePromotions" if your template uses that

        model.addAttribute("cart", cart);
        return "user/cart"; // Return the view name
    }

    /** Thêm sản phẩm vào giỏ (Dùng cho redirect cũ, có thể xóa nếu không cần) */
    @GetMapping("/cart/add/{variantId}")
    public String addToCartRedirect(@PathVariable("variantId") Long variantId,
                            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
                            Authentication authentication, RedirectAttributes redirectAttributes) {
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             logger.debug("User not authenticated, redirecting to login.");
             return "redirect:/login";
         }
        String username = authentication.getName();
        logger.debug("Adding variantId {} (qty: {}) to cart for user {} via redirect.", variantId, quantity, username);
        try {
            cartService.addItemToCart(username, variantId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (RuntimeException e) {
             logger.warn("Error adding item to cart via redirect for user {}: {}", username, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
             return "redirect:/cart";
        }
        return "redirect:/cart";
    }

    /** Xóa sản phẩm khỏi giỏ (Dùng cho trang cart.html) */
    @GetMapping("/cart/remove/{variantId}")
    public String removeFromCart(@PathVariable("variantId") Long variantId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             logger.debug("User not authenticated, redirecting to login.");
             return "redirect:/login";
        }
        String username = authentication.getName();
        logger.info("Attempting to remove variantId {} from cart for user {}", variantId, username);
        try {
            cartService.removeItemFromCart(username, variantId);
            logger.info("Successfully removed variantId {} for user {}", variantId, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (RuntimeException e) {
            logger.error("Error removing item variantId {} for user {}: {}", variantId, username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    /** Cập nhật số lượng (POST bằng AJAX - Dùng trong cart.html) */
    @PostMapping("/api/cart/update")
    @ResponseBody
    public ResponseEntity<?> updateCartItem(
            @RequestBody UpdateCartRequest request,
            Authentication authentication
    ) {
         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                  .body(Map.of("message", "Vui lòng đăng nhập."));
         }
        String username = authentication.getName();
        logger.debug("Updating cart for user {}, variantId: {}, quantity: {}", username, request.getVariantId(), request.getQuantity());
        try {
            CartDto updatedCart = cartService.updateItemQuantity(
                username,
                request.getVariantId(),
                request.getQuantity()
            );
            return ResponseEntity.ok(updatedCart);
        } catch (RuntimeException e) {
             logger.warn("Error updating cart for user {}: {}", username, e.getMessage());
             return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Endpoint API để thêm sản phẩm vào giỏ bằng AJAX (Dùng trong product.html) */
    @PostMapping("/api/cart/add/{variantId}")
    @ResponseBody
    public ResponseEntity<?> addToCartApi(
            @PathVariable("variantId") Long variantId,
            @RequestBody AddItemRequest request, // Nhận quantity từ body
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("message", "Vui lòng đăng nhập để thêm vào giỏ hàng."));
        }
        String username = authentication.getName();
        int quantity = (request != null && request.getQuantity() > 0) ? request.getQuantity() : 1;
        logger.debug("API request to add variantId {} (qty: {}) for user {}", variantId, quantity, username);

        try {
            CartDto updatedCart = cartService.addItemToCart(username, variantId, quantity);
            // Trả về cả CartDto mới để JS cập nhật dropdown
            return ResponseEntity.ok(Map.of(
                "message", "Thêm vào giỏ thành công!",
                "totalItems", updatedCart.getTotalItems(),
                "cart", updatedCart // <<< Trả về cart mới
            ));
        } catch (RuntimeException e) {
            logger.warn("API error adding item for user {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
             logger.error("API system error adding item for user {}: {}", username, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Map.of("message", "Lỗi hệ thống khi thêm vào giỏ hàng."));
        }
    }

    // ========== ENDPOINT API XÓA SẢN PHẨM KHỎI GIỎ (MỚI) ==========
    /** Dùng cho nút xóa trong header dropdown */
    @PostMapping("/api/cart/remove/{variantId}") // Có thể dùng @DeleteMapping nếu muốn
    @ResponseBody
    public ResponseEntity<?> removeFromCartApi(
            @PathVariable("variantId") Long variantId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("message", "Vui lòng đăng nhập."));
        }
        String username = authentication.getName();
        logger.info("API request to remove variantId {} from cart for user {}", variantId, username);

        try {
            cartService.removeItemFromCart(username, variantId); // Gọi service để xóa
            // Lấy lại giỏ hàng sau khi xóa để trả về client
            CartDto updatedCart = cartService.getCartForUser(username);
            logger.info("Successfully removed variantId {} via API for user {}", variantId, username);
            // Trả về CartDto mới và số lượng item
            return ResponseEntity.ok(Map.of(
                "message", "Đã xóa sản phẩm khỏi giỏ hàng.",
                "totalItems", updatedCart.getTotalItems(),
                "cart", updatedCart // <<< Trả về cart mới
            ));
        } catch (RuntimeException e) {
            logger.error("API error removing item variantId {} for user {}: {}", variantId, username, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi khi xóa sản phẩm: " + e.getMessage()));
        } catch (Exception e) {
             logger.error("API system error removing item variantId {} for user {}: {}", variantId, username, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Map.of("message", "Lỗi hệ thống khi xóa sản phẩm."));
        }
    }
    
    @PostMapping("/api/cart/apply-voucher")
    @ResponseBody
    public ResponseEntity<?> applyVoucher(
            @RequestBody ApplyVoucherRequest request, // Nhận code từ body JSON
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("message", "Vui lòng đăng nhập."));
        }
        String username = authentication.getName();
        String voucherCode = request.getCode(); // Lấy code từ DTO
        logger.info("User {} attempting to apply voucher code: {}", username, voucherCode);

        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp mã voucher."));
        }

        try {
            
            CartDto updatedCart = promotion.applyVoucher(username, voucherCode); // Giả sử service có hàm này

            logger.info("Voucher code {} applied successfully for user {}", voucherCode, username);
            return ResponseEntity.ok(updatedCart); // Trả về cart mới
        } catch (IllegalArgumentException e) {
            // Bắt lỗi cụ thể từ service (vd: mã không tồn tại, không đủ điều kiện)
            logger.warn("Failed to apply voucher code {} for user {}: {}", voucherCode, username, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Các lỗi khác không mong muốn
            logger.error("Error applying voucher code {} for user {}: {}", voucherCode, username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "Lỗi hệ thống khi áp dụng voucher."));
        }
    }
    
    @PostMapping("/api/cart/remove-voucher")
    @ResponseBody // Ensure JSON response
    public ResponseEntity<?> removeVoucher(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("message", "Vui lòng đăng nhập."));
        }
        String username = authentication.getName();
        logger.info("User {} attempting to remove voucher.", username);

        try {
            // Gọi PromotionService (hoặc CartService) để gỡ bỏ voucher
            // Service này cần trả về CartDto đã cập nhật (discountAmount=0, appliedVoucherCode=null)
            CartDto updatedCart = promotion.removeVoucher(username); // Assumes service has this method

            logger.info("Voucher removed successfully for user {}", username);
            return ResponseEntity.ok(updatedCart); // Trả về cart mới
        } catch (Exception e) {
            // Lỗi không mong muốn
            logger.error("Error removing voucher for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "Lỗi hệ thống khi gỡ bỏ voucher."));
        }
    }
    // =========================================================


    /** DTO cho request AJAX thêm sản phẩm */
    static class AddItemRequest {
        private int quantity;
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}