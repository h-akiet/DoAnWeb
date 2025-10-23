package com.oneshop.controller;

import com.oneshop.entity.Brand;
import com.oneshop.entity.Category;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductReview;
import com.oneshop.service.BrandService; // Assuming you have BrandService
import com.oneshop.service.CategoryService; // Assuming you have CategoryService
import com.oneshop.service.ProductReviewService;
import com.oneshop.service.ProductService;
import com.oneshop.specification.ProductSpecification;

import jakarta.servlet.http.HttpServletRequest; // Import HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal; // Use BigDecimal for price
import java.util.List;

@Controller
public class ProductController {

    @Autowired private ProductService productService;
    @Autowired private ProductReviewService reviewService;
    @Autowired private CategoryService categoryService; // Inject CategoryService
    @Autowired private BrandService brandService;     // Inject BrandService

    // --- Search Method (Seems okay, but ensure productService.searchAndFilter exists) ---
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String name,
                         @RequestParam(required = false) Long category,
                         @RequestParam(required = false) Double minPrice, // Consider BigDecimal
                         @RequestParam(required = false) Double maxPrice, // Consider BigDecimal
                         Model model) {
        // Assuming searchAndFilter returns List<Product> and getAllCategories exists
        model.addAttribute("products", productService.searchAndFilter(name, category, minPrice, maxPrice));
        model.addAttribute("categories", categoryService.findAll()); // Use CategoryService
        return "search"; // Ensure you have a search.html template
    }

    // --- Product Detail Method (Looks Correct) ---
    @GetMapping("/product/{productId}")
    public String viewProductDetail(@PathVariable Long productId, Model model) {
        Product product = productService.findProductById(productId);
        List<ProductReview> reviews = reviewService.getReviewsByProductId(productId);

        if (product == null) {
            return "redirect:/404"; // Or return an error page template
        }

        List<Product> relatedProducts = productService.findRelatedProducts(product);

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("reviews", reviews);

        return "user/product"; // Maps to templates/user/product.html
    }

    // --- Product List Method (AJAX Enabled) ---
    @GetMapping("/products")
    public String listProducts(
            Model model,
            // Filter Parameters
            @RequestParam(name = "categories", required = false) List<Long> categoryIds,
            @RequestParam(name = "brands", required = false) List<Long> brandIds,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice, // Use BigDecimal
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice, // Use BigDecimal
            // Pagination/Sorting
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "productId,desc") String sort,
            // Request Info
            HttpServletRequest request // Inject request
    ) {

        // --- 1. Build Specification for filtering ---
        Specification<Product> spec = Specification.where(ProductSpecification.hasCategory(categoryIds))
            .and(ProductSpecification.hasBrand(brandIds))
            .and(ProductSpecification.priceBetween(minPrice, maxPrice));
            // Add .and(ProductSpecification.hasName(name)) if you add a name filter parameter

        // --- 2. Build Pageable for sorting and pagination ---
        int zeroBasedPage = Math.max(0, page - 1); // Convert to 0-based page index
        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
        Pageable pageable = PageRequest.of(zeroBasedPage, size, sortOrder);

        // --- 3. Fetch data using Specification and Pageable ---
        Page<Product> productPage = productService.findAll(spec, pageable);

        // --- 4. Add data to Model ---
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryService.findAll()); // For sidebar filters
        model.addAttribute("brands", brandService.findAll());       // For sidebar filters

        // Pass back selected filters/sort/page info to keep state in the view
        model.addAttribute("selectedCategories", categoryIds);
        model.addAttribute("selectedBrands", brandIds);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page); // Use 1-based page for display
        model.addAttribute("totalPages", productPage.getTotalPages());

        // --- 5. Check if AJAX request and return appropriate view ---
        String requestedWithHeader = request.getHeader("X-Requested-With");
        boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWithHeader);

        if (isAjaxRequest) {
            // Return ONLY the fragment for AJAX updates
            return "user/listProduct :: product_list_fragment"; // Check fragment name matches your HTML
        } else {
            // Return the full page template for initial load
            return "user/listProduct"; // Maps to templates/user/listProduct.html
        }
    }
}