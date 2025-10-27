package com.oneshop.controller.admin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
		if (!model.containsAttribute("editCategory")) {
			model.addAttribute("editCategory", new Category());
		}
		model.addAttribute("allCategories", categories);

		return "admin/categories";
	}
	@GetMapping("/categories/{id}/edit")
	public String editCategory(@PathVariable("id") Long id, Model model) {
		Category categoryToEdit = categoryService.findById(id).orElse(null);

		if (categoryToEdit == null) {
			return "redirect:/admin/categories";
		}
		List<Category> allCategories = categoryService.findAll();
		List<Category> eligibleParentCategories = getEligibleParentCategories(categoryToEdit, allCategories);
		model.addAttribute("editCategory", categoryToEdit);
		model.addAttribute("eligibleParentCategories", eligibleParentCategories);
		model.addAttribute("allCategories", allCategories);
		model.addAttribute("newCategory", new Category()); 
		model.addAttribute("isEditing", true);

		return "admin/categories"; 
	}
	private List<Category> getDescendants(Category category, List<Category> allCategories) {
		List<Category> descendants = new ArrayList<>();
		List<Category> directChildren = allCategories.stream()
				.filter(cat -> cat.getParentCategory() != null
						&& cat.getParentCategory().getCategoryId().equals(category.getCategoryId()))
				.collect(Collectors.toList());
		for (Category child : directChildren) {
			descendants.add(child);
			descendants.addAll(getDescendants(child, allCategories)); 
		}
		return descendants;
	}

	private List<Category> getEligibleParentCategories(Category categoryToExclude, List<Category> allCategories) {
		List<Category> descendants = getDescendants(categoryToExclude, allCategories);
		List<Long> excludedIds = descendants.stream().map(Category::getCategoryId).collect(Collectors.toList());
		excludedIds.add(categoryToExclude.getCategoryId());
		return allCategories.stream().filter(cat -> !excludedIds.contains(cat.getCategoryId()))
				.collect(Collectors.toList());
	}

	// XỬ LÝ LƯU DANH MỤC (cho cả Thêm mới và Chỉnh sửa)
	@PostMapping("/categories/save")
	public String saveCategory(@ModelAttribute("newCategory") Category category,
			RedirectAttributes redirectAttributes) {

		try {
			boolean isNew = category.getCategoryId() == null; 
			Long parentId = null;
			if (category.getParentCategory() != null) {
				parentId = category.getParentCategory().getCategoryId();
			}
			if (parentId != null) {
				Category parent = categoryService.findById(parentId).orElse(null);
				category.setParentCategory(parent);
			} else {
				category.setParentCategory(null);
			}
			categoryService.save(category);
			String message = isNew ? "Thêm danh mục **" + category.getName() + "** thành công!"
					: "Cập nhật danh mục **" + category.getName() + "** thành công!";
			redirectAttributes.addFlashAttribute("successMessage", message);

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu danh mục: " + e.getMessage());
			e.printStackTrace();
		}
		return "redirect:/admin/categories";
	}
}