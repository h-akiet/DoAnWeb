package com.oneshop.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.oneshop.entity.Product;
import com.oneshop.service.ProductService;

import org.springframework.ui.Model;


@Controller
public class HomeController {

	@Autowired
    private ProductService productService;
	
    @GetMapping("/")
    public String index(Model model) {
    	List<Product> bestSellers = productService.findBestSellingProducts();
    	
        List<Product> featured = productService.findFeaturedProducts(); // Bạn cần tạo phương thức này

        // DÙNG model.addAttribute ĐỂ ĐẨY DỮ LIỆU RA VIEW
        model.addAttribute("bestSellingProducts", bestSellers);
        model.addAttribute("featuredProducts", featured);
    	
        model.addAttribute("bestSellingProducts", bestSellers);
        return "guest/index";
    }

    @GetMapping("/cart")
    public String cart() {
        return "guest/cart";
    }

    @GetMapping("/contact")
    public String contact() {
        return "guest/contact";
    }

    @GetMapping("/list-product")
    public String listProduct() {
        return "guest/listProduct";
    }

    @GetMapping("/news")
    public String news() {
        return "guest/news";
    }

    @GetMapping("/pay")
    public String pay() {
        return "guest/pay";
    }

    @GetMapping("/product")
    public String product() {
        return "guest/product";
    }
    @GetMapping("/pay-customer")
    public String pay_customer() {
        return "user/pay";
    }
}