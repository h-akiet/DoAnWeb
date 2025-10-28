// src/main/java/com/oneshop/service/impl/OrderServiceImpl.java
package com.oneshop.service.impl;

import com.oneshop.dto.CartDto;
import com.oneshop.dto.CartItemDto;
import com.oneshop.dto.PlaceOrderRequest;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import com.oneshop.service.*;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;         
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap; 
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService { 

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ProductRepository productRepository; // <<< THÊM DÒNG NÀY
    @Autowired private UserService userService;
    @Autowired private ProductVariantService productVariantService;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired @Lazy private CartService cartService;
    @Autowired private EmailService emailService;
    @Autowired private PromotionService promotionService;
   

    @Autowired @Lazy 
    private ProductService productService;

   

    @Override
    @Transactional(readOnly = true)
    public Order getOrderDetails(Long orderId, Long shopId) {
        logger.debug("Getting order details for orderId: {}, shopId: {}", orderId, shopId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng #" + orderId));
        if (order.getShop() == null || !order.getShop().getId().equals(shopId)) {
            logger.warn("Security violation: Shop {} tried to access order {} of shop {}", shopId, orderId, order.getShop() != null ? order.getShop().getId() : "null");
            throw new SecurityException("Bạn không có quyền xem đơn hàng này.");
        }
        Hibernate.initialize(order.getOrderDetails());
        return order;
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long shopId) {
        logger.info("Updating order status - orderId: {}, newStatus: {}, shopId: {}", orderId, newStatus, shopId);
        Order order = getOrderDetails(orderId, shopId);
        validateStatusTransition(order.getOrderStatus(), newStatus);
        order.setOrderStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        logger.info("Order {} status updated to {}", orderId, newStatus);
        return updatedOrder;
    }

    private void validateStatusTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        if (newStatus == OrderStatus.CANCELLED && !(oldStatus == OrderStatus.PENDING || oldStatus == OrderStatus.CONFIRMED)) {
             throw new IllegalStateException("Không thể hủy đơn hàng ở trạng thái " + oldStatus);
        }
        if (oldStatus == OrderStatus.DELIVERING && !(newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.RETURNED)) {
             throw new IllegalStateException("Đơn hàng đang giao chỉ có thể chuyển thành Đã giao hoặc Trả hàng.");
        }
        if (oldStatus == OrderStatus.DELIVERED && newStatus != OrderStatus.RETURNED) {
            throw new IllegalStateException("Không thể thay đổi trạng thái đơn hàng đã giao (trừ trả hàng).");
        }
         if (oldStatus == OrderStatus.CANCELLED || oldStatus == OrderStatus.RETURNED) {
            throw new IllegalStateException("Không thể thay đổi trạng thái đơn hàng đã hủy hoặc đã trả.");
         }
    }

    @Override
    @Transactional(readOnly = true)
    public long countNewOrdersByShop(Long shopId) {
        logger.debug("Counting new orders for shopId: {}", shopId);
        return orderRepository.countByShopIdAndOrderStatus(shopId, OrderStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueByShop(Long shopId) {
        logger.debug("Calculating total revenue for shopId: {}", shopId);
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenueByShopIdAndStatus(shopId, OrderStatus.DELIVERED);
        return totalRevenue != null ? totalRevenue : BigDecimal.ZERO; 
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentMonthRevenueByShop(Long shopId) {
        logger.debug("Calculating current month revenue for shopId: {}", shopId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        BigDecimal monthlyRevenue = orderRepository.calculateTotalRevenueByShopIdAndStatusBetweenDates(
                shopId, OrderStatus.DELIVERED, startOfMonth, endOfMonth);
        return monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public long countDeliveredOrdersByShop(Long shopId) {
        logger.debug("Counting delivered orders for shopId: {}", shopId);
        return orderRepository.countByShopIdAndOrderStatus(shopId, OrderStatus.DELIVERED);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMonthlyRevenueData(Long shopId, int months) {
        logger.debug("Fetching monthly revenue data for last {} months for shopId: {}", months, shopId);
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months - 1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Object[]> results = orderRepository.findMonthlyRevenueByShopIdAndStatusSinceDate(
                shopId, OrderStatus.DELIVERED, startDate);

        Map<String, BigDecimal> monthlyRevenueMap = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.now();
        while (!currentMonth.isAfter(endMonth)) {
            monthlyRevenueMap.put(currentMonth.format(monthFormatter), BigDecimal.ZERO);
            currentMonth = currentMonth.plusMonths(1);
        }
        for (Object[] result : results) {
            String monthYear = (String) result[0];
            BigDecimal revenue = (BigDecimal) result[1]; 
            if (monthYear != null && revenue != null) {
                monthlyRevenueMap.put(monthYear, revenue);
            }
        }
        Map<String, BigDecimal> finalMap = new LinkedHashMap<>();
        List<String> sortedKeys = new ArrayList<>(monthlyRevenueMap.keySet());
        int startIndex = Math.max(0, sortedKeys.size() - months);
        for (int i = startIndex; i < sortedKeys.size(); i++) {
             String key = sortedKeys.get(i);
             finalMap.put(key, monthlyRevenueMap.get(key));
        }
        return finalMap;
    }

     @Override
    @Transactional(readOnly = true)
    public List<Order> findOrdersByCurrentUser(String username) {
        logger.debug("Finding orders for current user: {}", username);
        try {
            User currentUser = userService.findByUsername(username);
            return orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        } catch (UsernameNotFoundException e) {
             logger.error("User not found when finding orders: {}", username);
             return Collections.emptyList();
        }
    }
    
     @Override
     @Transactional(readOnly = true)
     public Page<Order> getOrdersByShop(Long shopId, Optional<OrderStatus> status, Pageable pageable) {
         logger.debug("Fetching orders for shopId: {}, status: {}, pageable: {}", shopId, status, pageable);
         Page<Order> orderPage;
         if (status.isPresent()) {
             orderPage = orderRepository.findByShopIdAndOrderStatus(shopId, status.get(), pageable);
         } else {
             orderPage = orderRepository.findByShopId(shopId, pageable);
         }
         return orderPage;
     }

     // ... (Các phương thức khác không đổi) ...
     
     @Override
     @Transactional(rollbackFor = Exception.class)
     public Order createOrderFromRequest(String username, PlaceOrderRequest orderRequest) {
         logger.info("Creating order from request for user: {}", username);
         User user = userService.findByUsername(username);
         Address address = addressRepository.findById(orderRequest.getSelectedAddressId())
                 .filter(addr -> addr.getUser().getId().equals(user.getId()))
                 .orElseThrow(() -> new EntityNotFoundException("Địa chỉ giao hàng không hợp lệ."));
         List<Long> variantIds = Arrays.stream(orderRequest.getVariantIds().split(","))
                                       .map(Long::parseLong).distinct().collect(Collectors.toList());
         if (variantIds.isEmpty()) { throw new IllegalArgumentException("Không có sản phẩm nào được chọn."); }

         CartDto userCart = cartService.getCartForUser(username);
         Map<Long, CartItemDto> cartItemsMap = userCart.getItems();
         List<CartItemDto> itemsToOrder = variantIds.stream().map(cartItemsMap::get)
             .filter(item -> item != null && item.getQuantity() > 0).collect(Collectors.toList());
         if (itemsToOrder.isEmpty()) { throw new IllegalArgumentException("Sản phẩm không có trong giỏ hoặc số lượng không hợp lệ."); }

         Order newOrder = new Order();
         newOrder.setUser(user); newOrder.setCreatedAt(LocalDateTime.now());
         newOrder.setRecipientName(address.getFullName()); newOrder.setShippingAddress(address.getAddress()); newOrder.setShippingPhone(address.getPhone());
         newOrder.setOrderStatus(OrderStatus.PENDING); newOrder.setPaymentMethod(orderRequest.getPaymentMethod());

         BigDecimal subtotal = BigDecimal.ZERO;
         List<ProductVariant> variantsToUpdateStock = new ArrayList<>();
         Shop orderShop = null;

         // 1. Lặp và tính subtotal + kiểm tra kho
         for (CartItemDto cartItem : itemsToOrder) {
             ProductVariant variant = productVariantService.findOptionalVariantById(cartItem.getProductId())
                 .orElseThrow(() -> new EntityNotFoundException("Sản phẩm ID " + cartItem.getProductId() + " không tồn tại."));
              Shop currentItemShop = variant.getProduct().getShop();
              if (currentItemShop == null) { throw new RuntimeException("Lỗi: Sản phẩm '" + variant.getProduct().getName() + "' không thuộc về gian hàng nào."); }
              if (orderShop == null) { orderShop = currentItemShop; }
              else if (!orderShop.getId().equals(currentItemShop.getId())) { throw new IllegalArgumentException("Không thể đặt hàng sản phẩm từ nhiều gian hàng khác nhau trong cùng một đơn."); }

             int requestedQuantity = cartItem.getQuantity();
             if (variant.getStock() < requestedQuantity) { throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + " - " + variant.getName() + "' không đủ hàng (còn " + variant.getStock() + ")."); }
             
             variant.setStock(variant.getStock() - requestedQuantity);
             variantsToUpdateStock.add(variant);
             
             OrderDetail detail = new OrderDetail();
             detail.setOrder(newOrder); detail.setProductVariant(variant); detail.setQuantity(requestedQuantity); detail.setPrice(variant.getPrice());
             newOrder.getOrderDetails().add(detail);
             subtotal = subtotal.add(variant.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));
         }
         if (orderShop == null) { throw new RuntimeException("Lỗi: Không thể xác định gian hàng cho đơn hàng."); }
         newOrder.setShop(orderShop);
         
         // 2. Tính toán Phí Ship
         BigDecimal shippingCost = calculateShippingCost(subtotal, address);
         
         // 3. TÍNH TOÁN VOUCHER (GÁN ENTITY VÀO KHÓA NGOẠI)
         BigDecimal discountAmount = BigDecimal.ZERO;
         Promotion appliedPromotion = null; // Biến Entity Promotion
         
         if (userCart.getAppliedVoucherCode() != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
             
             // GỌI HÀM TÍNH DISCOUNT
             discountAmount = promotionService.calculateDiscountForOrder(userCart, subtotal);
             
             // TÌM VÀ GÁN PROMOTION ENTITY (KHẮC PHỤC LỖI promotion_id NULL)
             Optional<Promotion> promoOpt = promotionService.findByDiscountCode(userCart.getAppliedVoucherCode());
             if (promoOpt.isPresent()) {
                 appliedPromotion = promoOpt.get();
             }
             
             // Xử lý Freeship nếu cần
             if (appliedPromotion != null && "FREE_SHIPPING".equalsIgnoreCase(userCart.getAppliedVoucherTypeCode())) {
                  shippingCost = BigDecimal.ZERO;
             }
             
             // Đảm bảo discountAmount không vượt quá subtotal
             discountAmount = discountAmount.min(subtotal);
             
             // Cần gán lại shippingCost nếu có thay đổi
             newOrder.setShippingCost(shippingCost);
             // GÁN PROMOTION ENTITY VÀ DISCOUNT AMOUNT
             newOrder.setPromotion(appliedPromotion); 
             newOrder.setDiscountAmount(discountAmount); 
             
         } else {
              // Nếu không có voucher
              newOrder.setShippingCost(shippingCost);
              newOrder.setDiscountAmount(BigDecimal.ZERO);
              newOrder.setPromotion(null); // Đảm bảo gán NULL rõ ràng
         }

         // 4. GÁN TỔNG CUỐI CÙNG (GRAND TOTAL)
         BigDecimal finalGrandTotal = subtotal
                                      .subtract(discountAmount)
                                      .add(shippingCost)
                                      .max(BigDecimal.ZERO)
                                      .setScale(0, RoundingMode.HALF_UP);

         newOrder.setSubtotal(subtotal);
         newOrder.setTotal(finalGrandTotal);
         
         Order savedOrder = orderRepository.save(newOrder); 
         
         // 5. XÓA VOUCHER KHỎI SESSION (QUAN TRỌNG)
         promotionService.removeVoucher(username);
         logger.info("Voucher cleared from session after order creation.");
         
         // ... (Phần lưu kho và cập nhật tổng sản phẩm cha không đổi) ...
         
         variantRepository.saveAll(variantsToUpdateStock); 
         variantRepository.flush(); 
         logger.debug("Đã flush thay đổi kho variant xuống database.");
         
         // ... (Phần cập nhật kho tổng sản phẩm cha không đổi) ...
         
         // Flush để đẩy tất cả thay đổi xuống DB
         productRepository.flush();
         logger.info("===> ĐÃ FLUSH tất cả products xuống database ✓✓✓");

         List<Long> orderedVariantIds = itemsToOrder.stream().map(CartItemDto::getProductId).collect(Collectors.toList());
         cartService.clearCartItems(user.getId(), orderedVariantIds);
         logger.info("Đơn hàng {} đã được tạo thành công.", savedOrder.getId());
         return savedOrder;
     }
    
    @Override
    @Transactional(readOnly = true)
    public Order findOrderByIdAndUser(Long orderId, String username) {
        logger.debug("Finding order by ID: {} for user: {}", orderId, username);
        Optional<Order> orderOptional = orderRepository.findByIdAndUser_Username(orderId, username);
        return orderOptional.orElseThrow(() -> {
                logger.warn("Order {} not found or access denied for user {}", orderId, username);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập.");
            });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String username) {
         logger.warn("User {} attempting to cancel order {}", username, orderId);
        Order order = findOrderByIdAndUser(orderId, username);
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            logger.warn("Cannot cancel order {} with status {}", orderId, currentStatus);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy đơn hàng ở trạng thái " + currentStatus + ".");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        List<ProductVariant> variantsToRestoreStock = new ArrayList<>();
        if(order.getOrderDetails() != null) { 
            for (OrderDetail detail : order.getOrderDetails()) {
                ProductVariant variant = detail.getProductVariant();
                if (variant != null) {
                    variant.setStock(variant.getStock() + detail.getQuantity());
                    variantsToRestoreStock.add(variant);
                    logger.debug("Restoring stock for variant {}: +{}", variant.getVariantId(), detail.getQuantity());
                } else {
                     logger.warn("OrderDetail {} in cancelled Order {} has null ProductVariant.", detail.getId(), orderId);
                }
            }
        } else {
             logger.warn("Order {} has null OrderDetails during cancellation.", orderId);
        }
        
        variantRepository.saveAll(variantsToRestoreStock);
        variantRepository.flush(); // Ép lưu kho variant
        logger.debug("Flushed variant stock restores to database.");
        
        // ===>>> CẬP NHẬT KHO TỔNG KHI HỦY ĐƠN <<<===
        try {
            Set<Product> productsToUpdate = variantsToRestoreStock.stream()
                                                .map(ProductVariant::getProduct)
                                                .collect(Collectors.toSet());
            logger.debug("Triggering stock/price update for {} parent products after cancellation.", productsToUpdate.size());
            
            for (Product product : productsToUpdate) {
                Set<ProductVariant> allVariantsSet = product.getVariants();
                
                if (allVariantsSet == null || allVariantsSet.isEmpty()) {
                    logger.warn("Product {} has no variants", product.getProductId());
                    continue;
                }
                
                List<ProductVariant> allVariants = new ArrayList<>(allVariantsSet);
                
                // Tính lại kho tổng
                int totalStock = allVariants.stream()
                        .mapToInt(ProductVariant::getStock)
                        .sum();
                
                // Tính lại giá min/max
                BigDecimal minPrice = allVariants.stream()
                        .map(ProductVariant::getPrice)
                        .filter(price -> price != null)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                
                BigDecimal maxPrice = allVariants.stream()
                        .map(ProductVariant::getPrice)
                        .filter(price -> price != null)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                
                product.setStock(totalStock);
                product.setPrice(minPrice);
                // Nếu Product có setMaxPrice thì uncomment dòng dưới
                // product.setMaxPrice(maxPrice);
                
                logger.info("✓ Restored Product {} - Stock: {}, Price: {}", 
                    product.getProductId(), totalStock, minPrice);
            }
            
            // ===>>> LƯU CÁC PRODUCT ĐÃ CẬP NHẬT <<<===
            if (!productsToUpdate.isEmpty()) {
                productRepository.saveAll(productsToUpdate);
                productRepository.flush();
                logger.info("✓ Saved {} parent products after order cancellation", productsToUpdate.size());
            }
            
        } catch (Exception e) {
             logger.error("Error updating parent product aggregate stock after order cancellation (orderId {}): {}", orderId, e.getMessage());
        }

        orderRepository.save(order);
        logger.info("Order {} cancelled by user {}", orderId, username);
    }

     @Override
     @Transactional(readOnly = true)
     public Order getOrderById(Long orderId) {
         logger.debug("Getting order by ID (no auth check): {}", orderId);
         return orderRepository.findById(orderId)
                 .orElseThrow(() -> new IllegalArgumentException("Đơn hàng #" + orderId + " không tồn tại!"));
     }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAssignedOrders(Long shipperId) {
         logger.debug("Fetching assigned orders for shipperId: {}", shipperId);
         List<OrderStatus> statusesToFetch = List.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERING);
        return orderRepository.findByShipper_IdAndOrderStatusInOrderByCreatedAtAsc(shipperId, statusesToFetch);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getOrderStats(Long shipperId) {
        logger.debug("Getting order stats for shipperId: {}", shipperId);
        List<Order> orders = orderRepository.findByShipper_Id(shipperId);
        return orders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getOrderStatus() != null ? order.getOrderStatus().name() : "UNKNOWN",
                Collectors.counting()
            ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(Long orderId, Long shipperId) {
        logger.info("Shipper {} attempting to mark order {} as delivered", shipperId, orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng #" + orderId + " không tồn tại!"));
        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperId)) {
            logger.warn("Security violation: Shipper {} tried to deliver order {} of shipper {}",
                        shipperId, orderId, order.getShipper() != null ? order.getShipper().getId() : "null");
            throw new SecurityException("Bạn không được gán để giao đơn hàng này!");
        }
        if (order.getOrderStatus() != OrderStatus.DELIVERING) {
             logger.warn("Cannot deliver order {}. Status: {}", orderId, order.getOrderStatus());
             throw new IllegalStateException("Chỉ có thể xác nhận giao đơn hàng 'Đang giao'.");
        }
        order.setOrderStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        logger.info("Order {} marked delivered by shipper {}", orderId, shipperId);
        try {
             if (order.getUser() != null && order.getUser().getEmail() != null) {
                 emailService.sendDeliveryConfirmation(order.getUser().getEmail(), order.getId());
             } else { logger.warn("Cannot send delivery confirm for order {}: User/email null.", orderId); }
        } catch (Exception e) { logger.error("Error sending delivery confirm email for order {}: {}", orderId, e.getMessage()); }
    }

     private BigDecimal calculateShippingCost(BigDecimal subtotal, Address address) {
        return BigDecimal.valueOf(30000); // Tạm thời phí cố định 30.000đ
     }
     
}