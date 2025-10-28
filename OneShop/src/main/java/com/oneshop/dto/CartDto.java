package com.oneshop.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // Import
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class CartDto {

    // Key là productId (thực chất là variantId từ CartItemDto)
    private Map<Long, CartItemDto> items = new HashMap<>();
    
    // Tổng tiền (đã đổi sang BigDecimal)
    private BigDecimal grandTotal;

    /**
     * Tính toán lại tổng tiền toàn giỏ hàng.
     * Được gọi bởi CartServiceImpl sau khi mapToCartDto.
     */
    public void calculateTotals() {
        this.grandTotal = items.values().stream()
                .map(CartItemDto::getSubtotal) // Lấy BigDecimal subtotal từ mỗi item
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Cộng tất cả lại, bắt đầu từ 0
    }
    public int getTotalItems() {
        // Lấy tất cả CartItemDto, lấy số lượng (quantity) của từng cái,
        // và cộng tất cả lại với nhau.
        return items.values().stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();
    }
}