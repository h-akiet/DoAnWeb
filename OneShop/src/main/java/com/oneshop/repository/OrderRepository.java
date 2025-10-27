// src/main/java/com/oneshop/repository/OrderRepository.java
package com.oneshop.repository;

import com.oneshop.entity.Order;
import com.oneshop.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // --- Cho Vendor ---
    Page<Order> findByShopId(Long shopId, Pageable pageable);
    Page<Order> findByShopIdAndOrderStatus(Long shopId, OrderStatus orderStatus, Pageable pageable);
    long countByShopIdAndOrderStatus(Long shopId, OrderStatus orderStatus);
    List<Order> findByShopIdAndOrderStatusIn(Long shopId, List<OrderStatus> statuses, Sort sort);

    // ===>>> PHƯƠNG THỨC TÍNH DOANH THU <<<===

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.shop.id = :shopId AND o.orderStatus = :status")
    BigDecimal calculateTotalRevenueByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") OrderStatus status);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.shop.id = :shopId AND o.orderStatus = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRevenueByShopIdAndStatusBetweenDates(
            @Param("shopId") Long shopId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy doanh thu theo từng tháng trong một khoảng thời gian (Ví dụ: 6 tháng gần nhất).
     * SỬA: Dùng FORMAT thay vì TO_CHAR cho SQL Server.
     */
    // ===>>> SỬA CÂU QUERY NÀY <<<===
    @Query(value = "SELECT FORMAT(o.created_at, 'yyyy-MM') as monthYear, SUM(o.total_amount) as monthlyRevenue " +
                   "FROM ORDERS o " +
                   "WHERE o.shop_id = :shopId AND o.order_status = :#{#status.name()} AND o.created_at >= :startDate " +
                   "GROUP BY FORMAT(o.created_at, 'yyyy-MM') " + // Sửa GROUP BY
                   "ORDER BY monthYear ASC", nativeQuery = true)
    // ===>>> KẾT THÚC SỬA <<<===
    List<Object[]> findMonthlyRevenueByShopIdAndStatusSinceDate(
            @Param("shopId") Long shopId,
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate);

    // ===>>> KẾT THÚC PHẦN DOANH THU <<<===


    // --- Cho User ---
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.productVariant pv LEFT JOIN FETCH pv.product p WHERE o.id = :id AND o.user.username = :username")
    Optional<Order> findByIdAndUser_Username(@Param("id") Long id, @Param("username") String username);

    // --- Cho Shipper ---
    List<Order> findByShipper_IdAndOrderStatusInOrderByCreatedAtAsc(Long shipperId, List<OrderStatus> statuses);
    List<Order> findByShipper_Id(Long shipperId);

    // --- Khác ---
    List<Order> findByUserId(Long userId);
    List<Order> findByOrderStatus(OrderStatus orderStatus);
    List<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus orderStatus);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails i " +
           "LEFT JOIN FETCH i.productVariant v " +
           "LEFT JOIN FETCH v.product p " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
}