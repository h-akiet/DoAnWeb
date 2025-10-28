package com.oneshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.oneshop.entity.Shop;
import com.oneshop.enums.ShopStatus;
@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
	@Query("SELECT s FROM Shop s JOIN FETCH s.user")
    List<Shop> findAllWithVendor();
	
	 List<Shop> findByStatus(ShopStatus status);
   // Tìm Shop dựa trên User (chủ sở hữu)
   Optional<Shop> findByUserId(Long userId);
}
