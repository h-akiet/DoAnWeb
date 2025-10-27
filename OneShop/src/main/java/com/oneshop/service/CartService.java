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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger; // Thêm Logger
import org.slf4j.LoggerFactory; // Thêm LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Thêm StringUtils

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class); // Thêm Logger

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
        // Sửa: Dùng findOptionalVariantById để xử lý tốt hơn
        ProductVariant variant = productVariantService.findOptionalVariantById(variantId)
                .orElseThrow(() -> {
                     logger.warn("Attempt to add non-existent variantId {} to cart for user {}", variantId, username);
                     return new RuntimeException("Không tìm thấy biến thể sản phẩm: " + variantId);
                });

        if (quantity <= 0) quantity = 1;

        // Kiểm tra tồn kho trước khi thêm/cập nhật
        int currentStock = variant.getStock();
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId);
        int quantityInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int requestedTotalQuantity = quantityInCart + quantity;

        if (currentStock < requestedTotalQuantity) {
            logger.warn("Not enough stock for variantId {} (requested: {}, in cart: {}, available: {}) for user {}",
                        variantId, quantity, quantityInCart, currentStock, username);
            throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + " - " + variant.getName() + "' không đủ hàng (còn " + currentStock + ").");
        }


        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(requestedTotalQuantity); // Cập nhật số lượng mới
            cartItemRepository.save(existingItem);
            logger.debug("Updated quantity for variantId {} in cart {} to {}", variantId, cart.getCartId(), requestedTotalQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(quantity); // Số lượng ban đầu
            // === THÊM ITEM VÀO COLLECTION CỦA CART ===
            // Điều này cần thiết nếu bạn muốn Cascade hoạt động đúng
            // Hoặc để đảm bảo trạng thái entity nhất quán
            cart.getItems().add(newItem);
            // ======================================
            cartItemRepository.save(newItem);
            logger.debug("Added new variantId {} (qty: {}) to cart {}", variantId, quantity, cart.getCartId());
        }

        // Tải lại giỏ hàng với đầy đủ item để map sang DTO
        // Cân nhắc: Có thể không cần tải lại nếu `cart` đã được fetch đúng cách
        Cart updatedCart = cartRepository.findByIdWithItems(cart.getCartId()).orElse(cart);
        return mapToCartDto(updatedCart);
    }

    @Transactional
    public CartDto updateItemQuantity(String username, Long variantId, int newQuantity) {
        User user = userService.findByUsername(username);
        Cart cart = findOrCreateCartByUser(user);

        CartItem cartItem = cartItemRepository.findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId)
                .orElseThrow(() -> {
                     logger.warn("Attempt to update non-existent variantId {} in cart for user {}", variantId, username);
                     return new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng");
                });

        if (newQuantity <= 0) {
            // === THAY ĐỔI: Xóa khỏi collection trước khi xóa khỏi repo ===
            boolean removed = cart.getItems().remove(cartItem); // Xóa khỏi Set trong Cart
            if (removed) {
                logger.debug("Removed cartItem object from Cart's items collection.");
            } else {
                 logger.warn("CartItem object was not found in Cart's items collection before deletion.");
            }
            cartItemRepository.delete(cartItem); // Xóa khỏi DB
            logger.debug("Removed variantId {} from cart {} due to quantity <= 0", variantId, cart.getCartId());
            // ==========================================================
        } else {
            // Kiểm tra tồn kho trước khi cập nhật
             ProductVariant variant = cartItem.getVariant();
             if (variant != null && variant.getStock() < newQuantity) {
                 logger.warn("Not enough stock for variantId {} (requested: {}, available: {}) on update for user {}",
                             variantId, newQuantity, variant.getStock(), username);
                 throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + " - " + variant.getName() + "' không đủ hàng (còn " + variant.getStock() + ").");
             }

            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
             logger.debug("Updated quantity for variantId {} in cart {} to {}", variantId, cart.getCartId(), newQuantity);
        }

        // Cân nhắc: Có thể không cần tải lại nếu `cart` đã được fetch đúng cách
        Cart updatedCart = cartRepository.findByIdWithItems(cart.getCartId()).orElse(cart);
        return mapToCartDto(updatedCart);
    }

    // === SỬA ĐỔI: Xóa item khỏi collection của Cart trước ===
    @Transactional
    public void removeItemFromCart(String username, Long variantId) {
        User user = userService.findByUsername(username);
        // Sử dụng query fetch để đảm bảo cart.getItems() được tải
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                 .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng cho user: " + username));

        logger.debug("Attempting to remove variantId {} from cartId {} for user {}", variantId, cart.getCartId(), username);

        // Tìm CartItem trong collection đã được tải của Cart
        CartItem cartItemToRemove = cart.getItems().stream()
            .filter(item -> item.getVariant() != null && item.getVariant().getVariantId().equals(variantId))
            .findFirst()
            .orElseThrow(() -> {
                 logger.warn("Attempt to remove non-existent variantId {} from cart {} (items loaded) for user {}", variantId, cart.getCartId(), username);
                 return new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng");
            });

        logger.debug("Found CartItem id {} in Cart's items collection to remove.", cartItemToRemove.getCartItemId());

        try {
            // 1. Xóa khỏi collection của Cart trước
            boolean removed = cart.getItems().remove(cartItemToRemove);
            if (removed) {
                logger.debug("Successfully removed cartItem object from Cart's items collection.");
                // 2. Sau đó mới xóa khỏi repository
                cartItemRepository.delete(cartItemToRemove);
                // cartItemRepository.flush(); // Tùy chọn: Ép flush để xem log delete ngay
                logger.info("Successfully executed delete for CartItem id {}, variantId {} from cart {}", cartItemToRemove.getCartItemId(), variantId, cart.getCartId());
            } else {
                // Trường hợp này không nên xảy ra nếu logic tìm kiếm ở trên đúng
                 logger.error("Failed to remove cartItem object from Cart's items collection! CartItemId: {}", cartItemToRemove.getCartItemId());
                 // Vẫn thử xóa khỏi DB
                 cartItemRepository.delete(cartItemToRemove);
                 logger.warn("Attempted deletion from repository despite failing to remove from collection for CartItemId: {}", cartItemToRemove.getCartItemId());
                 // Cân nhắc ném lỗi ở đây nếu việc không xóa được khỏi collection là nghiêm trọng
                 // throw new RuntimeException("Lỗi không đồng bộ khi xóa item khỏi collection.");
            }
        } catch (Exception e) {
            logger.error("Error during cartItem removal process for cartItemId {}: {}", cartItemToRemove.getCartItemId(), e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xóa sản phẩm khỏi giỏ hàng.", e);
        }
    }
    // =========================================================

    @Transactional
    public Cart findOrCreateCartByUser(User user) {
        return cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> {
                    logger.info("No cart found for user {}, creating a new one.", user.getUsername());
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    Cart savedCart = cartRepository.save(newCart);
                    user.setCart(savedCart);
                    // userService.save(user); // Cập nhật user nếu cần
                    return savedCart;
                });
    }

    private CartDto mapToCartDto(Cart cart) {
        CartDto cartDto = new CartDto();
        Map<Long, CartItemDto> itemsMap = new HashMap<>();

        // Kiểm tra cart và cart.getItems() có null không
        if (cart != null && cart.getItems() != null) {
            logger.debug("Mapping cartId {} with {} items to DTO.", cart.getCartId(), cart.getItems().size());
            for (CartItem itemEntity : cart.getItems()) {
                ProductVariant variant = itemEntity.getVariant();
                if (variant == null || variant.getProduct() == null) {
                     logger.warn("Skipping cart item id {} because its variant or product is null.", itemEntity.getCartItemId());
                     continue;
                }

                CartItemDto itemDto = new CartItemDto();
                itemDto.setProductId(variant.getVariantId());

                String productName = variant.getProduct().getName();
                String variantName = variant.getName();
                itemDto.setName(StringUtils.hasText(variantName) ? (productName + " - " + variantName) : productName);

                itemDto.setPrice(variant.getPrice());
                itemDto.setQuantity(itemEntity.getQuantity());

                String imageUrl = variant.getImageUrl();
                if (!StringUtils.hasText(imageUrl)) {
                     imageUrl = variant.getProduct().getPrimaryImageUrl();
                     logger.trace("Variant {} has no image, using product's primary image: {}", variant.getVariantId(), imageUrl);
                } else {
                     if (!imageUrl.startsWith("/")) {
                         imageUrl = "/uploads/images/" + imageUrl;
                     }
                      logger.trace("Using variant image for {}: {}", variant.getVariantId(), imageUrl);
                }
                itemDto.setImageUrl(imageUrl);

                itemsMap.put(itemDto.getProductId(), itemDto);
            }
        } else {
             logger.warn("Cart or Cart.items is null during mapping."); // Log cảnh báo nếu cart hoặc items là null
             if(cart != null) {
                 logger.debug("Mapping cartId {} with 0 items (items collection was null).", cart.getCartId());
             } else {
                  logger.debug("Mapping null cart to DTO.");
             }
        }

        cartDto.setItems(itemsMap);
        cartDto.calculateTotals();
        logger.debug("Mapped CartDto: totalItems={}, grandTotal={}", cartDto.getTotalItems(), cartDto.getGrandTotal());
        return cartDto;
    }


    @Transactional
    public void clearCartItems(Long userId, List<Long> variantIds) {
         if (userId == null || variantIds == null || variantIds.isEmpty()) {
             logger.warn("Attempted to clear cart items with invalid parameters. userId: {}, variantIds: {}", userId, variantIds);
             return;
         }
         logger.debug("Clearing cart items for userId {} with variantIds: {}", userId, variantIds);
         cartItemRepository.deleteByUserIdAndProductVariantIdIn(userId, variantIds);
    }
}