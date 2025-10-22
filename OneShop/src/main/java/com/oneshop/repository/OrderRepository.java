package com.oneshop.repository;

import java.util.List;
import java.util.Optional;

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
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByOrderStatus(String orderStatus);
    
    @Query("SELECT o FROM Order o " +
    	       "LEFT JOIN FETCH o.orderDetails od " + 
    	       "LEFT JOIN FETCH od.productVariant pv " + 
    	       "LEFT JOIN FETCH pv.product p " + 
    	       "WHERE o.id = :id AND o.user.username = :username")
    	Optional<Order> findByIdAndUser_Username(@Param("id") Long id, @Param("username") String username);
    
    List<Order> findByUserIdAndOrderStatus(Long userId, String orderStatus);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails i " +
    	       "LEFT JOIN FETCH i.productVariant v " +
    	       "LEFT JOIN FETCH v.product p " +
    	       "WHERE o.id = :orderId")
    	Order findByIdWithItems(@Param("orderId") Long orderId);
}