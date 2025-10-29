package com.oneshop.entity;

import com.oneshop.enums.ShopStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Shops")
@Setter
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String name;

    @Column(length = 1000, columnDefinition = "nvarchar(1000)")
    private String description;

    private String logo;
    private String banner;
    private String contactEmail;
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ShopStatus status = ShopStatus.PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Promotion> promotions;
    
    @Column(name = "commission_rate", precision = 5, scale = 4, nullable = false, columnDefinition = "DECIMAL(5,4) DEFAULT 0.05")
    private BigDecimal commissionRate = new BigDecimal("0.05");

    @Column(name = "commission_updated_at")
    private LocalDateTime commissionUpdatedAt;
}