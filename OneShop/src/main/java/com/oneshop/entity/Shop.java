package com.oneshop.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import com.oneshop.enums.ShopStatus;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "SHOPS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shop {

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
}