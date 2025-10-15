package com.oneshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "PROMOTIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long promotionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id") // NULL nếu là ưu đãi toàn hệ thống (Admin tạo)
    private Shop shop;

    @Column(name = "promo_code", unique = true, length = 50)
    private String promoCode;

    @Column(length = 30, nullable = false)
    private String type; // PRODUCT, ORDER, SHIPPING

    @Column(name = "discount_type", length = 20)
    private String discountType; // PERCENTAGE, FIXED

    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;
}