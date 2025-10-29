/*
$(document).ready(function() {

	if (typeof openEditModal !== 'undefined' && openEditModal) {
		const editModal = new bootstrap.Modal(document.getElementById('editShippingCompanyModal'));
		editModal.show();
	}
	function reIndexRules(containerSelector, entryClass, idPrefix) {
		$(containerSelector).children('.' + entryClass).each(function(newIndex) {
			const $ruleEntry = $(this);
			$ruleEntry.attr('data-index', newIndex);
			$ruleEntry.find('h4').text('Quy tắc #' + (newIndex + 1));

			$ruleEntry.find('[name]').each(function() {
				const $input = $(this);
				const oldName = $input.attr('name');
				if (oldName) {
					const newName = oldName.replace(/rules\[\d+\]/g, 'rules[' + newIndex + ']');
					$input.attr('name', newName);
				}
				if ($input.is(':checkbox')) {
					const newId = idPrefix + newIndex;
					$input.attr('id', newId);
					$ruleEntry.find('label[for^="' + idPrefix + '"]').attr('for', newId);
				}
			});
		});
	}
	const $editRulesContainer = $('#editRulesContainer');
	const editRuleTemplate = $("#ruleTemplate").html().replace(/rule-entry/g, 'rule-entry-edit').replace(/isExpress_/g, 'isExpressEdit_');
	$('#btnAddRuleEdit').on('click', function() {
		const index = $editRulesContainer.children('.rule-entry-edit').length;
		let template = editRuleTemplate.replace(/{INDEX}/g, index)
		.replace(/{INDEX_DISPLAY}/g, index + 1);
		$editRulesContainer.append(template);
		$editRulesContainer.find('.rule-entry-edit').last().find('input[name*="ruleId"]').val('');

		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});
	$editRulesContainer.on('click', '.btnRemoveRuleEdit', function() {
		$(this).closest('.rule-entry-edit').remove();
		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});
	const $rulesContainer = $('#rulesContainer');
	$('#btnAddRule').on('click', function() {
		const index = $rulesContainer.children('.rule-entry').length;
		let template = $('#ruleTemplate').html().replace(/{INDEX}/g, index)
		.replace(/{INDEX_DISPLAY}/g, index + 1);
		$rulesContainer.append(template);

		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_');
	});
	$rulesContainer.on('click', '.btnRemoveRule', function() {
		$(this).closest('.rule-entry').remove();
		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_'); 
	});
	$('.traditional-status-toggle').on('change', function() {
		const switchElement = $(this);
		const companyId = switchElement.data('id');
		const currentActive = switchElement.data('current-active');
		const action = currentActive ? 'VÔ HIỆU HÓA' : 'KÍCH HOẠT'; 
		const confirmMessage = `Bạn có chắc chắn muốn ${action} nhà vận chuyển ID: ${companyId}?`;
		if (confirm(confirmMessage)) {
			const formId = '#form-' + companyId;
			$(formId).submit();

		} else {
			switchElement.prop('checked', currentActive);
		}
	});
});

*/

