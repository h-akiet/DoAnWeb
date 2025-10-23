package com.oneshop.specification;

import com.oneshop.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class ProductSpecification {

    /**
     * Tạo một Specification để lọc sản phẩm theo danh sách ID danh mục.
     * @param categoryIds Danh sách ID danh mục để lọc.
     * @return Specification cho việc lọc.
     */
    public static Specification<Product> hasCategory(List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return criteriaBuilder.conjunction(); // Trả về điều kiện "luôn đúng" nếu không có ID nào được chọn
            }
            // Sử dụng "categoryId" để khớp với tên trường trong Entity Category của bạn
            return root.get("category").get("categoryId").in(categoryIds);
        };
    }

    /**
     * Tạo một Specification để lọc sản phẩm theo danh sách ID thương hiệu.
     * @param brandIds Danh sách ID thương hiệu để lọc.
     * @return Specification cho việc lọc.
     */
    public static Specification<Product> hasBrand(List<Long> brandIds) {
        return (root, query, criteriaBuilder) -> {
            if (brandIds == null || brandIds.isEmpty()) {
                return criteriaBuilder.conjunction(); // Trả về điều kiện "luôn đúng" nếu không có ID nào được chọn
            }
            // Sử dụng "brandId" để khớp với tên trường trong Entity Brand của bạn
            return root.get("brand").get("brandId").in(brandIds);
        };
    }

    /**
     * Tạo một Specification để lọc sản phẩm theo khoảng giá.
     * @param minPrice Giá tối thiểu.
     * @param maxPrice Giá tối đa.
     * @return Specification cho việc lọc.
     */
    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice != null && maxPrice != null) {
                return criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
            }
            if (maxPrice != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
            }
            return criteriaBuilder.conjunction(); // Không lọc theo giá nếu không có giá trị nào được cung cấp
        };
    }
}