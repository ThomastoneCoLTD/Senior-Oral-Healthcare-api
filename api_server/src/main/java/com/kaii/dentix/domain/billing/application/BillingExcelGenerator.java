package com.kaii.dentix.domain.billing.application;


import com.kaii.dentix.domain.billing.dto.BillingExcelData;
import com.kaii.dentix.domain.billing.dto.BillingOveruseResponse;
import com.kaii.dentix.domain.billing.dto.BillingSummaryResponse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Component
public class BillingExcelGenerator {
    public void generateExcel(BillingExcelData bundle, OutputStream os) throws IOException {

        Workbook wb = new XSSFWorkbook();

        //Summary Sheet 생성
        createSummarySheet(wb, bundle.getSummaries());

        //Billing ID 별 Detail 시트 생성
        for (Map.Entry<Long, BillingOveruseResponse> entry : bundle.getDetailMap().entrySet()) {
            Long billingId = entry.getKey();
            BillingOveruseResponse detail = entry.getValue();

            createBillingDetailSheet(wb, "Billing_" + billingId, detail);
        }

        wb.write(os);
        wb.close();
    }
    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    // ================================
    //  Sheet 1: Billing Summary
    // ================================
    private void createSummarySheet(Workbook wb, List<BillingSummaryResponse> list) {
        Sheet sheet = wb.createSheet("Billing Summary");

        Row header = sheet.createRow(0);
        String[] cols = {
                "billingId", "billingType", "amount", "billedAt",
                "periodStart", "periodEnd", "description"
        };

        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }

        int rowIdx = 1;
        for (BillingSummaryResponse b : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(b.getBillingId());
            row.createCell(1).setCellValue(b.getBillingType().name());
            row.createCell(2).setCellValue(b.getAmount());
            row.createCell(3).setCellValue(b.getBilledAt().toString());
            row.createCell(4).setCellValue(b.getPeriodStart().toString());
            row.createCell(5).setCellValue(b.getPeriodEnd().toString());
            row.createCell(6).setCellValue(b.getDescription());
        }

        autoSizeColumns(sheet, cols.length);
    }
    private int addRow(Sheet sheet, int rowIndex, String key, Object value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value != null ? value.toString() : "");
        return rowIndex + 1;
    }

    private void createBillingDetailSheet(Workbook wb, String sheetName, BillingOveruseResponse detail) {

        Sheet sheet = wb.createSheet(sheetName);

        int r = 0;

        r = addRow(sheet, r, "billingId", detail.getBillingId());
        r = addRow(sheet, r, "planName", detail.getPlanName());
        r = addRow(sheet, r, "periodStart", detail.getPeriodStart().toString());
        r = addRow(sheet, r, "periodEnd", detail.getPeriodEnd().toString());
        r = addRow(sheet, r, "baseAmount", detail.getBaseAmount());
        r = addRow(sheet, r, "totalOveruseAmount", detail.getTotalOveruseAmount());
        r = addRow(sheet, r, "totalOveruseCount", detail.getTotalOveruseCount());

        // 오버유스 리스트 테이블 생성
        r += 2;
        sheet.createRow(r++).createCell(0).setCellValue("Overuse List");

        Row header = sheet.createRow(r++);
        String[] cols = {"billingId", "amount", "billedAt", "billingStatus", "description"};

        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

        for (BillingOveruseResponse.Item item : detail.getOveruseList()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(item.getBillingId());
            row.createCell(1).setCellValue(item.getAmount());
            row.createCell(2).setCellValue(item.getBilledAt().toString());
            row.createCell(3).setCellValue(item.getBillingStatus());
            row.createCell(4).setCellValue(item.getDescription());
        }
    }
}