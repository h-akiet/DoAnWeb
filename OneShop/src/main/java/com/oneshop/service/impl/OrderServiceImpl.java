// src/main/java/com/oneshop/service/impl/OrderServiceImpl.java
package com.oneshop.service.impl;

import com.oneshop.dto.CartDto;
import com.oneshop.dto.CartItemDto;
import com.oneshop.dto.PlaceOrderRequest;
import com.oneshop.entity.*;
import com.oneshop.entity.Role.RoleName;
import com.oneshop.repository.*;
import com.oneshop.service.*;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserService userService;
    @Autowired private ProductVariantService productVariantService;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired @Lazy private CartService cartService;
    @Autowired private EmailService emailService;
    @Autowired private PromotionService promotionService;
    @Autowired private ShippingCompanyRepository shippingCompanyRepository;
    @Autowired @Lazy private ProductService productService;
    @Autowired private RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Order getOrderDetails(Long orderId, Long shopId) {
        logger.debug("Getting order details for orderId: {}, shopId: {}", orderId, shopId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng #" + orderId));
        
        // Kiểm tra quyền truy cập
        if (order.getShop() == null || !order.getShop().getId().equals(shopId)) {
            logger.warn("Security violation: Shop {} tried to access order {} of shop {}", 
                shopId, orderId, order.getShop() != null ? order.getShop().getId() : "null");
            throw new SecurityException("Bạn không có quyền xem đơn hàng này.");
        }
        
        // Chủ động load các quan hệ lazy
        Hibernate.initialize(order.getOrderDetails());
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Hibernate.initialize(detail.getProductVariant());
                if (detail.getProductVariant() != null) {
                    Hibernate.initialize(detail.getProductVariant().getProduct());
                }
            }
        }
        Hibernate.initialize(order.getShippingCompany());
        Hibernate.initialize(order.getShipper());
        Hibernate.initialize(order.getPromotion());
        
        return order;
    }

    @Override
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long shopId) {
        logger.info("Updating order status - orderId: {}, newStatus: {}, shopId: {}", orderId, newStatus, shopId);
        Order order = getOrderDetails(orderId, shopId);
        validateStatusTransition(order.getOrderStatus(), newStatus);

        // Cập nhật số lượng bán khi xác nhận đơn hàng
        if (order.getOrderStatus() == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            updateProductSaleCounts(order);
        }

        order.setOrderStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        logger.info("Order {} status updated to {}", orderId, newStatus);
        return updatedOrder;
    }

    private void validateStatusTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        // Không thể hủy đơn hàng đã giao hoặc đang giao
        if (newStatus == OrderStatus.CANCELLED && 
            !(oldStatus == OrderStatus.PENDING || oldStatus == OrderStatus.CONFIRMED)) {
            throw new IllegalStateException("Không thể hủy đơn hàng ở trạng thái " + oldStatus);
        }
        
        // Đơn hàng đang giao chỉ có thể chuyển thành Đã giao hoặc Trả hàng
        if (oldStatus == OrderStatus.DELIVERING && 
            !(newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.RETURNED)) {
            throw new IllegalStateException("Đơn hàng đang giao chỉ có thể chuyển thành Đã giao hoặc Trả hàng.");
        }
        
        // Không thể thay đổi trạng thái đơn hàng đã giao (trừ trả hàng)
        if (oldStatus == OrderStatus.DELIVERED && newStatus != OrderStatus.RETURNED) {
            throw new IllegalStateException("Không thể thay đổi trạng thái đơn hàng đã giao (trừ trả hàng).");
        }
        
        // Không thể thay đổi trạng thái đơn hàng đã hủy hoặc đã trả
        if (oldStatus == OrderStatus.CANCELLED || oldStatus == OrderStatus.RETURNED) {
            throw new IllegalStateException("Không thể thay đổi trạng thái đơn hàng đã hủy hoặc đã trả.");
        }
    }

    private void updateProductSaleCounts(Order order) {
        Map<Long, Product> productsToUpdateMap = new HashMap<>();

        for (OrderDetail detail : order.getOrderDetails()) {
            ProductVariant variant = detail.getProductVariant();
            if (variant == null || variant.getProduct() == null) {
                logger.warn("OrderDetail {} in Order {} has null variant or product, skipping stock/sales update.", 
                    detail.getId(), order.getId());
                continue;
            }

            Product product = variant.getProduct();
            int quantityBought = detail.getQuantity();

            // Lấy hoặc tạo product để cập nhật
            Product productToUpdate = productsToUpdateMap.computeIfAbsent(
                product.getProductId(), 
                id -> productRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found for update: " + id))
            );

            // Cập nhật số lượng đã bán
            int currentSales = productToUpdate.getSalesCount();
            productToUpdate.setSalesCount(currentSales + quantityBought);

            // Cập nhật tồn kho tổng
            Integer currentTotalStock = productToUpdate.getStock();
            if (currentTotalStock != null) {
                if (currentTotalStock < quantityBought) {
                    logger.error("Critical Stock Error! Product {} (Total Stock: {}) insufficient for quantity {}.", 
                        product.getProductId(), currentTotalStock, quantityBought);
                    throw new IllegalStateException("Không đủ tồn kho tổng cho sản phẩm ID " + product.getProductId());
                }
                productToUpdate.setStock(currentTotalStock - quantityBought);
                logger.debug("Updated Product {} aggregate stock: {} -> {}", 
                    product.getProductId(), currentTotalStock, productToUpdate.getStock());
            } else {
                logger.warn("Product {} has null aggregate stock. Cannot decrease stock.", product.getProductId());
            }
        }

        // Lưu tất cả sản phẩm đã cập nhật
        if (!productsToUpdateMap.isEmpty()) {
            productRepository.saveAll(productsToUpdateMap.values());
            logger.info("Updated sales count and aggregate stock for {} products related to order {}", 
                productsToUpdateMap.size(), order.getId());
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
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
            .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        
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
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months - 1)
            .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // SỬA LỖI: Truyền status dưới dạng String
        List<Object[]> results = orderRepository.findMonthlyRevenueByShopIdAndStatusSinceDate(
            shopId, OrderStatus.DELIVERED.name(), startDate);

        // Tạo map với tất cả các tháng trong khoảng thời gian
        Map<String, BigDecimal> monthlyRevenueMap = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.now();

        while (!currentMonth.isAfter(endMonth)) {
            monthlyRevenueMap.put(currentMonth.format(monthFormatter), BigDecimal.ZERO);
            currentMonth = currentMonth.plusMonths(1);
        }

        // Điền dữ liệu từ kết quả query
        for (Object[] result : results) {
            String monthYear = (String) result[0];
            BigDecimal revenue = (result[1] instanceof Number) ? 
                new BigDecimal(((Number) result[1]).toString()) : BigDecimal.ZERO;
            if (monthYear != null && revenue != null) {
                monthlyRevenueMap.put(monthYear, revenue);
            }
        }

        // Chỉ giữ lại số tháng yêu cầu
        List<String> sortedKeys = new ArrayList<>(monthlyRevenueMap.keySet());
        int startIndex = Math.max(0, sortedKeys.size() - months);
        Map<String, BigDecimal> finalMap = new LinkedHashMap<>();
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
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());

            // Load các quan hệ lazy
            for (Order order : orders) {
                Hibernate.initialize(order.getOrderDetails());
                if (order.getOrderDetails() != null) {
                    for (OrderDetail detail : order.getOrderDetails()) {
                        Hibernate.initialize(detail.getProductVariant());
                        if (detail.getProductVariant() != null) {
                            Hibernate.initialize(detail.getProductVariant().getProduct());
                            // Tính URL ảnh chính
                            try {
                                Product product = detail.getProductVariant().getProduct();
                                if (product != null && product.getPrimaryImageUrl() == null) {
                                    String primaryUrl = productService.calculatePrimaryImageUrl(product);
                                    product.setPrimaryImageUrl(primaryUrl);
                                }
                            } catch (Exception e) {
                                logger.error("Error setting primary image for product in order {}: {}", 
                                    order.getId(), e.getMessage());
                            }
                        }
                    }
                }
            }
            return orders;
        } catch (UsernameNotFoundException e) {
            logger.error("User not found when finding orders: {}", username);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByShop(Long shopId, Optional<OrderStatus> status, Pageable pageable) {
        logger.debug("Fetching orders for shopId: {}, status: {}", shopId, status);
        Page<Order> orderPage;
        if (status.isPresent()) {
            orderPage = orderRepository.findByShopIdAndOrderStatus(shopId, status.get(), pageable);
        } else {
            orderPage = orderRepository.findByShopId(shopId, pageable);
        }
        
        // Load thông tin user cho các đơn hàng
        orderPage.getContent().forEach(order -> Hibernate.initialize(order.getUser()));
        return orderPage;
    }

    @Override
    public Order createOrderFromRequest(String username, PlaceOrderRequest orderRequest) {
        logger.info("Creating order from request for user: {}", username);

        // 1. Lấy thông tin user và địa chỉ
        User user = userService.findByUsername(username);
        Address address = addressRepository.findById(orderRequest.getSelectedAddressId())
            .filter(addr -> addr.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new EntityNotFoundException("Địa chỉ giao hàng không hợp lệ hoặc không thuộc về bạn."));

        // 2. Parse danh sách variant IDs
        List<Long> variantIds = Arrays.stream(orderRequest.getVariantIds().split(","))
            .map(Long::parseLong)
            .distinct()
            .collect(Collectors.toList());
            
        if (variantIds.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm nào được chọn để đặt hàng.");
        }

        // 3. Lấy thông tin giỏ hàng
        CartDto userCart = cartService.getCartForUser(username);
        Map<Long, CartItemDto> cartItemsMap = userCart.getItems();

        List<CartItemDto> itemsToOrder = variantIds.stream()
            .map(cartItemsMap::get)
            .filter(item -> item != null && item.getQuantity() > 0)
            .collect(Collectors.toList());

        if (itemsToOrder.isEmpty()) {
            throw new IllegalArgumentException("Các sản phẩm được chọn không hợp lệ.");
        }

        // 4. Tạo đơn hàng mới
        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setCreatedAt(LocalDateTime.now());
        newOrder.setRecipientName(address.getFullName());
        newOrder.setShippingAddress(address.getAddress());
        newOrder.setShippingPhone(address.getPhone());
        newOrder.setOrderStatus(OrderStatus.PENDING);
        newOrder.setPaymentMethod(orderRequest.getPaymentMethod());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<ProductVariant> variantsToUpdateStock = new ArrayList<>();
        Shop orderShop = null;

        // 5. Xử lý từng sản phẩm trong đơn hàng
        for (CartItemDto cartItem : itemsToOrder) {
            ProductVariant variant = productVariantService.findOptionalVariantById(cartItem.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Sản phẩm biến thể ID " + cartItem.getProductId() + " không còn tồn tại."));

            // Kiểm tra shop
            Shop currentItemShop = variant.getProduct().getShop();
            if (currentItemShop == null) {
                throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + "' không thuộc về gian hàng nào.");
            }
            if (orderShop == null) {
                orderShop = currentItemShop;
            } else if (!orderShop.getId().equals(currentItemShop.getId())) {
                throw new IllegalArgumentException("Không thể đặt hàng các sản phẩm từ nhiều gian hàng khác nhau.");
            }

            // Kiểm tra và trừ tồn kho
            int requestedQuantity = cartItem.getQuantity();
            if (variant.getStock() < requestedQuantity) {
                throw new RuntimeException("Sản phẩm '" + variant.getProduct().getName() + " - " + 
                    variant.getName() + "' không đủ hàng (chỉ còn " + variant.getStock() + ").");
            }
            variant.setStock(variant.getStock() - requestedQuantity);
            variantsToUpdateStock.add(variant);

            // Tạo order detail
            OrderDetail detail = new OrderDetail();
            detail.setOrder(newOrder);
            detail.setProductVariant(variant);
            detail.setQuantity(requestedQuantity);
            detail.setPrice(variant.getPrice());
            newOrder.getOrderDetails().add(detail);

            subtotal = subtotal.add(variant.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));
        }

        if (orderShop == null) {
            throw new RuntimeException("Không thể xác định gian hàng cho đơn hàng này.");
        }
        newOrder.setShop(orderShop);
        newOrder.setSubtotal(subtotal);

        // 6. Tính phí vận chuyển
        BigDecimal shippingCost = calculateShippingCost(subtotal, address, null);
        newOrder.setShippingCost(shippingCost);

        // 7. Áp dụng voucher
        BigDecimal discountAmount = BigDecimal.ZERO;
        Promotion appliedPromotion = null;
        if (userCart.getAppliedVoucherCode() != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = promotionService.calculateDiscountForOrder(userCart, subtotal);

            Optional<Promotion> promoOpt = promotionService.findByDiscountCode(userCart.getAppliedVoucherCode());
            if (promoOpt.isPresent()) {
                appliedPromotion = promoOpt.get();
            } else {
                logger.warn("Applied voucher code '{}' from session not found in DB.", userCart.getAppliedVoucherCode());
            }

            // Xử lý freeship
            if (appliedPromotion != null && "FREE_SHIPPING".equalsIgnoreCase(userCart.getAppliedVoucherTypeCode())) {
                shippingCost = BigDecimal.ZERO;
                newOrder.setShippingCost(shippingCost);
                logger.info("Applied FREE_SHIPPING voucher {}. Shipping cost set to 0.", userCart.getAppliedVoucherCode());
            }

            discountAmount = discountAmount.min(subtotal);
            newOrder.setPromotion(appliedPromotion);
            newOrder.setDiscountAmount(discountAmount);
        } else {
            newOrder.setDiscountAmount(BigDecimal.ZERO);
            newOrder.setPromotion(null);
        }

        // 8. Tính tổng tiền và lưu đơn hàng
        newOrder.recalculateTotal();
        Order savedOrder = orderRepository.save(newOrder);

        // 9. Cập nhật tồn kho
        if (!variantsToUpdateStock.isEmpty()) {
            variantRepository.saveAll(variantsToUpdateStock);
            logger.debug("Updated stock for {} variants.", variantsToUpdateStock.size());
        }

        // 10. Cập nhật tồn kho tổng cho sản phẩm cha
        Set<Product> parentProductsToUpdate = variantsToUpdateStock.stream()
            .map(ProductVariant::getProduct)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
            
        if (!parentProductsToUpdate.isEmpty()) {
            parentProductsToUpdate.forEach(product -> {
                try {
                    productService.updateProductStockAndPriceFromVariants(product);
                } catch (Exception e) {
                    logger.error("Error updating aggregate stock/price for parent product {}: {}", 
                        product.getProductId(), e.getMessage());
                }
            });
        }

        // 11. Xóa voucher và sản phẩm khỏi giỏ hàng
        promotionService.removeVoucher(username);
        List<Long> orderedVariantIds = itemsToOrder.stream()
            .map(CartItemDto::getProductId)
            .collect(Collectors.toList());
        cartService.clearCartItems(user.getId(), orderedVariantIds);

        logger.info("Order {} created successfully for user {}", savedOrder.getId(), username);
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order findOrderByIdAndUser(Long orderId, String username) {
        logger.debug("Finding order by ID: {} for user: {}", orderId, username);
        Optional<Order> orderOptional = orderRepository.findByIdAndUser_Username(orderId, username);
        return orderOptional.orElseThrow(() -> {
            logger.warn("Order {} not found or access denied for user {}", orderId, username);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập.");
        });
    }

    @Override
    public void cancelOrder(Long orderId, String username) {
        logger.warn("User {} attempting to cancel order {}", username, orderId);
        Order order = findOrderByIdAndUser(orderId, username);

        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            logger.warn("Cannot cancel order {} with status {}", orderId, currentStatus);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Không thể hủy đơn hàng ở trạng thái này (" + currentStatus + ").");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        // Hoàn lại tồn kho
        List<ProductVariant> variantsToRestoreStock = new ArrayList<>();
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                ProductVariant variant = detail.getProductVariant();
                if (variant != null) {
                    variant.setStock(variant.getStock() + detail.getQuantity());
                    variantsToRestoreStock.add(variant);
                    logger.debug("Restoring stock for variant {}: +{}", variant.getVariantId(), detail.getQuantity());
                }
            }
        }

        if (!variantsToRestoreStock.isEmpty()) {
            variantRepository.saveAll(variantsToRestoreStock);
            
            // Cập nhật tồn kho tổng cho sản phẩm cha
            Set<Product> parentProductsToUpdate = variantsToRestoreStock.stream()
                .map(ProductVariant::getProduct)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
                
            parentProductsToUpdate.forEach(product -> {
                try {
                    productService.updateProductStockAndPriceFromVariants(product);
                } catch (Exception e) {
                    logger.error("Error updating aggregate stock/price for parent product {}: {}", 
                        product.getProductId(), e.getMessage());
                }
            });
        }

        orderRepository.save(order);
        logger.info("Order {} successfully cancelled by user {}", orderId, username);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        logger.debug("Getting order by ID (no auth check): {}", orderId);
        return orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Đơn hàng #" + orderId + " không tồn tại!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAssignedOrders(Long shipperId) {
        logger.debug("Fetching assigned orders for shipperId: {}", shipperId);
        List<OrderStatus> statusesToFetch = List.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERING);
        List<Order> orders = orderRepository.findByShipper_IdAndOrderStatusInOrderByCreatedAtAsc(shipperId, statusesToFetch);
        orders.forEach(order -> Hibernate.initialize(order.getUser()));
        return orders;
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
    public void deliverOrder(Long orderId, Long shipperId) {
        updateShipperOrderStatus(orderId, shipperId, OrderStatus.DELIVERED);
    }

    private BigDecimal calculateShippingCost(BigDecimal subtotal, Address address, ShippingCompany company) {
        logger.debug("Calculating shipping cost for subtotal: {}, company: {}", 
            subtotal, company != null ? company.getName() : "N/A");

        if (company == null) {
            logger.warn("No shipping company provided. Returning default fixed fee.");
            return new BigDecimal("30000");
        }

        Hibernate.initialize(company.getRules());
        if (company.getRules() == null || company.getRules().isEmpty()) {
            logger.warn("Shipping company {} has no rules defined. Returning 0.", company.getName());
            return BigDecimal.ZERO;
        }

        // Lấy rule đầu tiên (có thể cải thiện logic này)
        ShippingRule applicableRule = company.getRules().get(0);
        logger.debug("Applying rule '{}' with base fee {}", applicableRule.getRuleName(), applicableRule.getBaseFee());
        return applicableRule.getBaseFee();
    }

    @Override
    public Order updateShippingDetails(Long orderId, Long shopId, Long shippingCompanyId) {
        logger.info("Vendor updating shipping for orderId: {} with companyId: {}", orderId, shippingCompanyId);

        Order order = getOrderDetails(orderId, shopId);

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Không thể thay đổi đơn vị vận chuyển cho đơn hàng ở trạng thái " + order.getOrderStatus());
        }

        ShippingCompany selectedCompany = shippingCompanyRepository.findById(shippingCompanyId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn vị vận chuyển với ID: " + shippingCompanyId));

        if (!Boolean.TRUE.equals(selectedCompany.getIsActive())) {
            throw new IllegalArgumentException("Đơn vị vận chuyển '" + selectedCompany.getName() + "' hiện không hoạt động.");
        }

        BigDecimal newShippingCost = calculateShippingCost(order.getSubtotal(), null, selectedCompany);

        // Kiểm tra voucher freeship
        boolean isFreeShipping = order.getPromotion() != null && 
            order.getPromotion().getType() != null &&
            "FREE_SHIPPING".equalsIgnoreCase(order.getPromotion().getType().getCode());

        if (!isFreeShipping) {
            order.setShippingCost(newShippingCost);
        } else {
            order.setShippingCost(BigDecimal.ZERO);
            logger.info("FREE_SHIPPING voucher applied. Keeping shipping cost at 0.");
        }

        order.setShippingCompany(selectedCompany);
        order.recalculateTotal();

        Order updatedOrder = orderRepository.save(order);
        logger.info("Successfully updated shipping details for order {}. Final Total: {}", 
            orderId, updatedOrder.getTotal());

        return updatedOrder;
    }

    @Override
    public Order assignShipper(Long orderId, Long shopId, Long shipperId) {
        logger.info("Vendor assigning Shipper ID: {} to order {}", shipperId, orderId);

        Order order = getOrderDetails(orderId, shopId);

        if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể gán Shipper cho đơn hàng đã xác nhận (CONFIRMED).");
        }
        if (order.getShippingCompany() == null) {
            throw new IllegalStateException("Vui lòng chọn Đơn vị Vận chuyển trước khi gán Shipper.");
        }

        User shipper = userRepository.findById(shipperId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Shipper với ID: " + shipperId));

        if (shipper.getRole() == null || shipper.getRole().getName() != RoleName.SHIPPER) {
            throw new IllegalArgumentException("Người dùng ID " + shipperId + " không phải là Shipper.");
        }
        if (!shipper.isActivated()) {
            throw new IllegalArgumentException("Tài khoản Shipper ID " + shipperId + " hiện đang bị khóa.");
        }

        order.setShipper(shipper);
        order.setOrderStatus(OrderStatus.DELIVERING);

        Order updatedOrder = orderRepository.save(order);
        logger.info("Successfully assigned Shipper ID {} to order {} and set status to DELIVERING.", 
            shipperId, orderId);

        return updatedOrder;
    }

    @Override
    public void updateShipperOrderStatus(Long orderId, Long shipperId, OrderStatus newStatus) {
        logger.info("Shipper {} updating order {} status to {}", shipperId, orderId, newStatus);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Đơn hàng #" + orderId + " không tồn tại!"));

        if (order.getShipper() == null || !order.getShipper().getId().equals(shipperId)) {
            throw new SecurityException("Bạn không được gán để cập nhật đơn hàng này!");
        }

        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus != OrderStatus.DELIVERING) {
            throw new IllegalStateException("Chỉ có thể cập nhật trạng thái đơn hàng đang ở trạng thái 'Đang giao'.");
        }

        if (newStatus != OrderStatus.DELIVERED && newStatus != OrderStatus.RETURNED && newStatus != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Trạng thái cập nhật không hợp lệ cho Shipper.");
        }

        // Hoàn lại tồn kho nếu trả hàng hoặc hủy
        if (newStatus == OrderStatus.RETURNED || newStatus == OrderStatus.CANCELLED) {
            List<ProductVariant> variantsToRestoreStock = new ArrayList<>();
            if (order.getOrderDetails() != null) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    ProductVariant variant = detail.getProductVariant();
                    if (variant != null) {
                        variant.setStock(variant.getStock() + detail.getQuantity());
                        variantsToRestoreStock.add(variant);
                    }
                }
            }
            
            if (!variantsToRestoreStock.isEmpty()) {
                variantRepository.saveAll(variantsToRestoreStock);
                // Cập nhật tồn kho tổng cho sản phẩm cha
                Set<Product> parentProductsToUpdate = variantsToRestoreStock.stream()
                    .map(ProductVariant::getProduct)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                    
                parentProductsToUpdate.forEach(product -> {
                    try {
                        productService.updateProductStockAndPriceFromVariants(product);
                    } catch (Exception e) {
                        logger.error("Error updating aggregate stock/price for parent product {}: {}", 
                            product.getProductId(), e.getMessage());
                    }
                });
            }
        }

        order.setOrderStatus(newStatus);
        orderRepository.save(order);
        logger.info("Order {} status updated to {} by shipper {}", orderId, newStatus, shipperId);

        // Gửi email xác nhận giao hàng
        if (newStatus == OrderStatus.DELIVERED) {
            try {
                if (order.getUser() != null && order.getUser().getEmail() != null) {
                    emailService.sendDeliveryConfirmation(order.getUser().getEmail(), order.getId());
                    logger.info("Delivery confirmation email sent for order {}", orderId);
                }
            } catch (Exception e) {
                logger.error("Error sending delivery confirmation email for order {}: {}", orderId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getTopProductRevenueByShop(Long shopId, int limit) {
        logger.debug("Fetching top {} product revenue for shopId: {}", limit, shopId);
        
        Pageable topPageable = PageRequest.of(0, limit);
        List<Object[]> results = orderRepository.findTopProductRevenueByShop(shopId, OrderStatus.DELIVERED, topPageable);
        
        Map<String, BigDecimal> topProducts = new LinkedHashMap<>();
        for (Object[] result : results) {
            String productName = (String) result[0];
            BigDecimal revenue = (result[1] instanceof Number) ? 
                new BigDecimal(((Number) result[1]).toString()) : BigDecimal.ZERO;
            if (productName != null) {
                topProducts.put(productName, revenue);
            }
        }
        return topProducts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getCategoryRevenueDistributionByShop(Long shopId) {
        logger.debug("Fetching category revenue distribution for shopId: {}", shopId);
        
        List<Object[]> results = orderRepository.findCategoryRevenueByShop(shopId, OrderStatus.DELIVERED);
        
        Map<String, BigDecimal> categoryRevenue = new LinkedHashMap<>();
        for (Object[] result : results) {
            String categoryName = (String) result[0];
            if (categoryName == null || categoryName.trim().isEmpty()) {
                categoryName = "Chưa phân loại";
            }
            BigDecimal revenue = (result[1] instanceof Number) ? 
                new BigDecimal(((Number) result[1]).toString()) : BigDecimal.ZERO;

            categoryRevenue.put(categoryName, categoryRevenue.getOrDefault(categoryName, BigDecimal.ZERO).add(revenue));
        }
        return categoryRevenue;
    }
}