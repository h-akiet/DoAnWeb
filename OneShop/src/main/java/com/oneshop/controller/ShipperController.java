// src/main/java/com/oneshop/controller/ShipperController.java
package com.oneshop.controller;

import com.oneshop.entity.Order;               // Import Entity
import com.oneshop.entity.User;                // Import Entity
import com.oneshop.service.OrderService;         // Import Service

import org.slf4j.Logger;                     // Import Logger
import org.slf4j.LoggerFactory;            // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // Import PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Import RedirectAttributes

import java.util.Collections; // Import Collections
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/shipper") // Base path cho shipper
@PreAuthorize("hasAuthority('ROLE_SHIPPER')") // Đảm bảo chỉ Shipper mới truy cập được Controller này
public class ShipperController {

    private static final Logger logger = LoggerFactory.getLogger(ShipperController.class); // Thêm Logger

    @Autowired
    private OrderService orderService; // Đảm bảo OrderService có các hàm cần thiết

    /**
     * Hiển thị danh sách đơn hàng được gán cho shipper.
     */
    @GetMapping("/orders")
    public String listAssignedOrders(@AuthenticationPrincipal User shipper, Model model) {
        if (shipper == null || shipper.getUserId() == null) {
            logger.error("Could not get shipper information from AuthenticationPrincipal.");
            // Xử lý lỗi: Chuyển hướng hoặc báo lỗi
            return "redirect:/login?error=auth";
        }
        Long shipperId = shipper.getUserId(); // Sử dụng getUserId() đã thêm vào User
        logger.info("Fetching assigned orders for shipperId: {}", shipperId);

        try {
            // Lấy danh sách đơn hàng được gán (Cần hàm getAssignedOrders trong OrderService)
            List<Order> orders = orderService.getAssignedOrders(shipperId);
            // Lấy thống kê đơn hàng (Cần hàm getOrderStats trong OrderService)
            Map<String, Long> stats = orderService.getOrderStats(shipperId);

            model.addAttribute("orders", orders);
            model.addAttribute("stats", stats);

            return "shipper/orders"; // View templates/shipper/orders.html
        } catch (Exception e) {
             logger.error("Error fetching orders/stats for shipperId {}: {}", shipperId, e.getMessage(), e);
             model.addAttribute("errorMessage", "Không thể tải danh sách đơn hàng.");
             model.addAttribute("orders", Collections.emptyList());
             model.addAttribute("stats", Collections.emptyMap());
             return "shipper/orders";
        }
    }

    /**
     * Xử lý khi shipper xác nhận đã giao hàng thành công.
     */
    @PostMapping("/orders/{id}/deliver")
    public String deliverOrder(@PathVariable Long id,
                               @AuthenticationPrincipal User shipper,
                               RedirectAttributes redirectAttributes) {
         if (shipper == null || shipper.getUserId() == null) {
            return "redirect:/login?error=auth";
        }
        Long shipperId = shipper.getUserId();
        logger.info("Shipper {} attempting to mark order {} as delivered.", shipperId, id);

        try {
            // Gọi service để cập nhật trạng thái (Cần hàm deliverOrder trong OrderService)
            // Service sẽ kiểm tra xem shipper có quyền cập nhật đơn hàng này không
            orderService.deliverOrder(id, shipperId);
            logger.info("Order {} marked as delivered by shipper {}", id, shipperId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận giao hàng thành công cho đơn #" + id);
        } catch (SecurityException e) {
             logger.warn("Unauthorized attempt by shipper {} to deliver order {}: {}", shipperId, id, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật đơn hàng này.");
        } catch (IllegalStateException | IllegalArgumentException e) {
             logger.warn("Failed to deliver order {} by shipper {}: {}", id, shipperId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error delivering order {} by shipper {}: {}", id, shipperId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi xác nhận giao hàng.");
        }

        return "redirect:/shipper/orders"; // Chuyển hướng về danh sách đơn hàng
    }
}