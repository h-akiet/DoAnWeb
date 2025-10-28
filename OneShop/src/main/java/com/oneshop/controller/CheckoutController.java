package com.oneshop.controller; // Or your controller package

import com.oneshop.dto.CartDto;
import com.oneshop.dto.CartItemDto;
import com.oneshop.entity.Address;
import com.oneshop.service.AddressService;
import com.oneshop.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CheckoutController {

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressService addressService;

    @GetMapping("/pay") // Or "/checkout" if you prefer that URL
    public String showCheckoutPage(
            @RequestParam(name = "variantIds", required = false) List<Long> variantIds, // Get selected IDs from cart
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login"; // Must be logged in
        }
        
        // Handle case where no items were selected (e.g., direct URL access)
        if (variantIds == null || variantIds.isEmpty()) {
            return "redirect:/cart"; // Redirect back to cart if no items selected
        }

        String username = authentication.getName();

        // 1. Get User Addresses (Default first)
        List<Address> addresses = addressService.findByUsernameOrdered(username);
        Address defaultAddress = addresses.stream()
                                         .filter(addr -> Boolean.TRUE.equals(addr.getIsDefault()))
                                         .findFirst()
                                         .orElse(addresses.isEmpty() ? null : addresses.get(0)); // Use first if no default

        // 2. Get the full cart and filter selected items
        CartDto fullCart = cartService.getCartForUser(username);
        Map<Long, CartItemDto> selectedItemsMap = fullCart.getItems().entrySet().stream()
                .filter(entry -> variantIds.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                
        // Convert map values to a List for easier iteration in Thymeleaf if needed
        List<CartItemDto> selectedItemsList = List.copyOf(selectedItemsMap.values());

        // 3. Calculate Totals based ONLY on selected items
        BigDecimal subtotal = selectedItemsMap.values().stream()
                .map(CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Simple free shipping example - adjust as needed
        BigDecimal shippingCost = BigDecimal.ZERO; 
        BigDecimal grandTotal = subtotal.add(shippingCost);

        // 4. Add data to the model
        model.addAttribute("addresses", addresses);
        model.addAttribute("defaultAddress", defaultAddress);
        model.addAttribute("selectedItems", selectedItemsList); // Use the list
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shippingCost", shippingCost);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("selectedVariantIds", variantIds); 

        return "user/pay"; // Your Thymeleaf template name (e.g., pay.html)
    }

    

    @GetMapping("/api/addresses/{id}")
    public ResponseEntity<Address> getAddressById(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        return addressService.findByUsernameOrdered(username).stream()
                .filter(a -> a.getAddressId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    

   

    // Add POST mapping later for /placeOrder
}