$(document).ready(function() {

	if (typeof openEditModal !== 'undefined' && openEditModal) {
		const editModal = new bootstrap.Modal(document.getElementById('editShippingCompanyModal'));
		editModal.show();
	}
    
    // Sửa hàm: Loại bỏ việc cập nhật h4 ở đây, chỉ re-index input names
	function reIndexRules(containerSelector, entryClass, idPrefix) {
		$(containerSelector).children('.' + entryClass).each(function(newIndex) {
			const $ruleEntry = $(this);
			$ruleEntry.attr('data-index', newIndex);
            
            // LỖI ĐÃ SỬA: Cập nhật tiêu đề hiển thị từ 0 lên 1, 2, ...
			$ruleEntry.find('h4').text('Quy tắc #' + (newIndex + 1));


			$ruleEntry.find('[name]').each(function() {
				const $input = $(this);
				const oldName = $input.attr('name');
				if (oldName) {
                    // Cập nhật index trong thuộc tính name (rules[0].ruleName)
					const newName = oldName.replace(/rules\[\d+\]/g, 'rules[' + newIndex + ']');
					$input.attr('name', newName);
				}
				if ($input.is(':checkbox')) {
					const newId = idPrefix + newIndex;
					$input.attr('id', newId);
					$ruleEntry.find(`label[for^="${idPrefix}"]`).attr('for', newId);
				}
			});
		});
	}

    // --- LOGIC CHO FORM CHỈNH SỬA ---
	const $editRulesContainer = $('#editRulesContainer');
	const editRuleTemplate = $("#ruleTemplate").html().replace(/rule-entry/g, 'rule-entry-edit').replace(/isExpress_/g, 'isExpressEdit_');
	
    // Sửa logic Thêm Quy tắc (Edit)
	$('#btnAddRuleEdit').on('click', function() {
		const index = $editRulesContainer.children('.rule-entry-edit').length;
        
        // THÊM: Thay thế {INDEX_DISPLAY} bằng index + 1
		let template = editRuleTemplate
            .replace(/{INDEX}/g, index)
            .replace(/{INDEX_DISPLAY}/g, index + 1); 
            
		$editRulesContainer.append(template);
		$editRulesContainer.find('.rule-entry-edit').last().find('input[name*="ruleId"]').val('');

		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});
    
    // Logic Xóa Quy tắc (Edit)
	$editRulesContainer.on('click', '.btnRemoveRuleEdit', function() {
		$(this).closest('.rule-entry-edit').remove();
		reIndexRules('#editRulesContainer', 'rule-entry-edit', 'isExpressEdit_');
	});

    // --- LOGIC CHO FORM THÊM MỚI ---
	const $rulesContainer = $('#rulesContainer');
    
    // Sửa logic Thêm Quy tắc (Add New)
	$('#btnAddRule').on('click', function() {
		const index = $rulesContainer.children('.rule-entry').length;
        
        // THÊM: Thay thế {INDEX_DISPLAY} bằng index + 1
		let template = $('#ruleTemplate').html()
            .replace(/{INDEX}/g, index)
            .replace(/{INDEX_DISPLAY}/g, index + 1);
            
		$rulesContainer.append(template);

		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_');
	});
    
    // Logic Xóa Quy tắc (Add New)
	$rulesContainer.on('click', '.btnRemoveRule', function() {
		$(this).closest('.rule-entry').remove();
		reIndexRules('#rulesContainer', 'rule-entry', 'isExpress_'); 
	});

    // --- LOGIC CHO TOGGLE STATUS (Đã thay thế bằng AJAX) ---
	$('.traditional-status-toggle').on('change', function() {
		const checkbox = $(this);
		const companyId = checkbox.data('id');
		const currentActive = checkbox.data('current-active');
		const isActiveNew = checkbox.is(':checked');
		const formAction = `/admin/shipping-companies/${companyId}/toggle-status`;

		const actionText = isActiveNew ? 'kích hoạt' : 'vô hiệu hóa';
		if (!confirm(`Bạn có chắc muốn ${actionText} nhà vận chuyển này?`)) {
			checkbox.prop('checked', !isActiveNew);
			return;
		}

		$.post(formAction, { isActiveNew: isActiveNew, _method: 'POST' }) 
			.done(function(data) {
				checkbox.data('current-active', isActiveNew);
				 const successMsg = `<div class="alert alert-success alert-dismissible fade show" role="alert">
										Đã ${actionText} nhà vận chuyển thành công!
										<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
									 </div>`;
				 $('.lead.mb-4').after(successMsg);
				 setTimeout(() => { $('.alert-success').alert('close'); }, 3000);
			})
			.fail(function(xhr) {
				checkbox.prop('checked', !isActiveNew);
				 const errorMsg = `<div class="alert alert-danger alert-dismissible fade show" role="alert">
									  Lỗi khi ${actionText}: ${xhr.responseText || 'Vui lòng thử lại.'}
									  <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
								   </div>`;
				 $('.lead.mb-4').after(errorMsg);
			});
	});
    
    // Logic tự động đóng alert sau 5 giây (đã di chuyển từ HTML sang JS)
    $('.alert').each(function() {
        const alert = this;
        setTimeout(function() {
            $(alert).alert('close');
        }, 5000);
    });
});