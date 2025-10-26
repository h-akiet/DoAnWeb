package com.oneshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.Shop;
@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
	@Query("SELECT s FROM Shop s JOIN FETCH s.vendor")
    List<Shop> findAllWithVendor();
}
