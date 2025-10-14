package com.oneshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // ✅ Sử dụng @Query thay vì query method naming
    @Query("SELECT o FROM Order o WHERE o.shipper.id = :shipperId")
    List<Order> findByShipperId(@Param("shipperId") Long shipperId);
    
    // ✅ Hoặc dùng naming convention với underscore (cách khác)
    // List<Order> findByShipper_Id(Long shipperId);
    
    // ✅ Tìm orders theo user
    List<Order> findByUserId(Long userId);
    
    // ✅ Tìm orders theo status
    List<Order> findByOrderStatus(String orderStatus);
    
    // ✅ Tìm orders theo user và status
    List<Order> findByUserIdAndOrderStatus(Long userId, String orderStatus);
}