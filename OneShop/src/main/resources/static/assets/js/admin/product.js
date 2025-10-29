document.addEventListener('DOMContentLoaded', function() {
	var editModalElement = document.getElementById('editProductModal');
	var modalInstance = null; // Khởi tạo biến cho Modal instance

	if (editModalElement) {
		// 1. KHỞI TẠO MODAL INSTANCE (Bắt buộc khi không dùng Data API)
		// Nếu dòng này vẫn gây ra lỗi 'backdrop' cũ, bạn phải sửa lỗi tải Bootstrap JS trước.
		try {
			modalInstance = new bootstrap.Modal(editModalElement);
		} catch (e) {
			console.error("Lỗi khi khởi tạo Bootstrap Modal. Vui lòng kiểm tra lại việc tải Bootstrap JS bundle.", e);
			return; // Dừng nếu không khởi tạo được Modal
		}

		// 2. Gán sự kiện click cho các nút trigger
		document.querySelectorAll('.modal-trigger').forEach(function(button) {
			button.addEventListener('click', function(event) {

				try {
					// Lấy dữ liệu
					var productId = button.getAttribute('data-product-id');
					var productName = button.getAttribute('data-product-name');
					var productCategory = button.getAttribute('data-product-category');
					var productDescription = button.getAttribute('data-product-description');

					// Điền dữ liệu vào form Modal (dùng modalInstance đã khởi tạo)
					var modalTitle = editModalElement.querySelector('.modal-title');
					var modalProductId = editModalElement.querySelector('#modalProductId');
					var modalProductName = editModalElement.querySelector('#modalProductName');
					var modalProductCategory = editModalElement.querySelector('#modalProductCategory');
					var modalProductDescription = editModalElement.querySelector('#modalProductDescription');

					if (modalTitle) modalTitle.textContent = 'Chỉnh Sửa Sản Phẩm ID: ' + productId;
					if (modalProductId) modalProductId.value = productId;
					if (modalProductName) modalProductName.value = productName;
					if (modalProductCategory) modalProductCategory.value = productCategory;
					if (modalProductDescription) modalProductDescription.value = productDescription;

					// Mở Modal thủ công
					if (modalInstance) {
						modalInstance.show();
					}

				} catch (error) {
					console.error("LỖI XỬ LÝ DỮ LIỆU MODAL:", error);
				}
			});
		});

		// 3. Logic Tooltip
		var tooltipTriggerList = [].slice.call(document.querySelectorAll('.product-description-short'));
		tooltipTriggerList.map(function(tooltipTriggerEl) {
			if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
				return new bootstrap.Tooltip(
					tooltipTriggerEl,
					{
						title: tooltipTriggerEl.getAttribute('title') || 'Không có mô tả',
						placement: 'top',
						html: true
					}
				);
			}
		});
	}
});