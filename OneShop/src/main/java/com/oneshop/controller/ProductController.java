package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.oneshop.service.ProductService;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String name,
                         @RequestParam(required = false) Long category,
                         @RequestParam(required = false) Double minPrice,
                         @RequestParam(required = false) Double maxPrice,
                         Model model) {
        model.addAttribute("products", productService.searchAndFilter(name, category, minPrice, maxPrice));
        model.addAttribute("categories", productService.getAllCategories());
        return "search";
    }
}