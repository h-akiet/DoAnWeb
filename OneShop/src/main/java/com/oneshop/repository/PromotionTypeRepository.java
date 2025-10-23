package com.oneshop.repository.vendor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.vendor.PromotionTypeEntity;


@Repository
public interface PromotionTypeRepository extends JpaRepository<PromotionTypeEntity, Long> {
}