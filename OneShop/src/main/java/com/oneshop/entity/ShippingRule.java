package com.oneshop.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SHIPPING_RULES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ruleId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipping_company_id", nullable = false)
	private ShippingCompany company; // Liên kết 1-N với Nhà Vận Chuyển

	@Column(length = 50)
	private String ruleName; // Tên quy tắc (ví dụ: "Phí Nội Thành TPHCM", "Phí Hỏa Tốc Hà Nội")
	
	

	// --- CÁC TRƯỜNG ĐIỀU KIỆN ÁP DỤNG ---

	// 1. Điều kiện theo Khu vực/Vùng:
	@Column(length = 50)
	private String fromRegion; // Khu vực/tỉnh/thành phố gửi

	@Column(length = 50)
	private String toRegion; // Khu vực/tỉnh/thành phố nhận
	
	// 3. Điều kiện Vận chuyển Hỏa tốc (Loại dịch vụ):
	@Column(nullable = false)
	private Boolean isExpress = false; // true nếu đây là dịch vụ giao hàng Hỏa tốc
	
	@Column(name = "estimated_delivery_time", length = 50)
	private String estimatedDeliveryTime;
	// --- MỨC PHÍ TÍNH TOÁN ---

	@Column(precision = 10, scale = 2, nullable = false)
	private BigDecimal baseFee; // Phí cố định (Flat Rate) áp dụng khi thỏa mãn các điều kiện trên
}