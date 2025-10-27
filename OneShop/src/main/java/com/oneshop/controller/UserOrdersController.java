package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.oneshop.entity.Order;
import com.oneshop.entity.User;
import com.oneshop.service.OrderService;

import java.util.List;

@Controller
@RequestMapping("/user/orders")
public class UserOrdersController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String listOrders(@AuthenticationPrincipal User user, Model model) {
        String username = user.getUsername(); 
    	List<Order> userOrders = orderService.findOrdersByCurrentUser(username); 

        model.addAttribute("orders", userOrders);
        return "user/orders"; 
    }
    
    // Xử lý POST từ form ẩn để hủy đơn hàng
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        
        orderService.cancelOrder(id, user.getUsername());
        
        redirectAttributes.addFlashAttribute("successMessage", "Đơn hàng #" + id + " đã được hủy.");
        return "redirect:/user/orders";
    }
    
    // Xử lý GET chi tiết đơn hàng (Dùng cho AJAX Modal)
    @GetMapping("/{id}/details")
    public String getOrderDetails(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        
        // FIX: Lấy username và truyền vào service để kiểm tra quyền sở hữu
        String username = user.getUsername();
        Order order = orderService.findOrderByIdAndUser(id, username); 
        
        model.addAttribute("order", order);
        
        return "user/order-details-fragment :: content";
    }
}