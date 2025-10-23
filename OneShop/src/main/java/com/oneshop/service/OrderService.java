package com.oneshop.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Import bị thiếu
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException; // Import bị thiếu

import com.oneshop.dto.PlaceOrderRequest;
import com.oneshop.entity.Address;
import com.oneshop.entity.Order;
import com.oneshop.entity.OrderDetail;
import com.oneshop.entity.ProductVariant;
import com.oneshop.entity.User;
import com.oneshop.entity.OrderStatus;
import com.oneshop.repository.AddressRepository;
import com.oneshop.repository.OrderDetailRepository;
import com.oneshop.repository.OrderRepository;
import com.oneshop.repository.ProductVariantRepository;
import com.oneshop.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private ProductVariantRepository variantRepository;
    
    // Giả định bạn có UserService
    @Autowired 
    private UserService userService;
    
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    
    // Giả định bạn có CartService
    @Autowired(required = false) 
    private CartService cartService;

    @Autowired
    private EmailService emailService;

    public List<Order> getAssignedOrders(Long shipperId) {
        // Yêu cầu phương thức List<Order> findByShipperId(Long shipperId); trong OrderRepository
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

    public List<Order> findOrdersByCurrentUser(String username) {
        
        // 1. Dùng UserService để tìm User object từ username
        User currentUser = userService.findByUsername(username); 

        // 2. Kiểm tra nếu không tìm thấy user
        if (currentUser == null) {
            return new ArrayList<>(); // Trả về danh sách rỗng
        }

        // 3. Lấy userId và gọi phương thức repository
        Long userId = currentUser.getId();
        
        // Yêu cầu phương thức List<Order> findByUserIdOrderByCreatedAtDesc(Long userId); trong OrderRepository
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Transactional
    public Order createOrderFromRequest(String username, PlaceOrderRequest orderRequest) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng: " + username));
        
        Address address = addressRepository.findById(orderRequest.getSelectedAddressId())
                .filter(addr -> addr.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy địa chỉ hợp lệ."));

        List<Long> variantIds = Arrays.stream(orderRequest.getVariantIds().split(","))
                                      .map(Long::parseLong)
                                      .collect(Collectors.toList());
        
        List<ProductVariant> variants = variantRepository.findAllById(variantIds);
        
        if (variants.isEmpty() || variants.size() != variantIds.size()) {
            throw new RuntimeException("Một số sản phẩm trong giỏ hàng không tìm thấy.");
        }

        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setCreatedAt(LocalDateTime.now());
        
        newOrder.setRecipientName(address.getFullName());
        newOrder.setShippingAddress(address.getAddress());
        newOrder.setShippingPhone(address.getPhone());
      
        newOrder.setOrderStatus(OrderStatus.PENDING); 
        newOrder.setPaymentMethod(orderRequest.getPaymentMethod());
        
        BigDecimal shippingCost = BigDecimal.valueOf(30000); 
        newOrder.setShippingCost(shippingCost);
       
        BigDecimal subtotal = BigDecimal.ZERO; 
        
        for (ProductVariant variant : variants) {
            int quantity = 1; // <--- TODO: CẦN THAY THẾ BẰNG LOGIC ĐÚNG

            if (variant.getStock() < quantity) {
                throw new RuntimeException("Sản phẩm " + variant.getProduct().getName() + " không đủ hàng.");
            }
            
            variant.setStock(variant.getStock() - quantity);
            
            OrderDetail detail = new OrderDetail();
            detail.setOrder(newOrder);
            detail.setProductVariant(variant);
            detail.setQuantity(quantity);
            detail.setPrice(variant.getPrice()); 
            
            BigDecimal quantityBD = BigDecimal.valueOf(quantity);
            BigDecimal itemTotal = variant.getPrice().multiply(quantityBD);
            subtotal = subtotal.add(itemTotal);
            
             newOrder.getOrderDetails().add(detail);
       }
        
        variantRepository.saveAll(variants);
        
        newOrder.setSubtotal(subtotal);
        newOrder.setTotal(subtotal.add(shippingCost)); 
        
        Order savedOrder = orderRepository.save(newOrder);
        
         if (cartService != null) {
            cartService.clearCartItems(user.getId(), variantIds);
         }

        return savedOrder;
    }

    @Transactional
    public void deliverOrder(Long orderId, Long shipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperId)) {
            throw new SecurityException("Bạn không có quyền cập nhật đơn hàng này!");
        }

        order.setOrderStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        emailService.sendDeliveryConfirmation(order.getUser().getEmail(), order.getId());
    }

    public List<Order> getUserOrders(Long userId) {
        // Yêu cầu phương thức List<Order> findByUserId(Long userId); trong OrderRepository
        return orderRepository.findByUserId(userId); 
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));
    }
    
    // [PHƯƠNG THỨC MỚI] - Đảm bảo chỉ người dùng sở hữu mới có thể truy cập đơn hàng
    public Order findOrderByIdAndUser(Long orderId, String username) {
        // Yêu cầu phương thức Optional<Order> findByIdAndUser_Username(Long id, String username); trong OrderRepository
        Optional<Order> orderOptional = orderRepository.findByIdAndUser_Username(orderId, username);
        
        return orderOptional.orElseThrow(() -> 
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found or access denied."));
    }

    // [PHƯƠNG THỨC HOÀN CHỈNH] - Xử lý logic hủy đơn hàng
    @Transactional
    public void cancelOrder(Long orderId, String username) {
        // 1. Kiểm tra quyền sở hữu và lấy đơn hàng
        Order order = findOrderByIdAndUser(orderId, username);

        String currentStatus = order.getOrderStatus().name();

        // 2. Kiểm tra trạng thái có thể hủy
        if (!"PENDING".equals(currentStatus) && !"CONFIRMED".equals(currentStatus)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Order #" + orderId + " cannot be cancelled as its status is " + currentStatus
            );
        }
        
        // 3. Cập nhật trạng thái
        order.setOrderStatus(OrderStatus.CANCELLED); 
        
        // 4. Hoàn trả kho (TODO: Thêm logic hoàn trả kho hàng từ OrderDetail)
        // Ví dụ:
        /*
        for (OrderDetail detail : order.getOrderDetails()) {
            ProductVariant variant = detail.getProductVariant();
            variant.setStock(variant.getStock() + detail.getQuantity());
            variantRepository.save(variant);
        }
        */
        
        orderRepository.save(order);
    }
}