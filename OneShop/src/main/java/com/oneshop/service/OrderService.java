package com.oneshop.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oneshop.dto.PlaceOrderRequest; // <-- Bổ sung import DTO
import com.oneshop.entity.Address;
import com.oneshop.entity.Order;
//import com.oneshop.entity.OrderDetail;
import com.oneshop.entity.ProductVariant;
import com.oneshop.entity.User;
//import com.oneshop.entity.OrderStatus; // <-- Bổ sung import (Giả sử bạn có Enum này)
import com.oneshop.repository.AddressRepository; // <-- Bổ sung import
//import com.oneshop.repository.OrderDetailRepository; // <-- Bổ sung import
import com.oneshop.repository.OrderRepository;
import com.oneshop.repository.ProductVariantRepository; // <-- Bổ sung import
import com.oneshop.repository.UserRepository; // <-- Bổ sung import

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    
    // ================= BỔ SUNG CÁC REPOSITORIES CẦN THIẾT =================
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private ProductVariantRepository variantRepository;
    
 //   @Autowired
//    private OrderDetailRepository orderDetailRepository;
    
    // (Giả sử bạn có CartService để xóa giỏ hàng sau khi đặt)
    // @Autowired
    // private CartService cartService;
    // =====================================================================


    @Autowired
    private EmailService emailService;

    public List<Order> getAssignedOrders(Long shipperId) {
        return orderRepository.findByShipperId(shipperId);
    }

   
    /**
     * Hàm cũ: Thống kê trạng thái đơn hàng
     */
    public Map<String, Long> getOrderStats(Long shipperId) {
        List<Order> orders = getAssignedOrders(shipperId);
        
        return orders.stream()
            .collect(Collectors.groupingBy(
                order -> {
                    if (order.getOrderStatus() == null) {
                        return "UNKNOWN";
                    }
                    return order.getOrderStatus().toString(); 
                },
                Collectors.counting()
            ));
    }


    /**
     * ========================================================================
     * [HÀM MỚI ĐƯỢC BỔ SUNG]
     * Logic tạo đơn hàng, tính toán tổng tiền và lưu vào DB
     * ========================================================================
     */
    @Transactional // Đảm bảo tất cả cùng thành công hoặc thất bại
    public Order createOrderFromRequest(String username, PlaceOrderRequest orderRequest) {
        
        // 1. Tìm User
        User user = userRepository.findByUsername(username) // Giả sử repo có findByUsername
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng: " + username));
        
        // 2. Tìm Địa chỉ
        Address address = addressRepository.findById(orderRequest.getSelectedAddressId())
                .filter(addr -> addr.getUser().getId().equals(user.getId())) // Đảm bảo địa chỉ thuộc về user
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ hợp lệ."));

        // 3. Chuyển đổi chuỗi variantIds (vd: "1,5,10") thành List<Long>
        List<Long> variantIds = Arrays.stream(orderRequest.getVariantIds().split(","))
                                      .map(Long::parseLong)
                                      .collect(Collectors.toList());
        
        List<ProductVariant> variants = variantRepository.findAllById(variantIds);
        
        if (variants.isEmpty() || variants.size() != variantIds.size()) {
            throw new RuntimeException("Một số sản phẩm trong giỏ hàng không tìm thấy.");
        }

        // 4. Tạo Order chính
        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setCreatedAt(LocalDateTime.now());
        
        // Sao chép thông tin địa chỉ vào đơn hàng
 //       newOrder.setRecipientName(address.getFullName());
//        newOrder.setShippingAddress(address.getAddress());
  //      newOrder.setShippingPhone(address.getPhone());
      
        // [QUAN TRỌNG] Đặt trạng thái ban đầu là CHỜ THANH TOÁN
 //       newOrder.setOrderStatus(OrderStatus.PENDING); // Giả sử bạn có Enum OrderStatus.PENDING
        
 //       newOrder.setPaymentMethod(orderRequest.getPaymentMethod());
 //       newOrder.setShippingCost(30000L); // TODO: Tạm tính phí ship, bạn cần logic tính toán riêng
       
        // 5. Tạo Order Details và Tính tổng tiền
        long subtotal = 0;
        
        for (ProductVariant variant : variants) {
            // TODO: Bạn cần có logic lấy số lượng (quantity)
            // Hiện tại 'orderRequest' chưa có thông tin số lượng
            // Bạn cần sửa DTO và logic JS để gửi kèm số lượng, hoặc lấy từ giỏ hàng (Cart)
            
            // Tạm thời, tôi sẽ giả định số lượng là 1 CHO MỖI SẢN PHẨM
            int quantity = 1; // <--- TODO: CẦN THAY THẾ BẰNG LOGIC ĐÚNG (ví dụ: lấy từ giỏ hàng)

            // Kiểm tra tồn kho
            if (variant.getStock() < quantity) {
                throw new RuntimeException("Sản phẩm " + variant.getProduct().getName() + " không đủ hàng.");
            }
            
            // Trừ tồn kho
            variant.setStock(variant.getStock() - quantity);
            
            // Tạo chi tiết đơn hàng
 //           OrderDetail detail = new OrderDetail();
 //           detail.setOrder(newOrder);
 //           detail.setProductVariant(variant);
 //           detail.setQuantity(quantity);
 //           detail.setPrice(variant.getPrice()); // Lưu giá tại thời điểm mua
            
 //           subtotal += (variant.getPrice() * quantity);
            
            // Thêm detail vào list của Order
            // (Điều này giả định bạn có: @OneToMany(mappedBy="order", cascade=CascadeType.ALL) 
            // và private List<OrderDetail> orderDetails = new ArrayList<>(); trong Entity Order)
  //           newOrder.getOrderDetails().add(detail);
       }
        
        // 6. Cập nhật lại tồn kho hàng loạt
        variantRepository.saveAll(variants);
        
        // 7. Set tổng tiền
  //      newOrder.setSubtotal(subtotal);
  //      newOrder.setGrandTotal(subtotal + newOrder.getShippingCost()); // Tổng tiền = Tạm tính + Phí ship
        
        // 8. Lưu Order (và OrderDetail sẽ được lưu theo nhờ CascadeType.ALL)
        Order savedOrder = orderRepository.save(newOrder);
        
        // 9. TODO: Xóa sản phẩm khỏi giỏ hàng
        // cartService.clearCartItems(user.getId(), variantIds);

        return savedOrder;
        return orders.stream().collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));
    }

    public void deliverOrder(Long orderId, Long shipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));

        if (order.getShipper() == null || !order.getShipper().getUserId().equals(shipperId)) {
            throw new SecurityException("Bạn không có quyền cập nhật đơn hàng này!");
        }

        order.setOrderStatus("DELIVERED");
        orderRepository.save(order);

        // Gửi email thông báo đến người dùng sau khi cập nhật thành công
        emailService.sendDeliveryConfirmation(order.getUser().getEmail(), order.getId());
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId); // Giả sử thêm method findByUserId trong OrderRepository
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));
    }
}