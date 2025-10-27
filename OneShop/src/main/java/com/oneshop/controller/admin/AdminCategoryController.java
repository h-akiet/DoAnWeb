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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.oneshop.entity.Category;
import com.oneshop.service.CategoryService;
import com.oneshop.service.ProductService;

@Controller
@RequestMapping("/admin")
public class AdminCategoryController {
	private static final Long UNCATEGORIZED_CATEGORY_ID = 1001L;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private ProductService productService; 

	@GetMapping("/categories")
	public String listCategories(Model model) {
		List<Category> categories = categoryService.findAll();
		model.addAttribute("categories", categories);
		model.addAttribute("newCategory", new Category());
		if (!model.containsAttribute("editCategory")) {
			model.addAttribute("editCategory", new Category());
		}
		model.addAttribute("allCategories", categories);
		if (!model.containsAttribute("categoryToDelete")) {
			model.addAttribute("categoryToDelete", new Category());
		}
		if (!model.containsAttribute("replacementCategories")) {
			model.addAttribute("replacementCategories", new ArrayList<Category>());
		}

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
	@GetMapping("/categories/{id}/delete")
	public String prepareDeleteCategory(@PathVariable("id") Long id, Model model,
			RedirectAttributes redirectAttributes) {
		if (id.equals(UNCATEGORIZED_CATEGORY_ID)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa danh mục mặc định (Chưa Phân Loại).");
			return "redirect:/admin/categories";
		}
		List<Category> allCategories = categoryService.findAll();
		Category categoryToDelete = categoryService.findById(id).orElse(null);

		if (categoryToDelete == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục để xóa.");
			return "redirect:/admin/categories";
		}
		long childCount = countDirectChildren(categoryToDelete, allCategories); 
		if (childCount > 0) {
			List<Category> replacementCategories = getEligibleParentCategories(categoryToDelete, allCategories);
			redirectAttributes.addFlashAttribute("categoryToDelete", categoryToDelete);
			redirectAttributes.addFlashAttribute("replacementCategories", replacementCategories);
			redirectAttributes.addFlashAttribute("isDeleting", true); 
			redirectAttributes.addFlashAttribute("hasChildren", true);
			return "redirect:/admin/categories";
		} else {
			try {
				Long parentId = categoryToDelete.getParentCategory() != null
						? categoryToDelete.getParentCategory().getCategoryId()
						: null;

				Long replacementProductId = parentId;
				String replacementName = "Danh mục Gốc";

				if (parentId == null) {
					replacementProductId = UNCATEGORIZED_CATEGORY_ID;
					replacementName = "Chưa Phân Loại";
				} else {
					replacementName = categoryService.findById(parentId).map(Category::getName).orElse("Danh mục Gốc");
				}
				int productsMoved = productService.updateCategoryForProducts(id, replacementProductId);

				if (productsMoved > 0) {
					redirectAttributes.addFlashAttribute("infoMessage",
							"Đã chuyển " + productsMoved + " sản phẩm sang **" + replacementName + "**.");
				}
				categoryService.deleteById(id);

				redirectAttributes.addFlashAttribute("successMessage",
						"Xóa danh mục **" + categoryToDelete.getName() + "** thành công!");
			} catch (Exception e) {
				redirectAttributes.addFlashAttribute("errorMessage",
						"Lỗi khi xóa danh mục **" + categoryToDelete.getName() + "**: " + e.getMessage());
				e.printStackTrace();
			}

			return "redirect:/admin/categories";
		}
	}
	@PostMapping("/categories/delete")
	public String performDeleteCategory(@RequestParam("categoryId") Long categoryId,
			@RequestParam(value = "replacementId", required = false) Long replacementId,
			RedirectAttributes redirectAttributes) {
		if (categoryId.equals(UNCATEGORIZED_CATEGORY_ID)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa danh mục mặc định (Chưa Phân Loại).");
			return "redirect:/admin/categories";
		}
		Category categoryToDelete = categoryService.findById(categoryId).orElse(null);

		if (categoryToDelete == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục để xóa.");
			return "redirect:/admin/categories";
		}
		try {
			List<Category> allCategories = categoryService.findAll();
			long childCount = countDirectChildren(categoryToDelete, allCategories);
			if (childCount > 0) {
				Category replacementCategory = null;
				Long finalReplacementId = replacementId; 
				String replacementName = "Danh mục Gốc";
				if (finalReplacementId != null) {
					replacementCategory = categoryService.findById(finalReplacementId).orElse(null);
				}
				if (replacementCategory == null) {
					finalReplacementId = UNCATEGORIZED_CATEGORY_ID;
					replacementName = "Chưa Phân Loại";
				} else {
					replacementName = replacementCategory.getName();
				}
				List<Category> directChildren = allCategories.stream()
						.filter(cat -> cat.getParentCategory() != null
								&& cat.getParentCategory().getCategoryId().equals(categoryId))
						.collect(Collectors.toList());
				for (Category child : directChildren) {
					child.setParentCategory(replacementCategory);
					categoryService.save(child);
				}

				redirectAttributes.addFlashAttribute("infoMessage",
						"Đã chuyển " + childCount + " danh mục con sang **" + replacementName + "**.");
				int productsMoved = productService.updateCategoryForProducts(categoryId, finalReplacementId);

				if (productsMoved > 0) {
					redirectAttributes.addFlashAttribute("infoMessage2",
							"Đã chuyển " + productsMoved + " sản phẩm sang **" + replacementName + "**.");
				}
			}
			categoryService.deleteById(categoryId);
			redirectAttributes.addFlashAttribute("successMessage",
					"Xóa danh mục **" + categoryToDelete.getName() + "** thành công!");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Lỗi khi xóa danh mục **" + categoryToDelete.getName() + "**: " + e.getMessage());
			e.printStackTrace();
		}

		return "redirect:/admin/categories";
	}
	private long countDirectChildren(Category category, List<Category> allCategories) {
		return allCategories.stream().filter(cat -> cat.getParentCategory() != null
				&& cat.getParentCategory().getCategoryId().equals(category.getCategoryId())).count();
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