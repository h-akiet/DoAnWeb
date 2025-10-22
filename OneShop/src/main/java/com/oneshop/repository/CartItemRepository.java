package com.oneshop.repository;

import com.oneshop.entity.CartItem;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	Optional<CartItem> findByCart_CartIdAndVariant_VariantId(Long cartId, Long variantId);
}