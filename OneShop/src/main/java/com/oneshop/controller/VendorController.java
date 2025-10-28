package com.oneshop.controller;

import com.oneshop.dto.*;
import com.oneshop.entity.*;
import com.oneshop.enums.ShopStatus;
import com.oneshop.repository.UserRepository; // Đảm bảo import UserRepository
import com.oneshop.service.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
// Import AuthenticationPrincipal nếu bạn muốn dùng thay thế Authentication authentication
// import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException; // Import ResponseStatusException

import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set; // Import Set
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('ROLE_VENDOR')") // Chỉ VENDOR mới vào được controller này
public class VendorController {

	private static final Logger logger = LoggerFactory.getLogger(VendorController.class);

	// --- Autowired Services ---
	@Autowired private ProductService productService;
	@Autowired private CategoryService categoryService;
	@Autowired private OrderService orderService;
	@Autowired private ShopService shopService;
	@Autowired private PromotionService promotionService;
	@Autowired private UserService userService;
	@Autowired private PromotionTypeService promotionTypeService;
	@Autowired private ReportService reportService;
	@Autowired private UserRepository userRepository; // Cần để lấy User kèm Shop
	@Autowired private BrandService brandService;

	// --- Helper Methods ---

	/**
	 * Lấy thông tin User đang đăng nhập (bao gồm cả Shop nếu có).
	 * Cần đảm bảo query trong UserRepository tải được Shop (EAGER hoặc JOIN FETCH).
	 */
	private User getAuthenticatedUserEntity(Authentication authentication) {
		String username = authentication.getName();
		// Giả sử findByUsername trả về User với Shop (nếu có)
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin người dùng: " + username));
	}

	/**
	 * Lấy Shop của Vendor đang đăng nhập và kiểm tra trạng thái APPROVED.
	 * Ném lỗi nếu không có shop hoặc shop chưa được duyệt.
	 */
	private Shop getAndValidateVendorShop(Authentication authentication) {
		User user = getAuthenticatedUserEntity(authentication);
		Shop shop = user.getShop(); // Lấy shop từ User đã load

		if (shop == null) {
			logger.error("User {} is VENDOR but has no associated Shop entity!", user.getUsername());
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn chưa được liên kết với gian hàng nào.");
		}

		// Kiểm tra trạng thái shop
		if (shop.getStatus() != ShopStatus.APPROVED) {
			logger.warn("Vendor {} attempting to access vendor area, but shop status is {}", user.getUsername(), shop.getStatus());
			throw new ShopNotApprovedException("Shop của bạn chưa được duyệt hoặc đang ở trạng thái không hợp lệ.", shop.getStatus());
		}
		// Nếu mọi thứ ổn, trả về shop đã được duyệt
		return shop;
	}

	/**
	 * Lấy ID của Shop đã được xác thực và duyệt.
	 */
	private Long getAuthenticatedShopId(Authentication authentication) {
		 return getAndValidateVendorShop(authentication).getId();
	}

	/**
	 * Lấy ID của User đang đăng nhập.
	 */
	private Long getAuthenticatedUserId(Authentication authentication) {
		// Có thể tối ưu bằng cách lấy trực tiếp ID nếu Principal là User object
		return getAuthenticatedUserEntity(authentication).getId();
	}

	// --- Dashboard ---
	@GetMapping("/dashboard")
	public String dashboard(Model model, Authentication authentication) {
		String username = authentication.getName();
		logger.info("Accessing vendor dashboard for user: {}", username);
		model.addAttribute("currentPage", "dashboard");

		try {
			// Sử dụng hàm kiểm tra mới, nếu shop chưa duyệt sẽ ném Exception
			Shop shop = getAndValidateVendorShop(authentication);
			Long shopId = shop.getId();

			// Lấy dữ liệu dashboard
			long newOrderCount = orderService.countNewOrdersByShop(shopId);
			long totalProductCount = productService.countProductsByShop(shopId);
			BigDecimal currentMonthRevenue = orderService.getCurrentMonthRevenueByShop(shopId);

			model.addAttribute("newOrderCount", newOrderCount);
			model.addAttribute("totalProductCount", totalProductCount);
			model.addAttribute("monthlyRevenue", currentMonthRevenue);
			logger.debug("Dashboard data for shopId {}: newOrders={}, totalProducts={}, monthlyRevenue={}", shopId, newOrderCount, totalProductCount, currentMonthRevenue);

			return "vendor/dashboard"; // Trả về view dashboard

		} catch (ShopNotApprovedException e) {
			 // Xử lý khi shop chưa được duyệt (Exception được ném từ getAndValidateVendorShop)
			 logger.warn("Redirecting user {} to pending approval page. Status: {}", username, e.getShopStatus());
			 model.addAttribute("shopStatus", e.getShopStatus());
			 return "vendor/pending_approval"; // Chuyển đến trang thông báo chờ duyệt
		} catch (ResponseStatusException e) {
			 // Xử lý lỗi không có shop hoặc không có quyền
			 logger.error("Access forbidden for user {}: {}", username, e.getReason());
			 model.addAttribute("errorMessage", e.getReason());
			 // Có thể trả về trang lỗi chung thay vì dashboard
			 return "error/403"; // Ví dụ trang lỗi 403 (cần tạo trang này)
		} catch (Exception e) { // Bắt các lỗi không mong muốn khác
			logger.error("Could not load dashboard data for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải dữ liệu dashboard. Vui lòng thử lại sau.");
			// Vẫn trả về dashboard nhưng hiển thị lỗi
			return "vendor/dashboard";
		}
	}

