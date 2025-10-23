package com.oneshop.service.vendor.impl;

import com.oneshop.entity.vendor.Order;
import com.oneshop.entity.vendor.OrderItem;
import com.oneshop.repository.vendor.OrderRepository;
import com.oneshop.service.vendor.ReportService;

import org.apache.poi.ss.usermodel.*; // Import các lớp của POI
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // Import XSSFWorkbook cho .xlsx
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; // Import Sort
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public Workbook generateSalesReport(Long shopId) throws IOException {
        // Lấy tất cả đơn hàng đã giao hoặc đang giao (ví dụ)
        // Bạn có thể thêm bộ lọc ngày tháng phức tạp hơn ở đây
        List<Order> orders = orderRepository.findByShopIdAndStatusIn(shopId,
                List.of(com.oneshop.entity.vendor.OrderStatus.DELIVERED, com.oneshop.entity.vendor.OrderStatus.SHIPPING),
                Sort.by("orderDate").descending()); // Sắp xếp theo ngày mới nhất

        Workbook workbook = new XSSFWorkbook(); // Tạo workbook mới (.xlsx)
        Sheet sheet = workbook.createSheet("BaoCaoDonHang"); // Tạo sheet tên là "BaoCaoDonHang"

        // --- Tạo Header Row ---
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Mã ĐH", "Ngày Đặt", "Khách Hàng", "SĐT", "Địa Chỉ", "Sản Phẩm", "Số Lượng", "Đơn Giá", "Thành Tiền", "Trạng Thái"};
        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // --- Điền Dữ liệu Đơn hàng ---
        int rowNum = 1;
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        for (Order order : orders) {
            // Mỗi OrderItem là một dòng trong báo cáo (để chi tiết)
            for (OrderItem item : order.getOrderItems()) {
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, "#" + order.getId(), dataStyle);
                createCell(row, 1, order.getOrderDate().format(DATE_FORMATTER), dataStyle);
                createCell(row, 2, order.getCustomerName(), dataStyle);
                createCell(row, 3, order.getCustomerPhone(), dataStyle);
                createCell(row, 4, order.getShippingAddress(), dataStyle);
                createCell(row, 5, item.getProductName(), dataStyle);
                createCell(row, 6, item.getQuantity(), dataStyle);
                createCell(row, 7, item.getPriceAtPurchase().doubleValue(), currencyStyle); // Giá lúc mua
                createCell(row, 8, item.getQuantity() * item.getPriceAtPurchase().doubleValue(), currencyStyle); // Thành tiền item
                createCell(row, 9, order.getStatus().name(), dataStyle); // Tên trạng thái
            }
            // (Bạn có thể thêm dòng tổng tiền cho cả đơn hàng nếu muốn)
        }

        // --- Tự động điều chỉnh độ rộng cột ---
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbook;
    }

    // --- Helper Methods cho việc tạo Cell và Style ---

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
     private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true); // Tự xuống dòng nếu text dài
        style.setVerticalAlignment(VerticalAlignment.TOP);
         style.setBorderBottom(BorderStyle.THIN);
         style.setBorderTop(BorderStyle.THIN);
         style.setBorderLeft(BorderStyle.THIN);
         style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook); // Kế thừa style data
        DataFormat format = workbook.createDataFormat();
        // Định dạng tiền tệ VNĐ (có dấu phẩy, không có chữ VNĐ)
        style.setDataFormat(format.getFormat("#,##0")); 
        return style;
    }

    // Hàm tiện ích để tạo cell và gán giá trị/style
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
     private void createCell(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
         if (value != null) cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    private void createCell(Row row, int column, Double value, CellStyle style) {
        Cell cell = row.createCell(column);
         if (value != null) cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}