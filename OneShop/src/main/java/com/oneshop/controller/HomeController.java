package com.oneshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
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