package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.application.BillingExcelGenerator;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.dto.*;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final AdminService adminService;
    private final BillingExcelGenerator billingExcelGenerator;

    /** 일반관리자 - 본인 기관의 미납 청구 목록 조회 */
    @GetMapping("/unpaid")
    public ResponseEntity<List<BillingDto.Summary>> getUnpaidBillings() {
        return ResponseEntity.ok(billingService.findAllUnpaidBillings());
    }

    /** 일반관리자 - 본인 기관의 빌링 내역 조회 */
    @GetMapping("/my-organization")
    public ResponseEntity<DataResponse<BillingDto.ListResponse>> getMyOrganizationBillings(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        return ResponseEntity.ok(new DataResponse<>(200, "OK", billingService.getBillingsForAdmin(admin)));
    }

    /** 일반관리자 - 본인 기관의 빌링 중 초과요금 내역 상세 조회 */
    @GetMapping("/{billingId}/overuse")
    public ResponseEntity<?> getOveruseBillingDetails(@PathVariable Long billingId) {
        BillingDto.OveruseResponse response = billingService.getOveruseDetails(billingId);
        return ResponseEntity.ok(Map.of("rt", 200, "rtMsg", "초과요금 상세 조회 성공", "response", response));
    }

    /** 기관별 Billing 내역 조회 */
    @GetMapping("/{organizationId}/billings")
    public ResponseEntity<List<BillingDto.Detail>> getOrganizationBillings(@PathVariable Long organizationId) {
        return ResponseEntity.ok(billingService.getBillingsByOrganization(organizationId));
    }

    /** Billing 단건 조회 */
    @GetMapping("/{billingId}")
    public ResponseEntity<BillingDto.Detail> getBillingDetail(@PathVariable Long billingId) {
        return ResponseEntity.ok(billingService.getBillingDetail(billingId));
    }

    /** 결제 완료 처리 */
    @PatchMapping("/{billingId}/pay")
    public ResponseEntity<BillingDto.Detail> markBillingAsPaid(
            @PathVariable Long billingId,
            @RequestParam(required = false) String paymentRef
    ) {
        return ResponseEntity.ok(billingService.markBillingAsPaid(billingId, paymentRef));
    }

    /** 구독별 초과 요금 조회 */
    @GetMapping("/overuse/by-subscription")
    public ResponseEntity<DataResponse<List<BillingDto.SubscriptionOveruse>>> getOveruseBySubscription(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        return ResponseEntity.ok(new DataResponse<>(200, "OK", billingService.getOveruseBySubscription(admin)));
    }

    /** 엑셀 다운로드 */
    @GetMapping("/export/excel")
    public void exportAllBillingExcel(@RequestParam Long organizationId, HttpServletResponse response) {
        try {
            BillingDto.ExcelData bundle = billingService.getBillingExcelBundle(organizationId);

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=billing_all_" + organizationId + ".xlsx");

            try (var out = response.getOutputStream()) {
                billingExcelGenerator.generateExcel(bundle, out);
                out.flush();
            }
        } catch (Exception e) {
            log.error("Billing excel export failed", e);
        }
    }
}