package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode; // <<< THÊM: Import cần thiết cho BigDecimal
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "PRODUCTS")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "display_price", nullable = false, precision = 19, scale = 2) // <<< CẢI THIỆN: Thêm precision và scale
    private BigDecimal price;

    @Column(name = "original_price", precision = 19, scale = 2) // <<< CẢI THIỆN: Thêm precision và scale
    private BigDecimal originalPrice;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(nullable = false)
    private int salesCount = 0; // <<< SỬA: Giữ lại trường này là nguồn dữ liệu chính

    @Column(name = "is_published", nullable = false)
    private boolean published = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ProductVariant> variants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ProductImage> images;

    public String getPrimaryImageUrl() {
        if (images == null || images.isEmpty()) {
            return "/assets/img/product/product1.png";
        }
        return images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(images.iterator().next().getImageUrl());
    }

    // <<< SỬA: Viết lại hoàn toàn để tính toán đúng với BigDecimal
    public int getDiscountPercentage() {
        if (originalPrice == null || price == null ||
            originalPrice.compareTo(BigDecimal.ZERO) <= 0 ||
            price.compareTo(originalPrice) >= 0) {
            return 0; // Không có giảm giá
        }
        
        BigDecimal discount = originalPrice.subtract(price);
        BigDecimal percentage = discount.multiply(new BigDecimal("100"))
                                      .divide(originalPrice, 0, RoundingMode.HALF_UP);
        
        return percentage.intValue();
    }

    public String getFormattedDisplayPrice() {
        return formatCurrency(this.price);
    }

    public String getFormattedOriginalPrice() {
        return formatCurrency(this.originalPrice);
    }

    // <<< SỬA: Thay đổi tham số từ Double thành BigDecimal
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ"; // Trả về có đơn vị cho nhất quán
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormatter.format(amount).replace("₫", "đ");
    }

    @Transient
    private Double rating = 0.0;

    @Transient
    private Integer reviewCount = 0;

     @Transient
     private Integer soldCount = 0; 
}