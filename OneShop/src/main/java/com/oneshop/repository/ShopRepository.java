package com.oneshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.Shop;
@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

}
