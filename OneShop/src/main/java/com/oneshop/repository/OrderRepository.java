package com.oneshop.repository.vendor;

import java.util.List;

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
}