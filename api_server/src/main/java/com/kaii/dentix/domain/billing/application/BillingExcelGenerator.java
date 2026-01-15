package com.kaii.dentix.domain.billing.application;

import com.kaii.dentix.domain.billing.dto.BillingDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Component
public class BillingExcelGenerator {

    public void generateExcel(BillingDto.ExcelData data, OutputStream out) throws IOException { // 타입 변경
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Billing History");

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Billing ID", "Type", "Plan", "Status", "Amount", "Billed At", "Description"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 데이터 채우기
            List<BillingDto.Summary> summaries = data.getSummaries();
            int rowNum = 1;
            for (BillingDto.Summary summary : summaries) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(summary.getBillingId());
                row.createCell(1).setCellValue(summary.getBillingType().name());
                row.createCell(2).setCellValue(summary.getPlanName());
                row.createCell(3).setCellValue(summary.getBillingStatus());
                row.createCell(4).setCellValue(summary.getAmount());
                row.createCell(5).setCellValue(summary.getBilledAt() != null ? summary.getBilledAt().toString() : "");
                row.createCell(6).setCellValue(summary.getDescription());

                // 필요하다면 detailMap을 사용하여 상세 정보를 추가 시트에 작성 가능
            }

            workbook.write(out);
        }
    }
}