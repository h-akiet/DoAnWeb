package com.oneshop.service.vendor;

import org.apache.poi.ss.usermodel.Workbook; 
import java.io.IOException;

public interface ReportService {

    Workbook generateSalesReport(Long shopId) throws IOException;
}