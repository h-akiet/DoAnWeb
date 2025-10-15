package com.oneshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "PRODUCT_VARIANTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 100, nullable = false)
    private String name; // <-- BỔ SUNG: Tên biến thể, ví dụ: "Màu: Đỏ Ruby"

    @Column(length = 50, unique = true)
    private String sku;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price; // Giá bán của biến thể

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice; // <-- BỔ SUNG: Giá gốc của biến thể

    @Column(nullable = false)
    private int stock; // <-- BỔ SUNG: Tồn kho của riêng biến thể này

    @Column(name = "image_url")
    private String imageUrl; // <-- BỔ SUNG: Ảnh đại diện cho biến thể

    @Column(name = "is_active", nullable = false)
    private boolean active = true; 
}