package com.oneshop.controller;

import com.oneshop.dto.CartDto;
import com.oneshop.dto.UpdateCartRequest; // DTO cho AJAX
import com.oneshop.service.CartService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Quan trọng: Dùng để lấy user
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService; // Tiêm Service đã hoàn thiện

    /**
     * Lấy trang giỏ hàng (GET)
     * Lấy user từ Authentication, sau đó lấy giỏ hàng từ service.
     */
    @GetMapping("")
    public String viewCart(Authentication authentication, Model model) {
        String username = authentication.getName();
        CartDto cart = cartService.getCartForUser(username);
        model.addAttribute("cart", cart);
        return "user/cart"; // (Tên file view cart.html của bạn)
    }

    /**
     * Thêm sản phẩm vào giỏ (Thường gọi từ trang chi tiết sản phẩm)
     * Dùng {variantId} thay vì {productId}
     */
    @GetMapping("/add/{variantId}")
    public String addToCart(@PathVariable("variantId") Long variantId,
                            @RequestParam(name = "quantity", defaultValue = "1") int quantity,
                            Authentication authentication) {
        
        String username = authentication.getName();
        cartService.addItemToCart(username, variantId, quantity);
        
        // Chuyển hướng về giỏ hàng
        return "redirect:/cart"; 
    }
    
    /**
     * Xóa sản phẩm khỏi giỏ (Link "Xóa" trong giỏ hàng)
     * Dùng {variantId}
     */
    @GetMapping("/remove/{variantId}")
    public String removeFromCart(@PathVariable("variantId") Long variantId, Authentication authentication) {
        String username = authentication.getName();
        cartService.removeItemFromCart(username, variantId); // Gọi service
        return "redirect:/cart";
    }
    
    /**
     * Cập nhật số lượng (POST bằng AJAX)
     * Lấy dữ liệu từ @RequestBody và user từ Authentication.
     */
    @PostMapping("/update")
    @ResponseBody // Rất quan trọng: Trả về JSON, không phải tên view
    public ResponseEntity<?> updateCartItem(
            @RequestBody UpdateCartRequest request, // DTO chứa variantId và quantity
            Authentication authentication
    ) {
        String username = authentication.getName();
        
        CartDto updatedCart = cartService.updateItemQuantity(
            username, 
            request.getVariantId(), // Lấy variantId từ request
            request.getQuantity()
        );
        
        return ResponseEntity.ok(updatedCart); // Trả về CartDto (dạng JSON)
    }
}