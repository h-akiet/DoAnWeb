package com.oneshop.controller.admin;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.Shop;
import com.oneshop.enums.ProductStatus;
import com.oneshop.service.CategoryService;
import com.oneshop.service.ProductService;
import com.oneshop.service.ShopService;

@Controller
@RequestMapping("/admin")
public class AdminProductController {
	@Autowired
	private ShopService shopService;

	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	@GetMapping("/shops")
	public String listAllShops(Model model) {
		List<Shop> shops = shopService.findAll();
		model.addAttribute("shops", shops);
		return "admin/shops"; 
	}
	//  HIỂN THỊ SẢN PHẨM CỦA MỘT SHOP CỤ THỂ 

	@GetMapping("/shops/{shopId}/products")
	public String listShopProducts(@PathVariable Long shopId, @RequestParam(required = false) String productCode,
			@RequestParam(required = false) String status, 
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) String brand,
			Model model) {
		String productCodeParam = (productCode != null && productCode.isEmpty()) ? null : productCode;
		String brandParam = (brand != null && brand.isEmpty()) ? null : brand;
		ProductStatus statusEnum = null;
		if (status != null && !status.isEmpty()) { 
			try {
				statusEnum = ProductStatus.valueOf(status);
			} catch (IllegalArgumentException e) {
			}
		}
		Long categoryIdParam = categoryId;
		Shop currentShop = shopService.findById(shopId);
		List<Product> products = productService.findFilteredProducts(shopId, productCodeParam, statusEnum,
				categoryIdParam, brandParam);
		List<Category> categories = categoryService.findAll();
		Set<String> brands = productService.findAllUniqueBrands();

		model.addAttribute("products", products);
		model.addAttribute("categories", categories);
		model.addAttribute("brands", brands);
		model.addAttribute("currentShopId", shopId);
		model.addAttribute("currentShopName", currentShop != null ? currentShop.getShopName() : "N/A");

		model.addAttribute("selectedProductCode", productCodeParam);
		model.addAttribute("selectedStatusValue", status);
		model.addAttribute("selectedCategoryId", categoryIdParam);
		model.addAttribute("selectedBrand", brandParam);

		return "admin/shop_products";
	}
	
	// THAO TÁC QUẢN LÝ SẢN PHẨM

	//Xử lý việc duyệt hoặc từ chối sản phẩm.
	@GetMapping("/products/updateStatus")
	public String updateProductStatus(@RequestParam Long productId, @RequestParam String newStatus,
			@RequestParam Long shopId) {

		try {
			ProductStatus statusEnum = ProductStatus.valueOf(newStatus);
			productService.updateStatus(productId, statusEnum);
		} catch (IllegalArgumentException e) {
			
		}
		return "redirect:/admin/shops/" + shopId + "/products";
	}

	//Xử lý chỉnh sửa thông tin sản phẩm 
	@PostMapping("/products/update")
	public String editProduct(@ModelAttribute Product product, @RequestParam Long shopId) {
		productService.updateAdminFields(product);
		return "redirect:/admin/shops/" + shopId + "/products";
	}

	//Xử lý xóa sản phẩm.
	@GetMapping("/products/delete/{id}")
	public String deleteProduct(@PathVariable Long id, @RequestParam Long shopId) {
		productService.deleteById(id);
		return "redirect:/admin/shops/" + shopId + "/products";
	}
}