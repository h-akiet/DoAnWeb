/**
 * Logic xử lý việc thêm, xóa các quy tắc tính phí động (Shipping Rules)
 * trong form quản lý Nhà vận chuyển của Admin.
 */
$(document).ready(function() {

	// --- Bổ sung logic MỞ MODAL CHỈNH SỬA bằng JavaScript ---
	// Kiểm tra Flash Attribute 'openEditModal' từ Controller sau khi redirect
	// Cần dùng Thymeleaf Inline (hoặc đặt biến js từ Thymeleaf)
	// LƯU Ý: Đảm bảo file HTML của bạn có dòng script để đặt biến này:
	// <script th:inline="javascript">const openEditModal = [[${openEditModal} ?: false]];</script>

	if (typeof openEditModal !== 'undefined' && openEditModal) {
		// Tự động mở modal Chỉnh sửa
		// Khởi tạo đối tượng Bootstrap Modal và gọi show()
		const editModal = new bootstrap.Modal(document.getElementById('editShippingCompanyModal'));
		editModal.show();
	}

	// --- LOGIC CHUNG (Cho cả Thêm mới và Chỉnh sửa) ---

	// Hàm Re-index Rules
	function reIndexRules(containerSelector, entryClass, idPrefix) {
		$(containerSelector).children('.' + entryClass).each(function(newIndex) {
			const $ruleEntry = $(this);
			$ruleEntry.attr('data-index', newIndex);
			$ruleEntry.find('h4').text('Quy tắc #' + newIndex);

			$ruleEntry.find('[name]').each(function() {
				const $input = $(this);
				const oldName = $input.attr('name');
				if (oldName) {
					// Thay thế index cũ (rules[số]) bằng index mới
					const newName = oldName.replace(/rules\[\d+\]/g, 'rules[' + newIndex + ']');
					$input.attr('name', newName);
				}

				// Xử lý id cho checkbox và label đi kèm
				if ($input.is(':checkbox')) {
					const newId = idPrefix + newIndex;
					$input.attr('id', newId);
					// Cập nhật thuộc tính 'for' của label
					$ruleEntry.find('label[for^="' + idPrefix + '"]').attr('for', newId);
				}
			});
		});
	}

	// --- LOGIC CHO MODAL CHỈNH SỬA (MỚI) ---
	const $editRulesContainer = $('#editRulesContainer');
	// Tái sử dụng template, thay thế class và id prefix cho phù hợp với modal Chỉnh sửa
	// Cần đảm bảo ruleTemplate trong HTML đã được cập nhật với trường ruleId
	const editRuleTemplate = $("#ruleTemplate").html().replace(/rule-entry/g, 'rule-entry-edit').replace(/isExpress_/g, 'isExpressEdit_');

	// Logic Thêm Quy tắc trong modal Chỉnh sửa
	$('#btnAddRuleEdit').on('click', function() {
		const index = $editRulesContainer.children('.rule-entry-edit').length;
		let template = editRuleTemplate.replace(/{INDEX}/g, index);
		$editRulesContainer.append(template);

		// Đặt value ruleId là rỗng cho rule mới để Spring biết đây là INSERT mới
		$editRulesContainer.find('.rule-entry-edit').last().find('input[name*="ruleId"]').val('');

		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});

	// Logic Xóa Quy tắc trong modal Chỉnh sửa
	$editRulesContainer.on('click', '.btnRemoveRuleEdit', function() {
		$(this).closest('.rule-entry-edit').remove();
		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});

	// --- LOGIC CHO MODAL THÊM MỚI (CẬP NHẬT/THAY THẾ logic cũ) ---
	const $rulesContainer = $('#rulesContainer');

	// Logic Thêm Quy tắc trong modal Thêm mới
	// THAY THẾ logic cũ của $("#btnAddRule").click(function()...
	$('#btnAddRule').on('click', function() {
		const index = $rulesContainer.children('.rule-entry').length;
		let template = $('#ruleTemplate').html().replace(/{INDEX}/g, index);
		$rulesContainer.append(template);

		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_');
	});

	// Logic Xóa Quy tắc trong modal Thêm mới
	// THAY THẾ logic cũ của $("#rulesContainer").on("click", ".btnRemoveRule", function()...
	$rulesContainer.on('click', '.btnRemoveRule', function() {
		$(this).closest('.rule-entry').remove();
		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_'); // Bổ sung re-index
	});


	// --- LOGIC CHUYỂN ĐỔI TRẠNG THÁI (Giữ nguyên) ---
	$('.traditional-status-toggle').on('change', function() {
		const switchElement = $(this);
		const companyId = switchElement.data('id');
		const currentActive = switchElement.data('current-active');
		const action = currentActive ? 'VÔ HIỆU HÓA' : 'KÍCH HOẠT'; // Hành động đang được thực hiện (sau khi click)
		const confirmMessage = `Bạn có chắc chắn muốn ${action} nhà vận chuyển ID: ${companyId}?`;

		// 1. Xác nhận
		if (confirm(confirmMessage)) {
			// 2. Submit form tương ứng
			const formId = '#form-' + companyId;
			$(formId).submit();

		} else {
			// 3. Người dùng HỦY bỏ -> Đặt lại trạng thái cũ ngay lập tức
			switchElement.prop('checked', currentActive);
		}
	});
});