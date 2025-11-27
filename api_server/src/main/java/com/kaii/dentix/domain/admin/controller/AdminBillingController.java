package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.application.BillingExportService;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.dto.*;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ✅ 관리자 Billing 컨트롤러
 */
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
    /**
     * ✅ 미납 청구 목록 조회
     */
    @GetMapping("/unpaid")
    public ResponseEntity<List<BillingDto>> getUnpaidBillings() {
        List<BillingDto> unpaidBillings = billingService.findAllUnpaidBillings();
        return ResponseEntity.ok(unpaidBillings);
    }

    /** ✅ 기관별 Billing 내역 조회 */
    @GetMapping("/{organizationId}/billings")
    public ResponseEntity<List<BillingResponse>> getOrganizationBillings(
            @PathVariable Long organizationId
    ) {
        List<BillingResponse> billings = billingService.getBillingsByOrganization(organizationId);
        return ResponseEntity.ok(billings);
    }

    /** ✅ Billing 단건 조회 */
    @GetMapping("/{billingId}")
    public ResponseEntity<BillingDetailResponse> getBillingDetail(
            @PathVariable Long billingId
    ) {
        BillingDetailResponse response = billingService.getBillingDetail(billingId);
        return ResponseEntity.ok(response);
    }

    /** ✅ 결제 완료 처리 (markPaid) */
    @PatchMapping("/{billingId}/pay")
    public ResponseEntity<BillingDetailResponse> markBillingAsPaid(
            @PathVariable Long billingId,
            @RequestParam(required = false) String paymentRef
    ) {
        BillingDetailResponse response = billingService.markBillingAsPaid(billingId, paymentRef);
        return ResponseEntity.ok(response);
    }

    /** ✅ 관리자 본인 기관의 빌링 내역 조회 */
    @GetMapping("/my-organization")
    public ResponseEntity<?> getMyOrganizationBillings(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        BillingListResponse response = billingService.getBillingsForAdmin(admin);
//        List<BillingResponse> responses = billingService.getBillingsForAdmin(admin);
        return ResponseEntity.ok(new DataResponse<>(200, "OK", response));
    }

    /**
     * ✅ 기관별 빌링 내역 엑셀 export
     * 슈퍼관리자는 모든 기관, 일반 관리자는 본인 기관만 가능
     */
    /**
     * ✅ 빌링 내역 엑셀 Export
     * - 기관 관리자는 자신의 기관만 가능
     * - 슈퍼관리자는 organizationId 지정 가능
     */
    @GetMapping("/export/excel")
    public void exportBillingExcel(
            @RequestParam(required = false) Long organizationId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        ByteArrayOutputStream out = billingExportService.exportBillingExcel(request, organizationId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=billing_history.xlsx");

        out.writeTo(response.getOutputStream());
        out.close();
    }
    @GetMapping("/overuse/by-subscription")
    public ResponseEntity<?> getOveruseBySubscription(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        List<SubscriptionOveruseResponse> list =
                billingService.getOveruseBySubscription(admin);
        return ResponseEntity.ok(new DataResponse<>(200, "OK", list));
    }

//    /** ✅ 내 기관의 Billing 내역 조회 */
//    @GetMapping("/my")
//    public ResponseEntity<List<BillingResponse>> getMyBillingList(HttpServletRequest request) {
//
//        Long adminId = jwtTokenUtil.getCurrentAdminId();
//        Admin admin = adminRepository.findById(adminId)
//                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));
//
//        List<BillingResponse> responses = billingService.getBillingsForMyOrganization(admin);
//        return ResponseEntity.ok(responses);
//    }
}
