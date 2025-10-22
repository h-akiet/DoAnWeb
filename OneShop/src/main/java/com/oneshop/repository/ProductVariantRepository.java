package com.oneshop.repository;

import com.oneshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
	ProductVariant findByVariantId(Long variantId);
}