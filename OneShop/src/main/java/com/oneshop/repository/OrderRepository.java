package com.oneshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT o FROM Order o WHERE o.shipper.id = :shipperId")
    List<Order> findByShipperId(@Param("shipperId") Long shipperId);

    List<Order> findByUserId(Long userId);

    List<Order> findByOrderStatus(String orderStatus);

    List<Order> findByUserIdAndOrderStatus(Long userId, String orderStatus);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product p " +
           "LEFT JOIN FETCH i.variant v " +
           "WHERE o.id = :orderId")
    Order findByIdWithItems(@Param("orderId") Long orderId);
}