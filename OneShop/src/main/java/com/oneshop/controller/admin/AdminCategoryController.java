package com.oneshop.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.oneshop.entity.Category;
import com.oneshop.service.CategoryService;

@Controller
@RequestMapping("/admin")
public class AdminCategoryController {
	@Autowired
	private CategoryService categoryService;

	@GetMapping("/categories")
	public String listCategories(Model model) {
		List<Category> categories = categoryService.findAll();
		model.addAttribute("categories", categories);
		model.addAttribute("newCategory", new Category());

		return "admin/categories";
	}

	// XỬ LÝ LƯU DANH MỤC MỚI
	@PostMapping("/categories/save")
	public String saveCategory(@ModelAttribute("newCategory") Category category,
			RedirectAttributes redirectAttributes) {

		try {
			// Lấy ID của danh mục cha từ đối tượng được binding (ID được gửi từ form)
			Long parentId = null;
			if (category.getParentCategory() != null) {
				parentId = category.getParentCategory().getCategoryId();
			}

			// Xử lý Danh Mục Cha dựa trên ID đã nhận
			if (parentId != null) {
				// Tìm Category cha thực tế từ Database
				Category parent = categoryService.findById(parentId).orElse(null);

				// Thiết lập lại đối tượng Category cha đã được tìm thấy
				category.setParentCategory(parent);
			} else {
				// Nếu không có ID nào được chọn (parentId là null), đảm bảo đối tượng cha là
				// null
				category.setParentCategory(null);
			}

			// Lưu Category mới vào Database
			categoryService.save(category);

			// Thêm thông báo thành công
			redirectAttributes.addFlashAttribute("successMessage",
					"Thêm danh mục **" + category.getName() + "** thành công!");

		} catch (Exception e) {
			// Thêm thông báo lỗi
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm danh mục: " + e.getMessage());
			e.printStackTrace();
		}

		// Chuyển hướng về trang danh sách
		return "redirect:/admin/categories";
	}
}
