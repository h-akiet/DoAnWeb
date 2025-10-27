package com.oneshop.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.oneshop.entity.Shop;
import com.oneshop.service.ShopService;

@Controller
@RequestMapping("/admin")
public class AdminAppDiscountController {
	@Autowired
	private ShopService shopService;

	@GetMapping("/app-discounts")
	public String listApprovedShops(Model model) {
		List<Shop> approvedShops = shopService.getApprovedShops();
		model.addAttribute("shops", approvedShops);
		return "admin/app-discounts";
	}
	@PostMapping("/app-discounts/update")
	public String updateAppDiscount(@RequestParam("shopId") Long shopId,
			@RequestParam("newCommissionRatePercent") BigDecimal newCommissionRatePercent,
			RedirectAttributes redirectAttributes) {

		try {
			if (newCommissionRatePercent.compareTo(BigDecimal.ZERO) < 0
					|| newCommissionRatePercent.compareTo(new BigDecimal(100)) > 0) {

				redirectAttributes.addFlashAttribute("message",
						"Chiết khấu không hợp lệ! Phải nằm trong khoảng 0% đến 100%.");
				redirectAttributes.addFlashAttribute("status", "Danger");
				return "redirect:/admin/app-discounts";
			}
			BigDecimal newCommissionRate = newCommissionRatePercent.divide(new BigDecimal(100), 4,
					BigDecimal.ROUND_HALF_UP);

			Shop updatedShop = shopService.updateShopCommissionRate(shopId, newCommissionRate);

			if (updatedShop != null) {
				redirectAttributes.addFlashAttribute("message",
						"Cập nhật chiết khấu cho cửa hàng " + updatedShop.getShopName() + " thành công !");
				redirectAttributes.addFlashAttribute("status", "Success"); 
			} else {
				redirectAttributes.addFlashAttribute("message", "Lỗi: Không tìm thấy cửa hàng với ID " + shopId + ".");
				redirectAttributes.addFlashAttribute("status", "Danger");
			}

		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("message", "Đã xảy ra lỗi trong quá trình cập nhật chiết khấu.");
			redirectAttributes.addFlashAttribute("status", "Danger");
		}
		return "redirect:/admin/app-discounts";
	}
}
