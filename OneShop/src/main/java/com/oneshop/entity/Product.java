// src/main/java/com/oneshop/entity/Product.java
package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.*; // Import Getter, Setter riêng
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashSet; // Import HashSet
import java.util.Locale;
import java.util.Set;
import com.oneshop.enums.ProductStatus;

@Entity // Chỉ một @Entity
@Table(name = "PRODUCTS") // Đổi tên bảng thành PRODUCTS cho nhất quán
@Getter // Thêm @Getter
@Setter // Thêm @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "brand", "category", "variants", "images", "shop" }) // Exclude các quan hệ
@ToString(exclude = { "brand", "category", "variants", "images", "shop" }) // Exclude các quan hệ
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long productId;

	@Column(nullable = false, columnDefinition = "nvarchar(255)")
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "brand_id")
	private Brand brand; // Import com.oneshop.entity.Brand

	// Đổi tên cột price thành price (bỏ display_)
	@Column(name = "price", nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(name = "original_price", precision = 19, scale = 2)
	private BigDecimal originalPrice;

	@Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
	private String description;

	@Column(name = "sales_count", nullable = false)
	private int salesCount = 0;

	@Column(name = "is_published", nullable = false)
	private boolean published = true; // Mặc định là true khi tạo?

	@Column(nullable = false)
	private Integer stock; // Tổng tồn kho (nếu sản phẩm không có variant)

	@Column(columnDefinition = "nvarchar(500)")
	private String tags;
//ADMIN
	@Enumerated(EnumType.STRING) // Trạng thái duyệt đơn của admin
	@Column(name = "status", nullable = false)
	private ProductStatus status = ProductStatus.PENDING; // Mặc định là 'Chờ duyệt'

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	// Quan hệ với Shop (Product thuộc về Shop nào)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shop_id", nullable = false)
	private Shop shop; // Import com.oneshop.entity.Shop

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<ProductVariant> variants = new HashSet<>(); // Import com.oneshop.entity.ProductVariant

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<ProductImage> images = new HashSet<>(); // Import com.oneshop.entity.ProductImage

	// --- Các trường Transient và Methods ---

	@Transient
	private Double rating = 0.0;

	@Transient
	private Integer reviewCount = 0;

	@Transient
	private Integer soldCount = 0; // Có thể lấy từ salesCount

	public String getPrimaryImageUrl() {
		if (images == null || images.isEmpty()) {
			return "/assets/img/product/no-image.jpg"; // Default path
		}

		ProductImage image = images.stream().filter(img -> Boolean.TRUE.equals(img.getIsPrimary())).findFirst()
				.orElse(images.iterator().next());

		return "/uploads/images/" + image.getImageUrl();
	}

	public int getDiscountPercentage() {
		if (originalPrice == null || price == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0
				|| price.compareTo(originalPrice) >= 0) {
			return 0;
		}
		BigDecimal discount = originalPrice.subtract(price);
		BigDecimal percentage = discount.multiply(new BigDecimal("100")).divide(originalPrice, 0, RoundingMode.HALF_UP);
		return percentage.intValue();
	}

	public String getFormattedDisplayPrice() {
		return formatCurrency(this.price);
	}

	public String getFormattedOriginalPrice() {
		return formatCurrency(this.originalPrice);
	}

	private String formatCurrency(BigDecimal amount) {
		if (amount == null)
			return "0 đ";
		NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
		return currencyFormatter.format(amount).replace("₫", "đ");
	}
}