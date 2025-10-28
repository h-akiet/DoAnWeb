// src/main/java/com/oneshop/controller/ReviewController.java
package com.oneshop.controller;

import com.oneshop.entity.Order;               // Import Entity
import com.oneshop.entity.OrderDetail;          // Import Entity
// import com.oneshop.entity.OrderItem;        // Không dùng OrderItem nữa
import com.oneshop.entity.Product;             // Import Entity
import com.oneshop.entity.User;                // Import Entity
import com.oneshop.service.OrderService;         // Import Service
import com.oneshop.service.ReviewService;        // Import Service

import org.hibernate.Hibernate;              // Import Hibernate
import org.slf4j.Logger;                     // Import Logger
import org.slf4j.LoggerFactory;            // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;    // Import HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException; // Import ResponseStatusException
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Import RedirectAttributes
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class); // Thêm Logger

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderService orderService;

    /**
     * Hiển thị form đánh giá cho một đơn hàng đã giao.
     */
    @GetMapping("/user/orders/{orderId}/review")
    public String showReviewForm(@PathVariable Long orderId,
                                 @AuthenticationPrincipal User user, // Lấy user để kiểm tra quyền
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        logger.info("Showing review form for orderId: {} for user: {}", orderId, user.getUsername());
        try {
            // Lấy đơn hàng và kiểm tra quyền sở hữu + trạng thái
            Order order = orderService.findOrderByIdAndUser(orderId, user.getUsername()); // Service kiểm tra quyền

            // Kiểm tra trạng thái đơn hàng (phải là DELIVERED)
            if (order.getOrderStatus() != com.oneshop.entity.OrderStatus.DELIVERED) {
                 logger.warn("Attempt to review order {} which is not DELIVERED (status: {}) by user {}", orderId, order.getOrderStatus(), user.getUsername());
                 redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể đánh giá đơn hàng đã giao thành công.");
                 return "redirect:/user/orders"; // Quay lại danh sách đơn hàng
            }

            // Khởi tạo OrderDetails để tránh LazyInitializationException nếu cần
            Hibernate.initialize(order.getOrderDetails());
            List<OrderDetail> items = new ArrayList<>(order.getOrderDetails()); // Sao chép để an toàn

            // Chuẩn bị dữ liệu cho view: thông tin sản phẩm và trạng thái đã đánh giá chưa
            List<Map<String, Object>> productReviewsInfo = new ArrayList<>();
            for (OrderDetail item : items) {
                if (item.getProductVariant() != null && item.getProductVariant().getProduct() != null) {
                    Map<String, Object> reviewInfo = new HashMap<>();
                    Product product = item.getProductVariant().getProduct();
                    reviewInfo.put("product", product); // Product entity
                    // Kiểm tra xem sản phẩm này đã được đánh giá trong đơn hàng này chưa
                    boolean reviewed = reviewService.isProductReviewed(orderId, product.getProductId()); // Sửa: Gọi đúng getter getProductId()
                    reviewInfo.put("reviewed", reviewed);
                    productReviewsInfo.add(reviewInfo);
                } else {
                     logger.warn("Order detail item in order {} has null ProductVariant or Product.", orderId);
                }
            }

            model.addAttribute("order", order);
            model.addAttribute("productReviews", productReviewsInfo); // Đổi tên biến cho rõ ràng
            return "user/review-form"; // View templates/user/review-form.html

        } catch (ResponseStatusException e) {
             logger.warn("User {} attempted to access review form for order {} without permission or order not found.", user.getUsername(), orderId);
             redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập.");
             return "redirect:/user/orders";
        } catch (Exception e) {
            logger.error("Error showing review form for orderId {}: {}", orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải trang đánh giá.");
            return "redirect:/user/orders";
        }
    }

    /**
     * Xử lý việc gửi đánh giá cho một sản phẩm trong đơn hàng.
     */
    @PostMapping("/user/orders/{orderId}/review/{productId}")
    public String submitReview(@PathVariable Long orderId,
                              @PathVariable Long productId,
                              @RequestParam Integer rating, // Rating là bắt buộc
                              @RequestParam(required = false, defaultValue = "") String comment, // Comment có thể không bắt buộc
                              @AuthenticationPrincipal User user, // Lấy user để xác thực quyền
                              RedirectAttributes redirectAttributes) { // Dùng RedirectAttributes để gửi thông báo
        logger.info("Submitting review for orderId: {}, productId: {} by user: {}", orderId, productId, user.getUsername());

        // Validation cơ bản
        if (rating == null || rating < 1 || rating > 5) {
             redirectAttributes.addFlashAttribute("errorMessage_" + productId, "Vui lòng chọn số sao đánh giá (1-5).");
             return "redirect:/user/orders/" + orderId + "/review";
        }
        // Có thể thêm validation độ dài comment nếu cần

        try {
            // Gọi service để lưu đánh giá (service sẽ kiểm tra quyền, trạng thái đơn hàng, đã review chưa)
            reviewService.saveReview(orderId, productId, user, rating, comment);
            logger.info("Review submitted successfully for productId: {} in orderId: {}", productId, orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá sản phẩm thành công!");
            // Chuyển hướng về lại trang đánh giá của đơn hàng đó
            return "redirect:/user/orders/" + orderId + "/review";
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            // Bắt các lỗi nghiệp vụ cụ thể từ service
            logger.warn("Review submission failed for productId: {} in orderId: {}: {}", productId, orderId, e.getMessage());
            // Hiển thị lỗi cụ thể cho sản phẩm đó trên trang review
            redirectAttributes.addFlashAttribute("errorMessage_" + productId, e.getMessage());
            return "redirect:/user/orders/" + orderId + "/review";
        } catch (Exception e) {
            // Lỗi không mong muốn khác
            logger.error("Unexpected error submitting review for productId: {} in orderId: {}: {}", productId, orderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi gửi đánh giá.");
            return "redirect:/user/orders/" + orderId + "/review";
        }
    }
}