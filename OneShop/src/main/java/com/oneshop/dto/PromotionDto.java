package com.oneshop.dto;
import lombok.Data;
import jakarta.validation.constraints.NotNull; 
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionDto {
    private String campaignName;
    private String discountCode;
    
    @NotNull(message = "Vui lòng chọn loại khuyến mãi") 
    private Long promotionTypeId; 

    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
}