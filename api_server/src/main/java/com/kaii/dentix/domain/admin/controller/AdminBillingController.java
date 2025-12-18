package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.application.BillingExcelGenerator;
import com.kaii.dentix.domain.billing.application.BillingExportService;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.dto.*;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.extractor.ExcelExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *관리자 Billing 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/admin/billing")
@RequiredArgsConstructor
public class AdminBillingController {

    private final BillingService billingService;
    private final BillingExportService billingExportService;
    private final AdminService adminService;
    private final OrganizationService organizationService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;
    private final BillingExcelGenerator billingExcelGenerator;

    /** 일반관리자 - 본인 기관의 미납 청구 목록 조회 */
    @GetMapping("/unpaid")
    public ResponseEntity<List<BillingDto>> getUnpaidBillings() {
        List<BillingDto> unpaidBillings = billingService.findAllUnpaidBillings();
        return ResponseEntity.ok(unpaidBillings);
    }

    /** 일반관리자 - 본인 기관의 빌링 내역 조회 */
    @GetMapping("/my-organization")
    public ResponseEntity<?> getMyOrganizationBillings(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        BillingListResponse response = billingService.getBillingsForAdmin(admin);
//        List<BillingResponse> responses = billingService.getBillingsForAdmin(admin);
        return ResponseEntity.ok(new DataResponse<>(200, "OK", response));
    }

    /** 일반관리자 - 본인 기관의 빌링 중 초과요금 내역 상세 조회 */
    @GetMapping("/{billingId}/overuse")
    public ResponseEntity<?> getOveruseBillingDetails(@PathVariable Long billingId) {
        BillingOveruseResponse response = billingService.getOveruseDetails(billingId);
        return ResponseEntity.ok(Map.of(
                "rt", 200,
                "rtMsg", "초과요금 상세 조회 성공",
                "response", response
        ));
    }

    /**
     * 빌링 내역 엑셀 Export
     * - 기관 관리자는 자신의 기관만 가능
     * - 슈퍼관리자는 organizationId 지정 가능
     */
    @GetMapping("/export/excel")
    public void exportAllBillingExcel(
            @RequestParam Long organizationId,
            HttpServletResponse response
    ) {
        try {
            BillingExcelData bundle =
                    billingService.getBillingExcelBundle(organizationId);

            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=billing_all_" + organizationId + ".xlsx"
            );
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // ⚠ Content-Length 설정하지 말 것 (HTTP/2 충돌 방지)

            try (var out = response.getOutputStream()) {
                billingExcelGenerator.generateExcel(bundle, out);
                out.flush();
            }

        } catch (Exception e) {
            // ❗ 파일 다운로드 API에서는 응답 변경 금지
            log.error("Billing excel export failed", e);
            // 그냥 종료 (브라우저에서는 다운로드 실패로 처리)
        }
    }

    /**기관별 Billing 내역 조회 */
    @GetMapping("/{organizationId}/billings")
    public ResponseEntity<List<BillingResponse>> getOrganizationBillings(
            @PathVariable Long organizationId
    ) {
        List<BillingResponse> billings = billingService.getBillingsByOrganization(organizationId);
        return ResponseEntity.ok(billings);
    }

    /**Billing 단건 조회 */
    @GetMapping("/{billingId}")
    public ResponseEntity<BillingDetailResponse> getBillingDetail(
            @PathVariable Long billingId
    ) {
        BillingDetailResponse response = billingService.getBillingDetail(billingId);
        return ResponseEntity.ok(response);
    }

    /**결제 완료 처리 (markPaid) */
    @PatchMapping("/{billingId}/pay")
    public ResponseEntity<BillingDetailResponse> markBillingAsPaid(
            @PathVariable Long billingId,
            @RequestParam(required = false) String paymentRef
    ) {
        BillingDetailResponse response = billingService.markBillingAsPaid(billingId, paymentRef);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/overuse/by-subscription")
    public ResponseEntity<?> getOveruseBySubscription(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        List<SubscriptionOveruseResponse> list =
                billingService.getOveruseBySubscription(admin);
        return ResponseEntity.ok(new DataResponse<>(200, "OK", list));
    }
}
