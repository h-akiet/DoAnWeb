package com.oneshop.dto.vendor;

import lombok.Data;

@Data
public class ShopDto {
    private String shopName;
    private String shopDescription;
    private String contactEmail;
    private String contactPhone;
    // Các file logo và banner sẽ được xử lý riêng trong Controller/Service
}