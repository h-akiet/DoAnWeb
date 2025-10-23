package com.oneshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
    	List<Product> bestSellers = productService.findBestSellingProducts();
    	
    	List<Product> newest = productService.findNewestProducts();
        model.addAttribute("newestProducts", newest);

        List<Product> bestPrice = productService.findBestPriceProducts();
        model.addAttribute("bestPriceProducts", bestPrice);
        model.addAttribute("bestSellingProducts", bestSellers);
        
    	
        model.addAttribute("bestSellingProducts", bestSellers);
        return "guest/index";
    }


    @GetMapping("/contact")
    public String contact() {
        return "guest/contact";
    }

   

    @GetMapping("/news")
    public String news() {
        return "guest/news";
    }

   

    @GetMapping("/product")
    public String product() {
        return "user/product";
    }
    
    @GetMapping("/pay-customer")
    public String pay_customer() {
        return "user/pay";
    }
}