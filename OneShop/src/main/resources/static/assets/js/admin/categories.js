$(document).ready(function() {
	// ----------------------------------------------------------------------
	// LOGIC CHO MODAL CHỈNH SỬA
	// Mục đích: Đảm bảo sau khi đóng modal chỉnh sửa, URL không còn '/edit' nữa
	// để tránh modal tự động bật lại khi tải lại trang.
	// ----------------------------------------------------------------------
	$('#editCategoryModal').on('hidden.bs.modal', function() {
		// Kiểm tra xem URL hiện tại có chứa '/edit' không
		if (window.location.pathname.includes('/edit')) {
			// Chuyển hướng về trang danh sách chính
			window.location.href = '/admin/categories';
		}
	});

	// ----------------------------------------------------------------------
	// LOGIC CHO MODAL XÓA (CÓ CHỌN DANH MỤC THAY THẾ)
	// Mục đích: Đảm bảo sau khi đóng modal xóa, URL không còn giữ trạng thái 
	// kích hoạt modal xóa để tránh modal tự động bật lại.
	// ----------------------------------------------------------------------
	$('#deleteCategoryModal').on('hidden.bs.modal', function() {
		// Kiểm tra xem URL có chứa ID của danh mục đã chuẩn bị xóa (tức là đang ở chế độ isDeleting)
		// Đây là cách làm tương tự logic /edit. Tuy nhiên, logic chuyển hướng của bạn 
		// đã sử dụng redirectAttributes trong Controller nên việc này có thể không cần thiết, 
		// nhưng ta thêm vào để an toàn nếu có sự cố.
		// Tuy nhiên, vì Controller đã sử dụng FlashAttributes và redirect, URL sẽ là /admin/categories 
		// ngay sau khi Modal được hiển thị. Ta chỉ cần đảm bảo Modal không tự bật lại nếu có lỗi.

		// Vì Controller đã thực hiện chuyển hướng ngay sau khi GET /delete, ta không cần 
		// thay đổi URL ở đây nữa.
	});

});

// ----------------------------------------------------------------------
// HÀM HELPER ĐỂ XỬ LÝ SỰ KIỆN ONCLICK CỦA NÚT XÓA TRONG BẢNG
// Được gọi từ: <a onclick="return handleCategoryDelete(this);">
// ----------------------------------------------------------------------
function handleCategoryDelete(element) {
	// 1. Lấy dữ liệu từ thuộc tính data-*
	// Đảm bảo nút xóa trong HTML đã được sửa như sau:
	// th:data-category-name="${category.name}"
	// th:data-delete-url="@{/admin/categories/{id}/delete(id=${category.categoryId})}"

	var categoryName = element.getAttribute('data-category-name');
	var deleteUrl = element.getAttribute('data-delete-url');

	// 2. Tạo thông báo xác nhận
	var confirmMessage = 'Bạn có chắc chắn muốn xóa danh mục [' + categoryName + '] không? (Nếu danh mục này có danh mục con, bạn sẽ được yêu cầu chọn danh mục thay thế.)';

	// 3. Hiển thị hộp thoại xác nhận
	if (confirm(confirmMessage)) {
		// Nếu người dùng nhấn OK, chuyển hướng đến URL DELETE (GET)
		// Controller sẽ xử lý việc xóa ngay (nếu không có con) hoặc hiển thị Modal (nếu có con)
		window.location.href = deleteUrl;
		return true;
	}
	// Nếu người dùng nhấn Cancel, hủy hành động
	return false;
}