	// --- Quản lý Shop ---
	@GetMapping("/shop")
	public String shopManagement(Model model, Authentication authentication) {
		logger.info("Accessing shop management for user: {}", authentication.getName());
		model.addAttribute("currentPage", "shop");
		try {
			// Không cần kiểm tra duyệt ở đây, chỉ cần lấy thông tin shop hiện tại
			User user = getAuthenticatedUserEntity(authentication);
			Shop shop = user.getShop(); // Lấy shop trực tiếp từ user

			if (shop == null) {
				// Trường hợp này không nên xảy ra với role VENDOR, nhưng kiểm tra cho chắc
				 throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông tin gian hàng.");
			}

			ShopDto dto = new ShopDto();
			dto.setShopName(shop.getName());
			dto.setShopDescription(shop.getDescription());
			dto.setContactEmail(shop.getContactEmail());
			dto.setContactPhone(shop.getContactPhone());
			model.addAttribute("shopDto", dto);
			model.addAttribute("shop", shop); // Vẫn cần shop để hiển thị ảnh cũ và trạng thái
			model.addAttribute("shopStatus", shop.getStatus()); // Truyền trạng thái ra view
			return "vendor/shop_management";
		} catch (ResponseStatusException e) {
		     logger.error("Error accessing shop management for user {}: {}", authentication.getName(), e.getReason());
             model.addAttribute("errorMessage", e.getReason());
             model.addAttribute("shopDto", new ShopDto()); // DTO rỗng
             model.addAttribute("shop", null);
             return "vendor/shop_management"; // Vẫn hiển thị trang nhưng báo lỗi
        } catch (Exception e) {
			logger.error("Unexpected error accessing shop management for user {}: {}", authentication.getName(), e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải thông tin gian hàng: " + e.getMessage());
			model.addAttribute("shopDto", new ShopDto());
			model.addAttribute("shop", null);
			return "vendor/shop_management";
		}
	}

	@PostMapping("/shop/update")
	public String updateShop(@Valid @ModelAttribute ShopDto shopDto, BindingResult bindingResult,
			@RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
			@RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile,
			RedirectAttributes redirectAttributes, Authentication authentication, Model model) {
		String username = authentication.getName();
		logger.info("Attempting to update shop for user: {}", username);
		model.addAttribute("currentPage", "shop");
		Shop currentShop = null; // Biến để lưu shop hiện tại

		try {
			// Lấy shop hiện tại (không cần check duyệt vì đang ở trang quản lý shop)
			User user = getAuthenticatedUserEntity(authentication);
			currentShop = user.getShop();
			if (currentShop == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy gian hàng để cập nhật.");
			}
			model.addAttribute("shop", currentShop); // Add shop vào model để dùng nếu có lỗi
			model.addAttribute("shopStatus", currentShop.getStatus()); // Add status

			if (bindingResult.hasErrors()) {
				logger.warn("Validation errors updating shop for user {}", username);
				// model đã có shop và shopDto
				return "vendor/shop_management"; // Quay lại form với lỗi
			}

			// Gọi service để cập nhật
			shopService.updateShop(currentShop.getId(), shopDto, logoFile, bannerFile);
			logger.info("Shop updated successfully for user: {}", username);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin gian hàng thành công!");
			return "redirect:/vendor/shop";

		} catch (ResponseStatusException e) {
             logger.error("Error updating shop for user {}: {}", username, e.getReason());
             redirectAttributes.addFlashAttribute("errorMessage", e.getReason());
             return "redirect:/vendor/shop"; // Chuyển hướng về trang shop với lỗi
        } catch (Exception e) {
			logger.error("Error updating shop for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
			model.addAttribute("shopDto", shopDto); // Giữ lại DTO đã nhập
			// model đã có shop (nếu load được ở trên)
			return "vendor/shop_management"; // Quay lại form với lỗi
		}
	}

	// --- Quản lý Sản phẩm ---
	@GetMapping("/products")
	public String productList(Model model, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, Authentication authentication) {
		String username = authentication.getName();
		logger.debug("Listing products for user: {}, page: {}, size: {}", username, page, size);
		model.addAttribute("currentPage", "products");
		try {
			// getAuthenticatedShopId sẽ kiểm tra shop đã duyệt chưa
			Long shopId = getAuthenticatedShopId(authentication);
			Pageable pageable = PageRequest.of(page, size, Sort.by("productId").descending());
			Page<Product> productPage = productService.getProductsByShop(shopId, pageable);
			model.addAttribute("productPage", productPage);
			return "vendor/product_list";
        } catch (ShopNotApprovedException e) {
             logger.warn("Redirecting user {} accessing products to pending page. Status: {}", username, e.getShopStatus());
             model.addAttribute("shopStatus", e.getShopStatus());
             return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error listing products for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải danh sách sản phẩm.");
			model.addAttribute("productPage", Page.empty(PageRequest.of(page, size)));
			return "vendor/product_list"; // Vẫn trả về trang list nhưng báo lỗi
		}
	}

	// --- HIỂN THỊ FORM THÊM SẢN PHẨM ---
	@GetMapping("/products/add")
	public String addProductForm(Model model, Authentication authentication) {
		String username = authentication.getName();
		logger.debug("Showing add product form for user: {}", username);
		model.addAttribute("currentPage", "products");
		try {
			// Kiểm tra shop đã duyệt chưa trước khi cho vào form add
			getAndValidateVendorShop(authentication);

			ProductDto productDto = new ProductDto();
			if (productDto.getVariants().isEmpty()) { // Đảm bảo luôn có ít nhất 1 variant
			    productDto.getVariants().add(new VariantDto());
			}
			model.addAttribute("productDto", productDto);
			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("brands", brandService.findAll());
			model.addAttribute("isEditMode", false);
			return "vendor/product_add";
        } catch (ShopNotApprovedException e) {
             logger.warn("Redirecting user {} accessing add product form to pending page. Status: {}", username, e.getShopStatus());
             model.addAttribute("shopStatus", e.getShopStatus());
             return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error preparing add product form for user {}: {}", username, e.getMessage(), e);
			// Chuyển hướng về trang danh sách sản phẩm với thông báo lỗi
			RedirectAttributes redirectAttributes = (RedirectAttributes) model.asMap().get("redirectAttributes");
			if (redirectAttributes != null) {
			    redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở trang thêm sản phẩm: " + e.getMessage());
			}
			return "redirect:/vendor/products";
		}
	}

	// --- HIỂN THỊ FORM SỬA SẢN PHẨM ---
	@GetMapping("/products/edit/{id}")
	public String editProductForm(@PathVariable("id") Long productId, Model model, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		String username = authentication.getName();
		logger.debug("Showing edit product form for productId: {} for user: {}", productId, username);
		model.addAttribute("currentPage", "products");
		try {
			// Kiểm tra shop duyệt và lấy shopId
			Long shopId = getAuthenticatedShopId(authentication);

			Optional<Product> productOpt = productService.getProductByIdForVendor(productId, shopId);
			if (productOpt.isEmpty()) {
				redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm không tồn tại hoặc bạn không có quyền sửa.");
				return "redirect:/vendor/products";
			}
			Product product = productOpt.get();
			ProductDto dto = mapProductToDto(product);
			model.addAttribute("productDto", dto);
			model.addAttribute("categories", categoryService.findAll());
			model.addAttribute("brands", brandService.findAll());
			model.addAttribute("isEditMode", true);
			return "vendor/product_add";
        } catch (ShopNotApprovedException e) {
             logger.warn("Redirecting user {} accessing edit product form to pending page. Status: {}", username, e.getShopStatus());
             model.addAttribute("shopStatus", e.getShopStatus());
             return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error loading edit product form for productId {}: {}", productId, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Không thể tải thông tin sản phẩm để sửa.");
			return "redirect:/vendor/products";
		}
	}

	// --- HELPER MAP ENTITY SANG DTO (Sửa lại để map ảnh biến thể) ---
	private ProductDto mapProductToDto(Product product) {
		ProductDto dto = new ProductDto();
		dto.setId(product.getProductId());
		dto.setProductName(product.getName());
		dto.setProductDescription(product.getDescription());
		dto.setProductTags(product.getTags());
		if (product.getCategory() != null) dto.setCategoryId(product.getCategory().getId());
		if (product.getBrand() != null) dto.setBrandId(product.getBrand().getBrandId());

		// Map ảnh chung
		if (product.getImages() != null) {
			dto.setExistingImageUrls(
					product.getImages().stream()
                        .map(ProductImage::getImageUrl)
                        .filter(StringUtils::hasText) // Lọc bỏ URL rỗng
                        .collect(Collectors.toList()));
		} else {
            dto.setExistingImageUrls(new ArrayList<>());
        }

		// Map danh sách biến thể
		if (product.getVariants() != null) {
			dto.setVariants(product.getVariants().stream().map(variant -> {
				VariantDto variantDto = new VariantDto();
				variantDto.setVariantId(variant.getVariantId());
				variantDto.setName(variant.getName());
				variantDto.setSku(variant.getSku());
				variantDto.setPrice(variant.getPrice());
				variantDto.setOriginalPrice(variant.getOriginalPrice());
				variantDto.setStock(variant.getStock());
                variantDto.setExistingImageUrl(variant.getImageUrl()); // Map ảnh của variant
				return variantDto;
			}).collect(Collectors.toList()));
		} else {
             dto.setVariants(new ArrayList<>());
        }
        // Nếu list rỗng sau khi map, thêm 1 variant rỗng
        if (dto.getVariants().isEmpty()) {
            dto.getVariants().add(new VariantDto());
        }

		return dto;
	}

	// --- XỬ LÝ LƯU (THÊM/SỬA) SẢN PHẨM ---
	@PostMapping("/products/save")
	public String saveProduct(@Valid @ModelAttribute ProductDto productDto,
							  BindingResult bindingResult,
							  @RequestParam(value = "images", required = false) List<MultipartFile> images,
							  Authentication authentication,
							  RedirectAttributes redirectAttributes,
							  Model model) {
		String username = authentication.getName();
		boolean isEditMode = productDto.getId() != null;
		logger.info("Saving product (edit={}) for user: {}", isEditMode, username);
		model.addAttribute("currentPage", "products");

		Long shopId = null; // Khai báo shopId ở đây

		try {
            // Kiểm tra shop đã duyệt chưa trước khi lưu
            shopId = getAuthenticatedShopId(authentication);

			// --- VALIDATION NGHIỆP VỤ ---
			if (productDto.getBrandId() != null && StringUtils.hasText(productDto.getNewBrandName())) {
				bindingResult.rejectValue("newBrandName", "BrandConflict", "Chỉ chọn thương hiệu có sẵn hoặc nhập tên thương hiệu mới.");
			}
			if (productDto.getVariants() != null && !productDto.getVariants().isEmpty()) {
				for (int i = 0; i < productDto.getVariants().size(); i++) {
					VariantDto variant = productDto.getVariants().get(i);
					if (variant.getOriginalPrice() != null && variant.getPrice() != null &&
						variant.getOriginalPrice().compareTo(variant.getPrice()) < 0) { // Sửa: <= thành <
						bindingResult.rejectValue("variants[" + i + "].originalPrice", "PriceError", "Giá gốc phải lớn hơn hoặc bằng giá bán.");
					}
					// Kiểm tra tên biến thể không trống
                    if (!StringUtils.hasText(variant.getName())) {
                        bindingResult.rejectValue("variants[" + i + "].name", "NotBlank", "Tên loại không được để trống.");
                    }
				}
                // Kiểm tra tên biến thể trùng lặp
                Set<String> variantNames = productDto.getVariants().stream()
                                            .map(v -> v.getName().trim().toLowerCase())
                                            .collect(Collectors.toSet());
                if (variantNames.size() != productDto.getVariants().size()) {
                     bindingResult.reject("variants", "Tên các loại sản phẩm (biến thể) không được trùng nhau.");
                }
			} else {
				bindingResult.reject("variants", "Sản phẩm phải có ít nhất một loại (biến thể).");
			}

			// Kiểm tra lỗi validation (@Valid và lỗi nghiệp vụ)
			if (bindingResult.hasErrors()) {
				logger.warn("Validation errors saving product for user {}", username);
				throw new ValidationException("Dữ liệu sản phẩm không hợp lệ."); // Ném lỗi để vào catch block
			}

			// --- Gọi Service để lưu ---
			if (!isEditMode) {
				productService.addProduct(productDto, images, shopId);
				redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
			} else {
				productService.updateProduct(productDto.getId(), productDto, images, shopId);
				redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
			}
			return "redirect:/vendor/products";

        } catch (ShopNotApprovedException e) {
             logger.warn("Redirecting user {} saving product to pending page. Status: {}", username, e.getShopStatus());
             // Không cần addAttribute vì sẽ chuyển hướng ngay
             // model.addAttribute("shopStatus", e.getShopStatus());
             return "vendor/pending_approval"; // Chuyển hướng đến trang chờ duyệt
        } catch (ValidationException | IllegalArgumentException | EntityNotFoundException e) {
			// Bắt lỗi validation hoặc lỗi nghiệp vụ từ service
			logger.warn("Validation or business logic error during product save by user {}: {}", username, e.getMessage());
			// Load lại categories và brands cho form
			try {
				model.addAttribute("categories", categoryService.findAll());
				model.addAttribute("brands", brandService.findAll());
			} catch (Exception loadEx) {
				model.addAttribute("categories", Collections.emptyList());
				model.addAttribute("brands", Collections.emptyList());
				model.addAttribute("errorMessage", "Lỗi tải lại danh mục/thương hiệu.");
			}
			model.addAttribute("isEditMode", isEditMode);
			// productDto đã có trong model từ @ModelAttribute
            // Nếu lỗi không phải do bindingResult, thêm lỗi chung
            if (!(e instanceof ValidationException)) {
                 model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            }

            // Load lại ảnh cũ nếu là edit mode
			if (isEditMode && productDto.getId() != null && shopId != null) { // Thêm kiểm tra shopId != null
				try {
					productService.getProductByIdForVendor(productDto.getId(), shopId)
						.ifPresent(p -> {
							ProductDto existingDto = mapProductToDto(p); // Map lại để lấy ảnh variant
                            productDto.setExistingImageUrls(existingDto.getExistingImageUrls());
                            // Cập nhật lại existingImageUrl cho từng variant trong DTO hiện tại
                            for(int i = 0; i < productDto.getVariants().size(); i++){
                                VariantDto currentVarDto = productDto.getVariants().get(i);
                                if(currentVarDto.getVariantId() != null){
                                     existingDto.getVariants().stream()
                                         .filter(v -> v.getVariantId() != null && v.getVariantId().equals(currentVarDto.getVariantId()))
                                         .findFirst()
                                         .ifPresent(existingVarDto -> currentVarDto.setExistingImageUrl(existingVarDto.getExistingImageUrl()));
                                }
                            }
						});
				} catch (Exception imgEx) {
					logger.error("Error refetching product images/variants on save error", imgEx);
                    productDto.setExistingImageUrls(new ArrayList<>());
                    productDto.getVariants().forEach(v -> v.setExistingImageUrl(null));
				}
			} else {
                 productDto.setExistingImageUrls(new ArrayList<>());
                 productDto.getVariants().forEach(v -> v.setExistingImageUrl(null));
            }

			return "vendor/product_add"; // Quay lại form với lỗi
		} catch (Exception e) {
			// Bắt lỗi hệ thống khác
			logger.error("Error saving product for user {}: {}", username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Lưu sản phẩm thất bại: " + e.getMessage());
			return isEditMode ? ("redirect:/vendor/products/edit/" + productDto.getId()) : "redirect:/vendor/products/add";
		}
	}

	// --- XÓA SẢN PHẨM ---
	@PostMapping("/products/delete/{id}")
	public String deleteProduct(@PathVariable("id") Long productId, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		String username = authentication.getName();
		logger.warn("Attempting to delete product {} by user {}", productId, username);
		try {
			// Kiểm tra shop duyệt trước khi xóa
			Long shopId = getAuthenticatedShopId(authentication);
			productService.deleteProduct(productId, shopId);
			logger.info("Product {} deleted successfully by user {}", productId, username);
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm thành công!");
        } catch (ShopNotApprovedException e) {
             logger.warn("User {} cannot delete product, shop not approved. Status: {}", username, e.getShopStatus());
             redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm khi shop chưa được duyệt.");
             // Chuyển hướng về trang sản phẩm (sẽ lại bị chặn bởi check ở GET /products)
             // Hoặc chuyển về trang pending
             return "redirect:/vendor/dashboard"; // Chuyển về dashboard (sẽ lại bị check)
		} catch (Exception e) {
			logger.error("Error deleting product {} by user {}: {}", productId, username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Xóa sản phẩm thất bại: " + e.getMessage());
		}
		return "redirect:/vendor/products";
	}

	// --- Quản lý Đơn hàng ---
	@GetMapping("/orders")
	public String orderList(Model model, @RequestParam(name = "status", required = false) String statusString,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			Authentication authentication) {
		String username = authentication.getName();
		logger.debug("Listing orders for user: {}, status: {}, page: {}, size: {}", username, statusString, page, size);
		model.addAttribute("currentPage", "orders");
		try {
			// Kiểm tra shop duyệt
			Long shopId = getAuthenticatedShopId(authentication);

			Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
			Optional<OrderStatus> orderStatusOptional = Optional.empty();
			if (statusString != null && !statusString.trim().isEmpty()) {
				try {
					orderStatusOptional = Optional.of(OrderStatus.valueOf(statusString.trim().toUpperCase()));
				} catch (IllegalArgumentException e) {
					model.addAttribute("errorMessage", "Trạng thái đơn hàng không hợp lệ: " + statusString);
				}
			}
			Page<Order> orderPage = orderService.getOrdersByShop(shopId, orderStatusOptional, pageable);
			model.addAttribute("orderPage", orderPage);
			model.addAttribute("currentStatus", statusString);
			return "vendor/order_list";
        } catch (ShopNotApprovedException e) {
             logger.warn("Redirecting user {} accessing orders to pending page. Status: {}", username, e.getShopStatus());
             model.addAttribute("shopStatus", e.getShopStatus());
             return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error listing orders for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải danh sách đơn hàng.");
			model.addAttribute("orderPage", Page.empty(PageRequest.of(page, size)));
			return "vendor/order_list";
		}
	}

	@GetMapping("/orders/details/{id}")
	public String orderDetails(@PathVariable("id") Long orderId, Model model, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		String username = authentication.getName();
		logger.debug("Viewing order details for orderId: {} by user: {}", orderId, username);
		model.addAttribute("currentPage", "orders");
		try {
			// Kiểm tra shop duyệt
			Long shopId = getAuthenticatedShopId(authentication);

			Order order = orderService.getOrderDetails(orderId, shopId);
			model.addAttribute("order", order);
			return "vendor/order_details";
        } catch (ShopNotApprovedException e) {
             logger.warn("Redirecting user {} accessing order details to pending page. Status: {}", username, e.getShopStatus());
             model.addAttribute("shopStatus", e.getShopStatus());
             return "vendor/pending_approval";
		} catch (SecurityException e) {
			logger.warn("Access denied for user {} on orderId {}: {}", username, orderId, e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn hàng này.");
			return "redirect:/vendor/orders";
		} catch (RuntimeException e) {
			logger.error("Error viewing order details for orderId {} by user {}: {}", orderId, username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng hoặc đã xảy ra lỗi.");
			return "redirect:/vendor/orders";
		}
	}

	@PostMapping("/orders/update-status")
	public String updateOrderStatus(@RequestParam("orderId") Long orderId,
			@RequestParam("status") String statusString,
			Authentication authentication, RedirectAttributes redirectAttributes) {
		String username = authentication.getName();
		logger.info("User {} attempting to update orderId {} to status {}", username, orderId, statusString);
		OrderStatus newStatus;
		try {
            newStatus = OrderStatus.valueOf(statusString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value received: {}", statusString);
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái cập nhật không hợp lệ.");
            return "redirect:/vendor/orders/details/" + orderId;
        }

		try {
			// Kiểm tra shop duyệt
			Long shopId = getAuthenticatedShopId(authentication);

			orderService.updateOrderStatus(orderId, newStatus, shopId);
			logger.info("Order {} status updated to {} by user {}", orderId, newStatus, username);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (ShopNotApprovedException e) {
             logger.warn("User {} cannot update order status, shop not approved. Status: {}", username, e.getShopStatus());
             redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái khi shop chưa được duyệt.");
             // Chuyển hướng về trang chi tiết (sẽ lại bị chặn bởi check ở GET /orders/details)
             return "redirect:/vendor/dashboard"; // Hoặc về dashboard
		} catch (SecurityException e) {
			logger.warn("Access denied for user {} trying to update orderId {}: {}", username, orderId, e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật đơn hàng này.");
		} catch (IllegalStateException | IllegalArgumentException e) {
			logger.warn("Failed to update status for orderId {} by user {}: {}", orderId, username, e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật trạng thái thất bại: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Error updating status for orderId {} by user {}: {}", orderId, username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi cập nhật trạng thái.");
		}
		// Luôn chuyển về trang chi tiết đơn hàng để xem kết quả hoặc lỗi
		return "redirect:/vendor/orders/details/" + orderId;
	}

	// --- Quản lý Khuyến mãi ---
    // (Tương tự, thêm check shop duyệt ở các mapping GET và POST)
	@GetMapping("/promotions")
	public String promotionList(Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "promotions");
		try {
            Long shopId = getAuthenticatedShopId(authentication);
		    List<Promotion> promotions = promotionService.getPromotionsByShop(shopId);
		    model.addAttribute("promotions", promotions);
		    return "vendor/promotion_list";
        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
            logger.error("Error listing promotions for user {}: {}", username, e.getMessage());
            model.addAttribute("errorMessage", "Không thể tải danh sách khuyến mãi.");
            model.addAttribute("promotions", Collections.emptyList());
            return "vendor/promotion_list";
        }
	}

	@GetMapping("/promotions/add")
	public String addPromotionForm(Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "promotions");
        try {
            getAndValidateVendorShop(authentication); // Check shop duyệt
		    model.addAttribute("promotionDto", new PromotionDto());
		    model.addAttribute("promotionTypes", promotionTypeService.findAll());
            model.addAttribute("isEditMode", false); // Thêm cờ cho biết là form add
		    return "vendor/promotion_add"; // Trả về view add/edit
        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (Exception e) {
             logger.error("Error preparing add promotion form for user {}: {}", username, e.getMessage());
             RedirectAttributes redirectAttributes = (RedirectAttributes) model.asMap().get("redirectAttributes");
             if (redirectAttributes != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở trang thêm khuyến mãi: " + e.getMessage());
             }
             return "redirect:/vendor/promotions";
        }
	}

    @GetMapping("/promotions/edit/{id}")
    public String editPromotionForm(@PathVariable("id") Long promotionId, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        logger.debug("Showing edit promotion form for ID: {} by user: {}", promotionId, username);
        model.addAttribute("currentPage", "promotions");
        try {
            Long shopId = getAuthenticatedShopId(authentication); // Check duyệt và lấy shopId

            // Lấy thông tin promotion cần sửa
            Promotion promotion = promotionService.getPromotionByIdAndShopId(promotionId, shopId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khuyến mãi hoặc bạn không có quyền sửa."));

            // Chuyển entity sang DTO để hiển thị trên form
            PromotionDto dto = mapPromotionToDto(promotion); // Cần tạo hàm map này

            model.addAttribute("promotionDto", dto);
            model.addAttribute("promotionTypes", promotionTypeService.findAll());
            model.addAttribute("isEditMode", true); // Thêm cờ cho biết là form edit
            return "vendor/promotion_add"; // Dùng lại view add/edit

        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
        } catch (EntityNotFoundException e) {
            logger.warn("Promotion edit failed for user {}: {}", username, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vendor/promotions";
        } catch (Exception e) {
            logger.error("Error loading edit promotion form for ID {} by user {}: {}", promotionId, username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tải thông tin khuyến mãi để sửa.");
            return "redirect:/vendor/promotions";
        }
    }

	@PostMapping("/promotions/save") // Giữ nguyên URL này cho cả thêm và sửa
	public String saveOrUpdatePromotion(@Valid @ModelAttribute("promotionDto") PromotionDto promotionDto,
                                        BindingResult bindingResult,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        String username = authentication.getName();
        boolean isEditMode = promotionDto.getId() != null; // Kiểm tra xem có ID không để biết là sửa hay thêm
		model.addAttribute("currentPage", "promotions"); // Giữ currentPage

		Long shopId = null;
		try {
            shopId = getAuthenticatedShopId(authentication); // Check shop duyệt

			// Kiểm tra validation cơ bản (@Valid)
            if (bindingResult.hasErrors()) {
                 logger.warn("Validation errors {} promotion for user {}", isEditMode ? "updating" : "saving", username);
                 throw new ValidationException("Dữ liệu khuyến mãi không hợp lệ.");
			}

            // Gọi service tương ứng
            if (isEditMode) {
                logger.info("Attempting to update promotion ID: {} for user {}", promotionDto.getId(), username);
                promotionService.updatePromotion(promotionDto.getId(), promotionDto, shopId);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khuyến mãi thành công!");
            } else {
                logger.info("Attempting to create new promotion for user {}", username);
                promotionService.createPromotion(promotionDto, shopId);
                redirectAttributes.addFlashAttribute("successMessage", "Tạo khuyến mãi thành công!");
            }
			return "redirect:/vendor/promotions"; // Về trang danh sách

        } catch (ShopNotApprovedException e) {
            return "vendor/pending_approval";
        } catch (ValidationException | IllegalArgumentException | EntityNotFoundException e) {
            // Lỗi validation (@Valid hoặc từ service) hoặc không tìm thấy entity khi sửa
			logger.warn("Error {} promotion for user {}: {}", isEditMode ? "updating" : "saving", username, e.getMessage());
            try {
                model.addAttribute("promotionTypes", promotionTypeService.findAll()); // Load lại types
            } catch (Exception loadEx) {
                model.addAttribute("promotionTypes", Collections.emptyList());
                model.addAttribute("errorMessage", "Lỗi tải lại loại khuyến mãi.");
            }
            // Nếu lỗi không phải do bindingResult (ví dụ lỗi nghiệp vụ từ service)
            if (!(e instanceof ValidationException) && !bindingResult.hasErrors()) {
                 model.addAttribute("errorMessage", e.getMessage());
            }
            model.addAttribute("isEditMode", isEditMode); // Đặt lại cờ edit
            // promotionDto đã có trong model
			return "vendor/promotion_add"; // Quay lại form
		} catch (Exception e) { // Lỗi hệ thống khác
             logger.error("Error {} promotion for user {}: {}", isEditMode ? "updating" : "saving", username, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("errorMessage", (isEditMode ? "Cập nhật" : "Tạo") + " khuyến mãi thất bại: " + e.getMessage());
			 return "redirect:/vendor/promotions"; // Về trang danh sách với lỗi
		}
	}


	@PostMapping("/promotions/delete/{id}")
	public String deletePromotion(@PathVariable("id") Long promotionId, Authentication authentication,
			RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
		try {
            Long shopId = getAuthenticatedShopId(authentication); // Check shop duyệt
			promotionService.deletePromotion(promotionId, shopId);
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khuyến mãi!");
        } catch (ShopNotApprovedException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa khuyến mãi khi shop chưa được duyệt.");
             return "redirect:/vendor/dashboard";
		} catch (Exception e) {
            logger.error("Error deleting promotion {} for user {}: {}", promotionId, username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Xóa thất bại: " + e.getMessage());
		}
		return "redirect:/vendor/promotions";
	}

    // --- HELPER MAP ENTITY SANG DTO CHO FORM SỬA ---
    private PromotionDto mapPromotionToDto(Promotion promotion) {
        PromotionDto dto = new PromotionDto();
        dto.setId(promotion.getId());
        dto.setCampaignName(promotion.getCampaignName());
        dto.setDiscountCode(promotion.getDiscountCode());
        if (promotion.getType() != null) {
            dto.setPromotionTypeId(promotion.getType().getId());
        }
        dto.setDiscountValue(promotion.getValue());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        return dto;
    }


	// --- Doanh thu ---
    // (Tương tự, thêm check shop duyệt)
	@GetMapping("/revenue")
	public String revenueManagement(Model model, Authentication authentication) {
		model.addAttribute("currentPage", "revenue");
		String username = authentication.getName();
		logger.info("Accessing revenue management for user: {}", username);
		try {
			Long shopId = getAuthenticatedShopId(authentication); // Check shop duyệt

			BigDecimal totalRevenue = orderService.getTotalRevenueByShop(shopId);
			BigDecimal currentMonthRevenue = orderService.getCurrentMonthRevenueByShop(shopId);
			long totalDeliveredOrders = orderService.countDeliveredOrdersByShop(shopId);
			Map<String, BigDecimal> monthlyData = orderService.getMonthlyRevenueData(shopId, 6);

            Map<String, BigDecimal> chartLabelsAndData = new LinkedHashMap<>();
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
            monthlyData.forEach((key, value) -> {
                try {
                    String formattedLabel = YearMonth.parse(key, inputFormatter).format(outputFormatter);
                    chartLabelsAndData.put(formattedLabel, value);
                } catch (Exception e) {
                     chartLabelsAndData.put(key, value);
                }
            });

			model.addAttribute("totalRevenue", totalRevenue);
			model.addAttribute("currentMonthRevenue", currentMonthRevenue);
			model.addAttribute("totalDeliveredOrders", totalDeliveredOrders);
            model.addAttribute("chartLabels", chartLabelsAndData.keySet());
            model.addAttribute("chartData", chartLabelsAndData.values());
            logger.debug("Revenue data loaded successfully for shopId {}", shopId);
            return "vendor/revenue";

        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error loading revenue data for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải dữ liệu doanh thu: " + e.getMessage());
			model.addAttribute("totalRevenue", BigDecimal.ZERO);
			model.addAttribute("currentMonthRevenue", BigDecimal.ZERO);
			model.addAttribute("totalDeliveredOrders", 0L);
            model.addAttribute("chartLabels", List.of());
            model.addAttribute("chartData", List.of());
            return "vendor/revenue"; // Vẫn hiển thị trang nhưng báo lỗi
		}
	}

	// --- Quản lý Hồ sơ ---
    // (Không cần check shop duyệt vì đây là thông tin user)
	@GetMapping("/profile")
	public String showProfileForm(Model model, Authentication authentication) {
		model.addAttribute("currentPage", "profile");
		User currentUser = getAuthenticatedUserEntity(authentication); // Chỉ cần lấy user
		ProfileUpdateDto dto = new ProfileUpdateDto();
		dto.setFullName(currentUser.getFullName());
		dto.setEmail(currentUser.getEmail());
		dto.setAddress(currentUser.getAddress());
		dto.setPhoneNumber(currentUser.getPhoneNumber());
		model.addAttribute("profileUpdateDto", dto);
		return "vendor/profile";
	}

	@PostMapping("/profile/update")
	public String updateProfile(@Valid @ModelAttribute ProfileUpdateDto profileUpdateDto, BindingResult bindingResult,
			Authentication authentication, RedirectAttributes redirectAttributes, Model model) {
		model.addAttribute("currentPage", "profile");
		if (bindingResult.hasErrors()) {
			return "vendor/profile";
		}
		String username = authentication.getName();
		try {
			userService.updateUserProfile(username, profileUpdateDto);
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
			return "redirect:/vendor/profile";
		} catch (IllegalArgumentException e) {
			bindingResult.rejectValue("email", "Unique", e.getMessage());
			model.addAttribute("profileUpdateDto", profileUpdateDto);
			return "vendor/profile";
		} catch (Exception e) {
			logger.error("Error updating profile for user {}: {}", username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật hồ sơ thất bại: " + e.getMessage());
			return "redirect:/vendor/profile";
		}
	}

	// --- Quản lý Danh mục & Thương hiệu ---
    // (Thêm check shop duyệt)
	@GetMapping("/categories")
	public String categoryManagement(Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "categories");
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt
			List<Category> categories = categoryService.findAll();
			model.addAttribute("categories", categories);
            model.addAttribute("newCategory", new Category());
            return "vendor/category_management";
        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error fetching categories for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải danh mục.");
            model.addAttribute("categories", Collections.emptyList());
            model.addAttribute("newCategory", new Category());
            return "vendor/category_management";
		}
	}

	@PostMapping("/categories/add")
	public String addCategory(@Valid @ModelAttribute("newCategory") Category newCategory, BindingResult bindingResult,
			RedirectAttributes redirectAttributes, Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "categories");
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt

			if (bindingResult.hasErrors()) {
                 throw new ValidationException("Dữ liệu danh mục không hợp lệ.");
			}
			categoryService.saveCategory(newCategory);
			redirectAttributes.addFlashAttribute("successMessage", "Đã thêm danh mục '" + newCategory.getName() + "' thành công!");
			return "redirect:/vendor/categories";

        } catch (ShopNotApprovedException e) {
            return "vendor/pending_approval";
        } catch (ValidationException | IllegalArgumentException e) {
             logger.warn("Error adding category for user {}: {}", username, e.getMessage());
			 try { model.addAttribute("categories", categoryService.findAll()); } catch (Exception catEx) { /* Ignore */ }
             if (!(e instanceof ValidationException)) {
                 bindingResult.rejectValue("name", "Unique", e.getMessage());
             }
			 // newCategory đã có trong model
			 return "vendor/category_management";
		} catch (Exception e) {
			logger.error("Unexpected error adding category for user {}: {}", username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Thêm danh mục thất bại: " + e.getMessage());
			return "redirect:/vendor/categories";
		}
	}

	@PostMapping("/categories/delete/{id}")
	public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        String username = authentication.getName();
		logger.warn("Attempting to delete category ID: {} by user {}", id, username);
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt
			categoryService.deleteCategory(id);
			logger.info("Category {} deleted by user {}.", id, username);
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục thành công!");
        } catch (ShopNotApprovedException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa danh mục khi shop chưa được duyệt.");
             return "redirect:/vendor/dashboard";
		} catch (RuntimeException e) {
			logger.error("Error deleting category {} by user {}: {}", id, username, e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Xóa danh mục thất bại: " + e.getMessage());
		}
		return "redirect:/vendor/categories";
	}

	@GetMapping("/brands")
	public String brandManagement(Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "brands");
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt
			List<Brand> brands = brandService.findAll();
			model.addAttribute("brands", brands);
            model.addAttribute("newBrand", new Brand());
            return "vendor/brand_management";
        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error fetching brands for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải danh sách thương hiệu.");
            model.addAttribute("brands", Collections.emptyList());
            model.addAttribute("newBrand", new Brand());
			return "vendor/brand_management";
		}
	}

	@PostMapping("/brands/add")
	public String addBrand(@Valid @ModelAttribute("newBrand") Brand newBrand, BindingResult bindingResult,
						   RedirectAttributes redirectAttributes, Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "brands");
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt

			if (bindingResult.hasErrors()) {
                throw new ValidationException("Dữ liệu thương hiệu không hợp lệ.");
			}
			brandService.saveBrand(newBrand);
			redirectAttributes.addFlashAttribute("successMessage", "Đã thêm thương hiệu '" + newBrand.getName() + "' thành công!");
			return "redirect:/vendor/brands";

        } catch (ShopNotApprovedException e) {
            return "vendor/pending_approval";
        } catch (ValidationException | IllegalArgumentException e) {
			 logger.warn("Error adding brand for user {}: {}", username, e.getMessage());
             try { model.addAttribute("brands", brandService.findAll()); } catch (Exception brandEx) { /* Ignore */ }
             if (!(e instanceof ValidationException)) {
                bindingResult.rejectValue("name", "Unique", e.getMessage());
             }
			 // newBrand đã có trong model
			 return "vendor/brand_management";
		} catch (Exception e) {
			logger.error("Unexpected error adding brand for user {}: {}", username, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage", "Thêm thương hiệu thất bại: " + e.getMessage());
			return "redirect:/vendor/brands";
		}
	}

	@PostMapping("/brands/delete/{id}")
	public String deleteBrand(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        String username = authentication.getName();
		logger.warn("Attempting to delete brand ID: {} by user {}", id, username);
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt
			brandService.deleteBrand(id);
			logger.info("Brand {} deleted by user {}.", id, username);
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thương hiệu thành công!");
        } catch (ShopNotApprovedException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa thương hiệu khi shop chưa được duyệt.");
             return "redirect:/vendor/dashboard";
		} catch (RuntimeException e) {
			logger.error("Error deleting brand {} by user {}: {}", id, username, e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Xóa thương hiệu thất bại: " + e.getMessage());
		}
		return "redirect:/vendor/brands";
	}

	// --- Quản lý Loại Khuyến mãi ---
    // (Thêm check shop duyệt)
	@GetMapping("/promotion-types")
	public String promotionTypeManagement(Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "promotion-types");
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt
			model.addAttribute("promotionTypes", promotionTypeService.findAll());
            model.addAttribute("newPromotionType", new PromotionTypeEntity());
            return "vendor/promotion_type_management";
        } catch (ShopNotApprovedException e) {
            model.addAttribute("shopStatus", e.getShopStatus());
            return "vendor/pending_approval";
		} catch (Exception e) {
			logger.error("Error fetching promotion types for user {}: {}", username, e.getMessage(), e);
			model.addAttribute("errorMessage", "Không thể tải loại khuyến mãi.");
            model.addAttribute("promotionTypes", Collections.emptyList());
            model.addAttribute("newPromotionType", new PromotionTypeEntity());
			return "vendor/promotion_type_management";
		}
	}

	@PostMapping("/promotion-types/add")
	public String addPromotionType(@Valid @ModelAttribute("newPromotionType") PromotionTypeEntity newType,
			BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model, Authentication authentication) {
        String username = authentication.getName();
		model.addAttribute("currentPage", "promotion-types");
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt

			if (bindingResult.hasErrors()) {
                 throw new ValidationException("Dữ liệu loại khuyến mãi không hợp lệ.");
			}

            // === XÓA ĐOẠN CODE ĐỊNH DẠNG CODE ===
			// String formattedCode = newType.getCode().trim().toUpperCase().replaceAll("\\s+", "_")
			// 		.replaceAll("[^A-Z0-9_]+", "");
			// if (formattedCode.isEmpty()) {
			// 	bindingResult.rejectValue("code", "Invalid", "Mã loại không hợp lệ (chỉ chữ cái, số, gạch dưới).");
			// 	throw new ValidationException("Mã loại không hợp lệ.");
			// }
			// newType.setCode(formattedCode);
            // ===================================

            // Kiểm tra xem code có rỗng không (vẫn cần thiết)
            if (!StringUtils.hasText(newType.getCode())) {
                bindingResult.rejectValue("code", "NotBlank", "Vui lòng chọn mã loại.");
                throw new ValidationException("Chưa chọn mã loại.");
            }

            // Kiểm tra tên hiển thị không rỗng (đã có @Valid nhưng check lại cho chắc)
             if (!StringUtils.hasText(newType.getName())) {
                bindingResult.rejectValue("name", "NotBlank", "Tên hiển thị không được để trống.");
                throw new ValidationException("Chưa nhập tên hiển thị.");
            }
             newType.setName(newType.getName().trim()); // Trim tên hiển thị


			promotionTypeService.savePromotionType(newType);
			redirectAttributes.addFlashAttribute("successMessage", "Đã thêm loại khuyến mãi '" + newType.getName() + "'!");
			return "redirect:/vendor/promotion-types";

        } catch (ShopNotApprovedException e) {
            return "vendor/pending_approval";
        } catch (ValidationException | IllegalArgumentException e) {
			logger.warn("Error adding promotion type for user {}: {}", username, e.getMessage());
			try { model.addAttribute("promotionTypes", promotionTypeService.findAll()); } catch (Exception ptEx) { /* Ignore */ }
			// newType đã có trong model
			return "vendor/promotion_type_management";
		} catch (Exception e) { // Lỗi DB, trùng constraint,...
			logger.error("Error adding promotion type for user {}: {}", username, e.getMessage(), e);
            try { model.addAttribute("promotionTypes", promotionTypeService.findAll()); } catch (Exception ptEx) { /* Ignore */ }
            // Xử lý lỗi trùng lặp (có thể xảy ra nếu user submit form 2 lần nhanh hoặc constraint DB)
            if ((e.getMessage().contains("constraint") || e.getMessage().contains("duplicate"))) {
                // Kiểm tra xem lỗi trùng code hay trùng name
                boolean codeExists = promotionTypeService.findAll().stream().anyMatch(pt -> pt.getCode().equals(newType.getCode()));
                boolean nameExists = promotionTypeService.findAll().stream().anyMatch(pt -> pt.getName().equalsIgnoreCase(newType.getName()));
                if (codeExists && !bindingResult.hasFieldErrors("code")) {
                    bindingResult.rejectValue("code", "Unique", "Mã loại này đã được thêm.");
                }
                if (nameExists && !bindingResult.hasFieldErrors("name")) {
                    bindingResult.rejectValue("name", "Unique", "Tên hiển thị này đã tồn tại.");
                }
			} else {
                 model.addAttribute("errorMessage", "Lỗi không xác định: " + e.getMessage());
            }
			return "vendor/promotion_type_management";
		}
	}


	@PostMapping("/promotion-types/delete/{id}")
	public String deletePromotionType(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        String username = authentication.getName();
		logger.warn("Attempting to delete promotion type ID: {} by user {}", id, username);
		try {
            getAndValidateVendorShop(authentication); // Check shop duyệt
			promotionTypeService.deletePromotionType(id);
			logger.info("Promotion type {} deleted by user {}", id, username);
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa loại khuyến mãi!");
        } catch (ShopNotApprovedException e) {
             redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa loại khuyến mãi khi shop chưa được duyệt.");
             return "redirect:/vendor/dashboard";
		} catch (Exception e) {
			logger.error("Error deleting promotion type {} by user {}: {}", id, username, e.getMessage());
			redirectAttributes.addFlashAttribute("errorMessage", "Xóa thất bại: " + e.getMessage());
		}
		return "redirect:/vendor/promotion-types";
	}

	// --- Báo cáo ---
    // (Thêm check shop duyệt)
	@GetMapping("/reports/sales/download")
	public void downloadSalesReport(Authentication authentication, HttpServletResponse response) {
		String username = authentication.getName();
		logger.info("User {} requesting sales report download.", username);
		Long shopId = null;
		try {
			shopId = getAuthenticatedShopId(authentication); // Check shop duyệt

			Workbook workbook = reportService.generateSalesReport(shopId);
			String fileName = "BaoCaoBanHang_Shop" + shopId + "_" + java.time.LocalDate.now() + ".xlsx";
			response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
			workbook.write(response.getOutputStream());
			workbook.close();
			response.flushBuffer();
			logger.info("Sales report sent for shopId: {}", shopId);
        } catch (ShopNotApprovedException e) {
             logger.warn("User {} cannot download report, shop not approved. Status: {}", username, e.getShopStatus());
             // Gửi lỗi về trình duyệt
             sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Không thể tải báo cáo khi shop chưa được duyệt.");
		} catch (IOException e) {
			logger.error("IOException writing sales report for shopId {}: {}", shopId, e.getMessage());
            // Không gửi lỗi về client nếu response đã commit
		} catch (Exception e) {
			logger.error("Error generating or writing sales report for shopId {}: {}", shopId, e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi tạo báo cáo: " + e.getMessage());
		}
	}

    // --- Helper gửi lỗi response cho download ---
    private void sendErrorResponse(HttpServletResponse response, int status, String message) {
        try {
            if (!response.isCommitted()) {
                response.setStatus(status);
                response.setContentType("text/plain; charset=utf-8");
                response.getWriter().write(message);
                response.getWriter().flush();
            }
        } catch (IOException ioEx) {
            logger.error("Error writing error response after report generation failed: {}", ioEx.getMessage());
        }
    }

	// --- Exception tùy chỉnh cho shop chưa duyệt ---
	public static class ShopNotApprovedException extends RuntimeException {
		private final ShopStatus shopStatus;

		public ShopNotApprovedException(String message, ShopStatus status) {
			super(message);
			this.shopStatus = status;
		}

		public ShopStatus getShopStatus() {
			return shopStatus;
		}
	}

    // --- Exception tùy chỉnh cho lỗi validation (để phân biệt với lỗi khác) ---
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    // --- (Optional) Exception Handler ---
    // Có thể thêm @ExceptionHandler(ShopNotApprovedException.class) ở đây
    // để xử lý tập trung thay vì try-catch ở từng mapping
    /*
    @ExceptionHandler(ShopNotApprovedException.class)
    public String handleShopNotApproved(ShopNotApprovedException ex, Model model) {
        logger.warn("Handling ShopNotApprovedException: {}", ex.getMessage());
        model.addAttribute("shopStatus", ex.getShopStatus());
        return "vendor/pending_approval"; // Trả về view chờ duyệt
    }
    */
}