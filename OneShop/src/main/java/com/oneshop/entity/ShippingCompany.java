package com.oneshop.entity;

import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "SHIPPING_COMPANIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCompany {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long shippingId;

	@Column(length = 150, nullable = false)
	private String name; // Tên nhà vận chuyển (ví dụ: Giao Hàng Tiết Kiệm, VNPost)

	@Column(length = 20)
	private String phone; // Số điện thoại/Hotline liên hệ

	@Column(nullable = false)
	private Boolean isActive = true; // Trạng thái hoạt động
	
	@OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShippingRule> rules = new ArrayList<>();
}