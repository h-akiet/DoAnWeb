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

//Linh------
  @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long shopId;

	@Column(name = "shop_name", nullable = false, columnDefinition = "nvarchar(150)")
	private String shopName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor_id", nullable = false)
	private User vendor;

	@Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
	@ColumnDefault("0.0000") //
	private BigDecimal commissionRate = BigDecimal.ZERO;

	@Column(name = "commission_updated_at", nullable = false)
	@ColumnDefault("GETDATE()")
	private LocalDateTime commissionUpdatedAt = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, columnDefinition = "nvarchar(50)")
	@ColumnDefault("'PENDING'") // Đặt giá trị mặc định ở đây (nên dùng string literal)
	private ShopStatus status = ShopStatus.PENDING;
  //-------------------------------------------nd code cũ
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

    // --- THÊM TRẠNG THÁI SHOP ---
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ShopStatus status = ShopStatus.PENDING; // Mặc định là PENDING
    // ---------------------------

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

    // --- THÊM ENUM TRẠNG THÁI ---
    public enum ShopStatus {
        PENDING,    // Đang chờ duyệt
        APPROVED,   // Đã duyệt
        REJECTED,   // Bị từ chối
        INACTIVE    // Tạm ngưng (nếu cần)
    }
    // -------------------------- main
}