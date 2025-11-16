package com.kaii.dentix.domain.superUser.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminStatisticService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.dto.BillingStatusHistoryResponse;
import com.kaii.dentix.domain.billing.dto.BillingStatusUpdateRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.superUser.dto.OrganizationDetailResponse;
import com.kaii.dentix.domain.superUser.dto.OrganizationListResponse;
import com.kaii.dentix.domain.superUser.application.SuperAdminOrganizationService;
import com.kaii.dentix.domain.superUser.dto.SuperAdminAllUserStatisticsResponse;
import com.kaii.dentix.global.common.dto.PagingRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/superadmin/organization")
@RequiredArgsConstructor
public class SuperAdminOrganizationController {
    private final SuperAdminOrganizationService superAdminOrganizationService;
    private final BillingService billingService;
    private final AdminService adminService;
    private final AdminUserService adminUserService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;
    private final AdminStatisticService adminStatisticService;
    private final OrganizationService organizationService;
//    private final SuperAdminOrganizationService superAdminOrganizationService;

    /** ✅ 1. 전체 기관 목록 조회 */
    @GetMapping("/all")
    public ResponseEntity<DataResponse<List<OrganizationListResponse>>> getAllOrganizations() {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 목록 조회 성공",
                        superAdminOrganizationService.getAllOrganizations()));
    }

    /** ✅ 2. 기관 상세 정보 조회 */
    @GetMapping("/{organizationId}")
    public ResponseEntity<DataResponse<OrganizationDetailResponse>> getOrganizationDetail(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 상세 조회 성공",
                        superAdminOrganizationService.getOrganizationDetail(organizationId)));
    }

    /** ✅ 3. 기관별 구독이력 조회 */
    @GetMapping("/{organizationId}/subscriptions")
    public ResponseEntity<DataResponse<?>> getOrganizationSubscriptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 구독이력 조회 성공",
                        superAdminOrganizationService.getOrganizationSubscriptions(organizationId)));
    }

    /** ✅ 4. 기관별 빌링내역 조회 */
    @GetMapping("/{organizationId}/billings")
    public ResponseEntity<DataResponse<Map<String, Object>>> getBillings(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "DESC") String sort
    ) {

        PagingRequest pagingRequest = new PagingRequest();
        pagingRequest.setPage(page);
        pagingRequest.setSize(size);

        Map<String, Object> result = billingService.getBillingList(
                organizationId,
                status,
                sort,
                pagingRequest
        );

        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 빌링 목록 조회 성공", result)
        );
    }
    /**
     * 🔹 빌링 상태 변경
     */
    @PatchMapping("/billing/{billingId}/status")
    public ResponseEntity<DataResponse<BillingStatusHistoryResponse>> updateBillingStatus(
            HttpServletRequest request,
            @PathVariable Long billingId,
            @Valid @RequestBody BillingStatusUpdateRequest requestDto
    ) {
        // TODO: 토큰에서 슈퍼관리자 아이디 꺼내기
         Admin admin = adminService.getTokenAdmin(request);
         String changedBy = admin.getAdminLoginIdentifier();
//        String changedBy = "superadmin"; // 일단 하드코딩, 나중에 위로 교체
//
        BillingStatusHistoryResponse response =
                billingService.updateBillingStatus(billingId, requestDto, changedBy);

        return ResponseEntity.ok(
                new DataResponse<>(200, "빌링 상태 변경 성공", response)
        );
    }

    /**
     * 🔹 특정 Billing의 상태 변경 로그 조회
     */
    @GetMapping("/billing/{billingId}/status-histories")
    public ResponseEntity<DataResponse<List<BillingStatusHistoryResponse>>> getBillingStatusHistories(
            @PathVariable Long billingId
    ) {
        List<BillingStatusHistoryResponse> histories =
                billingService.getBillingStatusHistories(billingId);

        return ResponseEntity.ok(
                new DataResponse<>(200, "빌링 상태 변경 이력 조회 성공", histories)
        );
    }

    @GetMapping("/{organizationId}/users")
    public ResponseEntity<DataResponse<Page<AdminUserInfoDto>>> getUsersByOrganization(
            @PathVariable Long organizationId,
            AdminUserListRequest request
    ) {
        request.setOrganizationId(organizationId); // 🔥 중요: 기관 ID 설정

        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 사용자 목록 조회 성공",
                        adminUserService.getUsersByOrganization(request))
        );
    }

    /**
     * 슈퍼관리자 - 특정 기관의 사용자 사용량 조회
     */
    @GetMapping("/{orgId}/usage")
    public ResponseEntity<DataResponse<OrganizationUsageResponse>> getOrganizationUsage(
            @PathVariable Long orgId
    ) {
        return ResponseEntity.ok(
                new DataResponse<>(
                        200,
                        "기관 사용량 조회 성공",
                        superAdminOrganizationService.getOrganizationUsageByOrgId(orgId)
                )
        );
    }

    @GetMapping("/statistics")
    public DataResponse<SuperAdminAllUserStatisticsResponse> getSuperAdminStatistics(
            HttpServletRequest request
    ) {
        Admin admin = adminService.getTokenAdmin(request);

        return new DataResponse<>(
                adminStatisticService.getSuperAdminTotalStats(admin)
        );
    }

}
