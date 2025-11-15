package com.kaii.dentix.domain.billing.application;

import com.amazonaws.auth.policy.Resource;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.PlanName;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
@Service
@RequiredArgsConstructor
public class BillingExportService {

    private final BillingRepository billingRepository;
    private final OrganizationService organizationService;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    public ByteArrayOutputStream exportBillingExcel(HttpServletRequest request, Long organizationId) throws IOException {
        // ✅ 1. 관리자 권한별 기관 ID 결정
        Long orgId;
        boolean isSuperAdmin = jwtTokenUtil.isSuperAdmin(request);

        if (isSuperAdmin) {
            if (organizationId == null) {
                throw new IllegalArgumentException("슈퍼관리자는 organizationId를 반드시 지정해야 합니다.");
            }
            orgId = organizationId;
        } else {
            orgId = jwtTokenUtil.getOrganizationIdFromAccessToken(request);
        }

        Organization organization = organizationService.getOrganization(orgId);

        // ✅ 2. 구독상품 확인
        if (organization.getOrganizationSubscription() == null ||
                organization.getOrganizationSubscription().getSubscriptionPlan() == null) {
            throw new IllegalArgumentException("해당 기관의 구독 정보가 없습니다.");
        }

        String planName = organization.getOrganizationSubscription()
                .getSubscriptionPlan()
                .getPlanName()
                .name();

        if (planName.equals("SMALL") || planName.equals("GROWTH")) {
            throw new IllegalStateException("이 구독 상품에서는 빌링 내역 Export 기능을 사용할 수 없습니다.");
        }

        // ✅ 3. 해당 기관의 빌링 내역 조회
        List<Billing> billings = billingRepository.findAllByOrganization(organization);
        if (billings.isEmpty()) {
            throw new IllegalArgumentException("해당 기관의 빌링 내역이 존재하지 않습니다.");
        }

        // ✅ 4. 엑셀 생성
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Billing History");

        // ✅ 헤더
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Billing ID", "Billing Type", "Status",
                "Amount", "Billed At", "Paid At", "Period Start", "Period End", "Description"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // ✅ 데이터 작성
        int rowNum = 1;
        for (Billing billing : billings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(billing.getId());
//            row.createCell(1).setCellValue(billing.get);
            row.createCell(1).setCellValue(billing.getBillingType().name());
            row.createCell(2).setCellValue(billing.getBillingStatus().name());
            row.createCell(3).setCellValue(billing.getAmount());
            row.createCell(4).setCellValue(String.valueOf(billing.getBilledAt()));
            row.createCell(5).setCellValue(String.valueOf(billing.getPaidAt()));
            row.createCell(6).setCellValue(String.valueOf(billing.getPeriodStart()));
            row.createCell(7).setCellValue(String.valueOf(billing.getPeriodEnd()));
            row.createCell(8).setCellValue(billing.getDescription() != null ? billing.getDescription() : "-");
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return out;
    }
}