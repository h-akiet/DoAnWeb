package com.oneshop.service;

// Import tất cả các Repository, DTO, Entity cần thiết
import com.oneshop.dto.CartDto;
import com.oneshop.dto.CartItemDto;
import com.oneshop.entity.Cart;
import com.oneshop.entity.CartItem;
import com.oneshop.entity.ProductVariant;
import com.oneshop.entity.User;
import com.oneshop.repository.CartItemRepository;
import com.oneshop.repository.CartRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; 
import org.springframework.transaction.annotation.Transactional;

@Service 
public class CartService { 

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private UserService userService;
    @Autowired private ProductVariantService productVariantService;

    
    @Transactional(readOnly = true)
    public CartDto getCartForUser(String username) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);
        return mapToCartDto(cart);
    }

    @Transactional
    public CartDto addItemToCart(String username, Long variantId, int quantity) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);
        ProductVariant variant = productVariantService.findVariantById(variantId);

        if (variant == null) {
            throw new RuntimeException("Không tìm thấy biến thể sản phẩm: " + variantId);
        }
        if (quantity <= 0) quantity = 1;

        // Dùng query chính xác: findByCart_CartIdAndVariant_Id
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId);

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }
        
        // Tải lại giỏ hàng với đầy đủ item để map sang DTO
        Cart updatedCart = cartRepository.findByIdWithItems(cart.getCartId()).orElse(cart);
        return mapToCartDto(updatedCart);
    }

    @Transactional
    public CartDto updateItemQuantity(String username, Long variantId, int newQuantity) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);

        CartItem cartItem = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (newQuantity <= 0) {
            // Nếu số lượng là 0, xóa item
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
        }
        
        Cart updatedCart = cartRepository.findByIdWithItems(cart.getCartId()).orElse(cart);
        return mapToCartDto(updatedCart);
    }

    // [THÊM PHƯƠNG THỨC XÓA] - CartController sẽ cần
    @Transactional
    public CartDto removeItemFromCart(String username, Long variantId) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);

        CartItem cartItem = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        
        Cart updatedCart = cartRepository.findByIdWithItems(cart.getCartId()).orElse(cart);
        return mapToCartDto(updatedCart);
    }

    // [SỬA LỖI 2] - Bổ sung logic
    @Transactional
    public Cart findOrCreateCartByUser(User user) {
        // Dùng query đã được fetch đầy đủ
        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    // Nếu user chưa có cart, tạo mới
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    // Lưu cart mới (bên sở hữu)
                    return cartRepository.save(newCart);
                });
    }

    // [SỬA LỖI 3] - Bổ sung logic
    private CartDto mapToCartDto(Cart cart) {
        CartDto cartDto = new CartDto();
        Map<Long, CartItemDto> itemsMap = new HashMap<>();

        if (cart.getItems() != null) {
            for (CartItem itemEntity : cart.getItems()) {
                ProductVariant variant = itemEntity.getVariant();
                if (variant == null) continue; // Bỏ qua nếu variant bị xóa

                // Ánh xạ sang CartItemDto (phiên bản của bạn)
                CartItemDto itemDto = new CartItemDto();
                
                // 1. Dùng variant.getId()
                itemDto.setProductId(variant.getVariantId()); 
                
                // 2. Ghép tên
                String productName = variant.getProduct().getName();
                String variantName = variant.getName();
                itemDto.setName(productName + " - " + variantName);

                // 3. Lấy giá (BigDecimal)
                itemDto.setPrice(variant.getPrice());
                itemDto.setQuantity(itemEntity.getQuantity());
                itemDto.setImageUrl(variant.getImageUrl());
                
                // 4. Thêm vào map
                itemsMap.put(itemDto.getProductId(), itemDto);
            }
        }

        cartDto.setItems(itemsMap);
        cartDto.calculateTotals(); // Tính tổng tiền
        return cartDto; // Trả về DTO đã hoàn thiện
    }
}