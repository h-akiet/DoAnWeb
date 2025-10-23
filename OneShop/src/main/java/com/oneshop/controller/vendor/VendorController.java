package com.oneshop.controller.vendor;

import com.oneshop.dto.vendor.ProductDto;
import com.oneshop.dto.vendor.ProfileUpdateDto;
import com.oneshop.dto.vendor.PromotionDto;
import com.oneshop.dto.vendor.ShopDto;
import com.oneshop.entity.vendor.Category;
import com.oneshop.entity.vendor.Order;
import com.oneshop.entity.vendor.OrderStatus;
import com.oneshop.entity.vendor.Product;
import com.oneshop.entity.vendor.Promotion;
import com.oneshop.entity.vendor.PromotionTypeEntity;
import com.oneshop.entity.vendor.Shop;
import com.oneshop.entity.vendor.User;
import com.oneshop.repository.vendor.UserRepository;
import com.oneshop.service.vendor.CategoryService;
import com.oneshop.service.vendor.OrderService;
import com.oneshop.service.vendor.ProductService;
import com.oneshop.service.vendor.PromotionService;
import com.oneshop.service.vendor.PromotionTypeService;
import com.oneshop.service.vendor.ReportService;
import com.oneshop.service.vendor.ShopService;
import com.oneshop.service.vendor.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private OrderService orderService;
    @Autowired private ShopService shopService;
    @Autowired private PromotionService promotionService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private PromotionTypeService promotionTypeService; // Đảm bảo đã Autowired
    @Autowired private ReportService reportService; // Đảm bảo đã Autowired

    // --- Helper Method ---
    private User getAuthenticatedUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));
    }
    private Long getAuthenticatedShopId(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user.getShop() == null) {
            throw new RuntimeException("Người dùng này chưa có shop. Hãy đăng ký shop trước.");
        }
        return user.getShop().getId();
    }
    private Long getAuthenticatedUserId(Authentication authentication) {
        return getAuthenticatedUser(authentication).getId();
    }

    // --- Dashboard ---
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) { 
        model.addAttribute("currentPage", "dashboard");
        try { // Thêm try-catch để xử lý nếu chưa có shop
            Long shopId = getAuthenticatedShopId(authentication); 
            model.addAttribute("newOrderCount", orderService.countNewOrdersByShop(shopId));
            model.addAttribute("totalProductCount", productService.countProductsByShop(shopId));
        } catch (RuntimeException e) {
            // Nếu chưa có shop, đặt giá trị mặc định là 0
             model.addAttribute("newOrderCount", 0L);
             model.addAttribute("totalProductCount", 0L);
             // (Tùy chọn) Thêm thông báo yêu cầu đăng ký shop
             // model.addAttribute("shopRequiredMessage", "Vui lòng đăng ký thông tin shop để bắt đầu.");
        }
        model.addAttribute("monthlyRevenue", BigDecimal.ZERO);
        return "vendor/dashboard";
    }

    // --- Quản lý Shop ---
    @GetMapping("/shop")
    public String shopManagement(Model model, Authentication authentication) { 
        model.addAttribute("currentPage", "shop");
        Long userId = getAuthenticatedUserId(authentication); 
        Shop shop = shopService.getShopByUserId(userId);
        
        ShopDto dto = new ShopDto();
        dto.setShopName(shop.getName());
        dto.setShopDescription(shop.getDescription());
        dto.setContactEmail(shop.getContactEmail());
        dto.setContactPhone(shop.getContactPhone());

        model.addAttribute("shopDto", dto);
        model.addAttribute("shop", shop);
        return "vendor/shop_management";
    }

    @PostMapping("/shop/update")
    public String updateShop(@ModelAttribute ShopDto shopDto,
                             @RequestParam("logoFile") MultipartFile logoFile,
                             @RequestParam("bannerFile") MultipartFile bannerFile,
                             RedirectAttributes redirectAttributes, Authentication authentication) { 
        try {
            Long userId = getAuthenticatedUserId(authentication); 
            Shop shop = shopService.getShopByUserId(userId);
            shopService.updateShop(shop.getId(), shopDto, logoFile, bannerFile);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật shop thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/shop";
    }

    // --- Quản lý Sản phẩm ---
    @GetMapping("/products")
    public String productList(Model model, @RequestParam(defaultValue = "0") int page, Authentication authentication) { 
        model.addAttribute("currentPage", "products");
        Long shopId = getAuthenticatedShopId(authentication); 
        Pageable pageable = PageRequest.of(page, 10);
        Page<Product> productPage = productService.getProductsByShop(shopId, pageable);
        model.addAttribute("productPage", productPage);
        return "vendor/product_list";
    }

    @GetMapping("/products/add")
    public String addProductForm(Model model) {
        model.addAttribute("currentPage", "products");
        model.addAttribute("productDto", new ProductDto());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isEditMode", false);
        return "vendor/product_add";
    }

    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable("id") Long productId, Model model, Authentication authentication) { 
        model.addAttribute("currentPage", "products");
        Long shopId = getAuthenticatedShopId(authentication);
        Product product = productService.getProductById(productId);
        if(!product.getShop().getId().equals(shopId)) {
            return "redirect:/vendor/products"; 
        }
        
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setProductName(product.getName());
        dto.setProductDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setSalePrice(product.getSalePrice()); // Thêm giá sale vào DTO
        dto.setStock(product.getStock());
        dto.setProductTags(product.getTags());
        dto.setCategoryId(product.getCategory().getId());
        // (Tùy chọn) Thêm danh sách ảnh vào DTO để hiển thị ảnh cũ
        // dto.setImages(product.getImages()); 

        model.addAttribute("productDto", dto);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isEditMode", true);
        return "vendor/product_add";
    }

    @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute ProductDto productDto, // Thêm @Valid
                              BindingResult bindingResult, // Thêm BindingResult
                              @RequestParam("images") List<MultipartFile> images,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes, // Thêm RedirectAttributes
                              Model model) { // Thêm Model
        Long shopId = getAuthenticatedShopId(authentication); 

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentPage", "products");
            model.addAttribute("categories", categoryService.findAll());
            // Xác định lại isEditMode dựa trên ID
            model.addAttribute("isEditMode", productDto.getId() != null); 
            return "vendor/product_add"; // Quay lại form nếu validation lỗi
        }

        try {
            if (productDto.getId() == null) {
                productService.addProduct(productDto, images, shopId);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
            } else {
                Product existingProduct = productService.getProductById(productDto.getId());
                if(existingProduct.getShop().getId().equals(shopId)) {
                     productService.updateProduct(productDto.getId(), productDto, images);
                     redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
                } else {
                     redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền sửa sản phẩm này.");
                }
            }
        } catch (IllegalArgumentException e) { // Bắt lỗi giá sale >= giá gốc
             model.addAttribute("currentPage", "products");
             model.addAttribute("categories", categoryService.findAll());
             model.addAttribute("isEditMode", productDto.getId() != null);
             // Thêm lỗi vào trường salePrice
             bindingResult.rejectValue("salePrice", "error.productDto", e.getMessage()); 
             return "vendor/product_add"; 
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Lưu sản phẩm thất bại: " + e.getMessage());
             // Nếu là edit thì quay lại trang edit, nếu add thì quay lại trang add
             if (productDto.getId() != null) {
                 return "redirect:/vendor/products/edit/" + productDto.getId();
             } else {
                 return "redirect:/vendor/products/add";
             }
        }
        return "redirect:/vendor/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long productId, Authentication authentication, RedirectAttributes redirectAttributes) { 
        Long shopId = getAuthenticatedShopId(authentication);
        try {
            Product product = productService.getProductById(productId);
            if(product.getShop().getId().equals(shopId)) {
                 productService.deleteProduct(productId);
                 redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm thành công!");
            } else {
                 redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa sản phẩm này.");
            }
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Xóa sản phẩm thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/products";
    }

    // --- Quản lý Đơn hàng ---
    @GetMapping("/orders")
    public String orderList(Model model, 
                            @RequestParam("status") Optional<String> status,
                            @RequestParam(defaultValue = "0") int page,
                            Authentication authentication) { 
        model.addAttribute("currentPage", "orders");
        Long shopId = getAuthenticatedShopId(authentication); 
        Pageable pageable = PageRequest.of(page, 10);
        
        Optional<OrderStatus> orderStatus = Optional.empty();
        if (status.isPresent() && !status.get().isEmpty()) {
            try { // Thêm try-catch để xử lý status không hợp lệ
                orderStatus = Optional.of(OrderStatus.valueOf(status.get().toUpperCase()));
            } catch (IllegalArgumentException e) {
                 model.addAttribute("errorMessage", "Trạng thái đơn hàng không hợp lệ.");
                 // Có thể trả về trang lỗi hoặc trang order_list với thông báo
            }
        }
        
        Page<Order> orderPage = orderService.getOrdersByShop(shopId, orderStatus, pageable);
        model.addAttribute("orderPage", orderPage);
        return "vendor/order_list";
    }

    @GetMapping("/orders/details/{id}")
    public String orderDetails(@PathVariable("id") Long orderId, Model model, Authentication authentication) { 
        model.addAttribute("currentPage", "orders");
        Long shopId = getAuthenticatedShopId(authentication); 
        try {
             Order order = orderService.getOrderDetails(orderId, shopId); 
             model.addAttribute("order", order);
             return "vendor/order_details";
        } catch (Exception e) {
             // redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); // Cần RedirectAttributes nếu redirect
             return "redirect:/vendor/orders"; // Hoặc trả về trang lỗi
        }
    }

    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") OrderStatus status,
                                    Authentication authentication, RedirectAttributes redirectAttributes) { // Thêm RedirectAttributes
        Long shopId = getAuthenticatedShopId(authentication); 
        try {
            orderService.updateOrderStatus(orderId, status, shopId); 
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/orders/details/" + orderId;
    }
    
    // --- Quản lý Khuyến mãi ---
    @GetMapping("/promotions")
    public String promotionList(Model model, Authentication authentication) { 
        model.addAttribute("currentPage", "promotions");
        Long shopId = getAuthenticatedShopId(authentication); 
        List<Promotion> promotions = promotionService.getPromotionsByShop(shopId);
        model.addAttribute("promotions", promotions);
        return "vendor/promotion_list";
    }

    @GetMapping("/promotions/add")
    public String addPromotionForm(Model model) {
        model.addAttribute("currentPage", "promotions");
        model.addAttribute("promotionDto", new PromotionDto());
        // === ĐÃ THÊM DÒNG NÀY ===
        model.addAttribute("promotionTypes", promotionTypeService.findAll()); 
        // =======================
        return "vendor/promotion_add";
    }
    
    @PostMapping("/promotions/save")
    public String savePromotion(@Valid @ModelAttribute PromotionDto promotionDto, // Thêm @Valid
                                BindingResult bindingResult, // Thêm BindingResult
                                Authentication authentication, 
                                RedirectAttributes redirectAttributes, // Thêm RedirectAttributes
                                Model model) { // Thêm Model
        Long shopId = getAuthenticatedShopId(authentication); 

        if (bindingResult.hasErrors()) {
             model.addAttribute("currentPage", "promotions");
             model.addAttribute("promotionTypes", promotionTypeService.findAll()); // Gửi lại types nếu lỗi
             return "vendor/promotion_add";
        }
        
        try {
            promotionService.createPromotion(promotionDto, shopId);
             redirectAttributes.addFlashAttribute("successMessage", "Tạo khuyến mãi thành công!");
        } catch (IllegalArgumentException e) { // Bắt lỗi value không hợp lệ
             model.addAttribute("currentPage", "promotions");
             model.addAttribute("promotionTypes", promotionTypeService.findAll());
             // Thêm lỗi vào trường discountValue
             bindingResult.rejectValue("discountValue", "error.promotionDto", e.getMessage()); 
             return "vendor/promotion_add"; 
        } catch (Exception e) { // Bắt lỗi chung (ví dụ: mã code trùng)
             model.addAttribute("currentPage", "promotions");
             model.addAttribute("promotionTypes", promotionTypeService.findAll());
             bindingResult.rejectValue("discountCode", "error.promotionDto", "Mã giảm giá có thể đã tồn tại."); 
             return "vendor/promotion_add"; 
            // redirectAttributes.addFlashAttribute("errorMessage", "Tạo khuyến mãi thất bại: " + e.getMessage()); // Cách cũ
        }
        return "redirect:/vendor/promotions";
    }

    // Thêm phương thức xóa khuyến mãi nếu cần
     @PostMapping("/promotions/delete/{id}")
     public String deletePromotion(@PathVariable("id") Long promotionId, 
                                   Authentication authentication, 
                                   RedirectAttributes redirectAttributes) {
         Long shopId = getAuthenticatedShopId(authentication);
         try {
             promotionService.deletePromotion(promotionId, shopId); // Đã có check bảo mật
             redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khuyến mãi!");
         } catch (Exception e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Xóa thất bại: " + e.getMessage());
         }
         return "redirect:/vendor/promotions";
     }


    // --- Doanh thu ---
    @GetMapping("/revenue")
    public String revenueManagement(Model model, Authentication authentication) { 
        model.addAttribute("currentPage", "revenue");
        return "vendor/revenue";
    }
    
    // --- Quản lý Hồ sơ ---
    @GetMapping("/profile")
    public String showProfileForm(Model model, Authentication authentication) {
        model.addAttribute("currentPage", "profile"); 
        User currentUser = getAuthenticatedUser(authentication);

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName(currentUser.getFullName());
        dto.setEmail(currentUser.getEmail());
        dto.setAddress(currentUser.getAddress());
        dto.setPhoneNumber(currentUser.getPhoneNumber());

        model.addAttribute("profileUpdateDto", dto);
        return "vendor/profile"; 
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute ProfileUpdateDto profileUpdateDto,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model) { 
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentPage", "profile"); 
            return "vendor/profile"; 
        }
        try {
            userService.updateUserProfile(authentication.getName(), profileUpdateDto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.profileUpdateDto", e.getMessage());
            model.addAttribute("currentPage", "profile"); 
            return "vendor/profile"; 
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật hồ sơ thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/profile";
    }
    
    // --- Quản lý Danh mục ---
    @GetMapping("/categories")
    public String categoryManagement(Model model, Authentication authentication) {
        model.addAttribute("currentPage", "categories"); 
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        model.addAttribute("newCategory", new Category()); 
        return "vendor/category_management";
    }

    @PostMapping("/categories/add")
    public String addCategory(@Valid @ModelAttribute("newCategory") Category newCategory,
                              BindingResult bindingResult, 
                              RedirectAttributes redirectAttributes,
                              Model model) { 

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentPage", "categories");
            model.addAttribute("categories", categoryService.findAll()); 
            return "vendor/category_management"; 
        }

        try {
            categoryService.saveCategory(newCategory);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm danh mục '" + newCategory.getName() + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thêm danh mục thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa danh mục thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/categories";
    }

    // --- Quản lý Loại Khuyến mãi ---
    @GetMapping("/promotion-types")
    public String promotionTypeManagement(Model model) { 
        model.addAttribute("currentPage", "promotion-types"); 
        model.addAttribute("promotionTypes", promotionTypeService.findAll());
        model.addAttribute("newPromotionType", new PromotionTypeEntity()); 
        return "vendor/promotion_type_management";
    }

    @PostMapping("/promotion-types/add")
    public String addPromotionType(@Valid @ModelAttribute("newPromotionType") PromotionTypeEntity newType,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentPage", "promotion-types");
            model.addAttribute("promotionTypes", promotionTypeService.findAll());
            return "vendor/promotion_type_management";
        }
        try {
            String formattedCode = newType.getCode().toUpperCase().replaceAll("\\s+", "_").replaceAll("[^A-Z0-9_]+", "");
            newType.setCode(formattedCode); 
            
            promotionTypeService.savePromotionType(newType);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm loại khuyến mãi '" + newType.getName() + "'!");
        } catch (Exception e) { 
             model.addAttribute("currentPage", "promotion-types");
             model.addAttribute("promotionTypes", promotionTypeService.findAll());
             bindingResult.rejectValue("code", "error.newPromotionType", "Mã loại hoặc tên loại có thể đã tồn tại."); 
             return "vendor/promotion_type_management"; 
        }
        return "redirect:/vendor/promotion-types";
    }

    @PostMapping("/promotion-types/delete/{id}")
    public String deletePromotionType(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionTypeService.deletePromotionType(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa loại khuyến mãi!");
        } catch (Exception e) { 
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa thất bại: " + e.getMessage());
        }
        return "redirect:/vendor/promotion-types";
    }

    // --- Báo cáo ---
    @GetMapping("/reports/sales/download")
    public void downloadSalesReport(Authentication authentication, HttpServletResponse response) throws IOException { 
        Long shopId = getAuthenticatedShopId(authentication);
        try (Workbook workbook = reportService.generateSalesReport(shopId)) { 
            String fileName = "BaoCaoBanHang_Shop" + shopId + "_" + java.time.LocalDate.now() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); 
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
             response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
             response.getWriter().write("Lỗi khi tạo báo cáo: " + e.getMessage());
             e.printStackTrace(); 
        }
    }
}