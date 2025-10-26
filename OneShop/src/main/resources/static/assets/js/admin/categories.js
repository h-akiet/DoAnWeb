// SỬ DỤNG window.allCategories đã được tạo trong HTML bằng Thymeleaf
// Nếu bạn chỉ dùng 'const allCategories = [];' như trong code cũ, 
// bạn phải đổi thành 'const allCategories = window.allCategories;'
const allCategories = window.allCategories || []; 

// Hàm đệ quy để tìm tất cả các ID con (và cháu) của một ID cha
function getChildrenIds(parentId) {
    let childrenIds = [];
    allCategories.forEach(cat => {
        // So sánh ID chuỗi
        if (cat.parentId === String(parentId)) { 
            childrenIds.push(cat.id);
            childrenIds = childrenIds.concat(getChildrenIds(cat.id));
        }
    });
    return childrenIds;
}

// Hàm điền dữ liệu vào combobox
function fillParentCategorySelect(currentCategoryId) {
    const select = $('#parentCategorySelect');
    select.empty();
    select.append('<option value="">-- Chọn danh mục cha --</option>');

    // Lấy danh sách ID cần loại trừ (chính nó và con cháu)
    let excludedIds = [];
    if (currentCategoryId) {
        excludedIds.push(String(currentCategoryId));
        excludedIds = excludedIds.concat(getChildrenIds(currentCategoryId));
    }

    // Thêm các danh mục hợp lệ vào combobox
    allCategories.forEach(cat => {
        if (!excludedIds.includes(cat.id)) {
            select.append(`<option value="${cat.id}">${cat.name}</option>`);
        }
    });
}

$(document).ready(function() {
    // Đảm bảo jQuery được tải trước
    if (typeof jQuery === 'undefined') {
        console.error("jQuery không được tải. Vui lòng kiểm tra lại thứ tự tải script.");
        return;
    }

    const modal = $('#generalCategoryModal');
    const modalTitle = $('#modalTitle');
    const categoryForm = $('#categoryForm');
    
    // Đảm bảo đối tượng bootstrap tồn tại (cho Modal)
    if (typeof bootstrap === 'undefined') {
        console.error("Bootstrap JS không được tải. Vui lòng kiểm tra lại thứ tự tải script.");
    }

    // --- Xử lý khi nhấn nút Thêm Mới ---
    $('#btnAddCategory').on('click', function() {
        modalTitle.text('Thêm Danh Mục Mới');
        categoryForm.trigger('reset'); // Reset form
        $('#categoryId').val(''); // Đảm bảo ID trống cho chế độ Thêm mới
        fillParentCategorySelect(null); // Lấy tất cả danh mục
    });

    // --- Xử lý khi nhấn nút Chỉnh Sửa ---
    $('.btn-edit').on('click', function(e) {
        e.preventDefault();
        
        // Lấy ID từ href
        const url = $(this).attr('href');
        const categoryId = url.split('/edit')[0].split('/').pop();

        // Tìm đối tượng danh mục cần chỉnh sửa
        const categoryToEdit = allCategories.find(c => c.id === categoryId);

        if (categoryToEdit) {
            // Đặt tiêu đề và điền dữ liệu
            modalTitle.text('Chỉnh Sửa Danh Mục: ' + categoryToEdit.name);
            $('#categoryId').val(categoryToEdit.id);
            $('#categoryName').val(categoryToEdit.name);

            // Lọc và điền combobox
            fillParentCategorySelect(categoryToEdit.id);
            
            // Chọn danh mục cha hiện tại
            $('#parentCategorySelect').val(categoryToEdit.parentId);

            // Mở Modal
            const bootstrapModal = new bootstrap.Modal(modal[0]);
            bootstrapModal.show();
        }
    });
});