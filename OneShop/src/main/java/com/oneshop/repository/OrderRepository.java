package com.oneshop.repository.vendor;

import java.util.List;
import java.util.Optional;

// === SỬA IMPORT NÀY ===
import org.springframework.data.domain.Sort; 
// =====================
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.oneshop.entity.vendor.Order;
import com.oneshop.entity.vendor.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Tìm tất cả đơn hàng của một Shop (có phân trang)
    Page<Order> findByShopId(Long shopId, Pageable pageable);

    // Tìm tất cả đơn hàng của một Shop theo trạng thái (có phân trang)
    Page<Order> findByShopIdAndStatus(Long shopId, OrderStatus status, Pageable pageable);

    // Đếm số đơn hàng mới cho dashboard
    long countByShopIdAndStatus(Long shopId, OrderStatus status);

    // Tìm đơn hàng theo shop và một danh sách các trạng thái, có sắp xếp
    List<Order> findByShopIdAndStatusIn(Long shopId, List<OrderStatus> statuses, Sort sort);
    // ====================================================================
    
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