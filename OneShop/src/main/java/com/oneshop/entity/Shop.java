package com.oneshop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import com.oneshop.enums.ShopStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "name", nullable = false, columnDefinition = "nvarchar(150)")
	private String name;

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "vendor_id", nullable = false)
//	private User vendor;

	@Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
	@ColumnDefault("0.0000") // Chiet khau App
	private BigDecimal commissionRate = BigDecimal.ZERO;

	@Column(name = "commission_updated_at", nullable = false)
	@ColumnDefault("GETDATE()")
	private LocalDateTime commissionUpdatedAt = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, columnDefinition = "nvarchar(20)")
	@ColumnDefault("'PENDING'") 
	private ShopStatus status = ShopStatus.PENDING;

    @Column(length = 1000, columnDefinition = "nvarchar(1000)")
    private String description;

    private String logo;
    private String banner;
    private String contactEmail;
    private String contactPhone;

    // Một Shop chỉ thuộc về một User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Một Shop có nhiều Sản phẩm
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    // Một Shop có nhiều Đơn hàng
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    // Một Shop có nhiều Khuyến mãi
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Promotion> promotions;

}