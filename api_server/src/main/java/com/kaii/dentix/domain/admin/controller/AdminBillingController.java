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
    @GetMapping(value = "/export/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void exportAllBillingExcel(
            @RequestParam Long organizationId,
            HttpServletResponse response
    ) throws IOException {

        try {
            BillingExcelData bundle = billingService.getBillingExcelBundle(organizationId);

            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=billing_all_" + organizationId + ".xlsx"
            );

            billingExcelGenerator.generateExcel(bundle, response.getOutputStream());

        } catch (Exception e) {
            // ⭐ 여기서 응답을 직접 종료
            response.reset();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("application/json;charset=UTF-8");

            response.getWriter().write("""
            {
              "code": 500,
              "message": "엑셀 파일 생성 중 오류가 발생했습니다."
            }
        """);

            // ⭐ GlobalExceptionHandler로 던지지 않음
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
