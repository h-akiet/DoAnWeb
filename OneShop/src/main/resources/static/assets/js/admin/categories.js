
$(document).ready(function() {
    $('#editCategoryModal').on('hidden.bs.modal', function () {
        if (window.location.pathname.includes('/edit')) {
            window.location.href = '/admin/categories';
        }
    });
});