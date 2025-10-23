package com.oneshop.dto.vendor;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDto {
    
    private Long id; // Dùng để phân biệt Thêm mới (null) hay Cập nhật (có giá trị)

    private String productName;
    private String productDescription;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stock;
    private String productTags;
}