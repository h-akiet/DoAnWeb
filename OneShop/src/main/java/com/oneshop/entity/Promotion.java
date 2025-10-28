package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String campaignName; 

    @Column(nullable = false, unique = true, columnDefinition = "nvarchar(100)")
    private String discountCode; // Mã giảm giá

    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "promotion_type_id", nullable = false)
    private PromotionTypeEntity type;

    // Giá trị giảm (có thể là % hoặc số tiền, null nếu là free shipping)
    @Column(columnDefinition = "numeric(19, 2)")
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // Một khuyến mãi thuộc về một Shop
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
}