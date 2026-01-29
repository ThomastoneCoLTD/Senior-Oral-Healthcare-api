package com.kaii.dentix.domain.superAdmin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.dto.BillingDto;
import com.kaii.dentix.domain.billing.dto.BillingOveruseResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.superAdmin.application.SuperAdminOrganizationService;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminDto;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminStatisticDto;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PagingRequest;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/superadmin/organization")
@RequiredArgsConstructor
public class SuperAdminOrganizationController {
    private final SuperAdminOrganizationService superAdminOrganizationService;
    private final BillingService billingService;
    private final AdminService adminService;
    private final AdminUserService adminUserService;

    /** 사용자 통계 */
    @GetMapping("/statistics")
    public DataResponse<SuperAdminStatisticDto.TotalUserStats> getSuperAdminStatistics(
            HttpServletRequest request
    ) {
        Admin admin = adminService.getTokenAdmin(request);

        return new DataResponse<>(
                200,
                "전체 통계 조회 성공",
                superAdminOrganizationService.getSuperAdminTotalUserStatistics(admin)
        );
    }
    /** 전체 기관 목록 조회 */
    @GetMapping("/all")
    public ResponseEntity<DataResponse<List<SuperAdminDto.OrganizationListResponse>>> getAllOrganizations() {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 목록 조회 성공",
                        superAdminOrganizationService.getAllOrganizations()));
    }

    /** 특정 기관의 사용자 사용량 조회 */
    @GetMapping("/{orgId}/usage")
    public ResponseEntity<DataResponse<OrganizationDto.UsageResponse>> getOrganizationUsage(
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

    /** 특정 기관의 사용자 조회 */
    @GetMapping("/{organizationId}/users")
    public ResponseEntity<DataResponse<Page<AdminUserDto.Info>>> getUsersByOrganization(
            @PathVariable Long organizationId,
            @ModelAttribute AdminUserDto.SearchRequest request
    ) {
        request.setOrganizationId(organizationId);

        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 사용자 목록 조회 성공",
                        adminUserService.getUsersByOrganization(request))
        );
    }

    /** 기관별 현재 구독 상품 조회 */
    @GetMapping("/{organizationId}/subscription/current")
    public ResponseEntity<DataResponse<SuperAdminDto.CurrentSubscriptionResponse>> getCurrentSubscription(
            @PathVariable Long organizationId) {

        SuperAdminDto.CurrentSubscriptionResponse dto =
                superAdminOrganizationService.getCurrentSubscription(organizationId);

        return ResponseEntity.ok(
                new DataResponse<>(200, "현재 구독상품 조회 성공", dto)
        );
    }

    /** 기관별 구독이력 조회 */
    @GetMapping("/{organizationId}/subscriptions")
    public ResponseEntity<DataResponse<?>> getOrganizationSubscriptions(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 구독이력 조회 성공",
                        superAdminOrganizationService.getOrganizationSubscriptions(organizationId)));
    }

    /** 특정 기관의 Billing 리스트 조회 */
    @GetMapping("/{organizationId}/org-bill")
    public ResponseEntity<DataResponse<BillingDto.ListResponse>> getBillingList(
            @PathVariable Long organizationId,
            HttpServletRequest request
    ) {
        Admin superAdmin = adminService.getTokenAdmin(request);

        //슈퍼관리자 권한 체크 (Entity 필드 확인 필요: getAdminIsSuper() == YnType.Y 등)
        if (superAdmin.getAdminIsSuper() != YnType.Y) {
            throw new UnauthorizedException("슈퍼관리자 권한이 없습니다.");
        }

        //Service 호출 및 응답
        return ResponseEntity.ok(new DataResponse<>(
                200,
                "OK",
                billingService.getBillingListByOrganization(organizationId)
        ));
    }

    /** 빌링 초과내역 조회 */
    @GetMapping("/{billingId}/overuse")
    public ResponseEntity<DataResponse<BillingDto.OveruseResponse>> getOveruseBillingDetails(
            @PathVariable Long billingId
    ) {
        return ResponseEntity.ok(new DataResponse<>(
                200,
                "초과요금 상세 조회 성공",
                billingService.getOveruseDetails(billingId)
        ));
    }

    /** 기관 상세 정보 조회 */
    @GetMapping("/{organizationId}")
    public ResponseEntity<DataResponse<SuperAdminDto.OrganizationDetailResponse>> getOrganizationDetail(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 상세 조회 성공",
                        superAdminOrganizationService.getOrganizationDetail(organizationId)));
    }

    /** 기관별 빌링내역 조회 */
    @GetMapping("/{organizationId}/billings")
    // 1. 메서드 리턴 타입 변경: Map -> BillingDto.PagedResponse
    public ResponseEntity<DataResponse<BillingDto.PagedResponse>> getBillings(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "DESC") String sort
    ) {

        PagingRequest pagingRequest = new PagingRequest();
        pagingRequest.setPage(page);
        pagingRequest.setSize(size);

        // 2. 서비스 호출 결과 타입 변경
        BillingDto.PagedResponse result = billingService.getBillingList(
                organizationId,
                status,
                sort,
                pagingRequest
        );

        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 빌링 목록 조회 성공", result)
        );
    }

    /** 빌링 상태 변경 */
    @PatchMapping("/billing/{billingId}/status")
    public ResponseEntity<DataResponse<BillingDto.StatusHistoryResponse>> updateBillingStatus(
            HttpServletRequest request,
            @PathVariable Long billingId,
            @Valid @RequestBody BillingDto.StatusUpdateRequest requestDto
    ) {
        Admin admin = adminService.getTokenAdmin(request);
        String changedBy = admin.getAdminLoginIdentifier();

        BillingDto.StatusHistoryResponse response =
                billingService.updateBillingStatus(billingId, requestDto, changedBy);

        return ResponseEntity.ok(
                new DataResponse<>(200, "빌링 상태 변경 성공", response)
        );
    }

    /** 특정 Billing의 상태 변경 로그 조회 */
    @GetMapping("/billing/{billingId}/status-histories")
    public ResponseEntity<DataResponse<List<BillingDto.StatusHistoryResponse>>> getBillingStatusHistories(
            @PathVariable Long billingId
    ) {
        List<BillingDto.StatusHistoryResponse> histories =
                billingService.getBillingStatusHistories(billingId);

        return ResponseEntity.ok(
                new DataResponse<>(200, "빌링 상태 변경 이력 조회 성공", histories)
        );
    }

    @GetMapping("/{organizationId}/billing")
    public ResponseEntity<?> getOrganizationBilling(
            @PathVariable Long organizationId, HttpServletRequest request) {

        Admin superAdmin = adminService.getTokenAdmin(request);
        if (!superAdmin.isSuperAdmin()) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }
        SuperAdminDto.BillingListResponse response =
                superAdminOrganizationService.getOrganizationBillingForSuperAdmin(organizationId);

        return ResponseEntity.ok(new DataResponse<>(200, "OK", response));
    }

    @GetMapping("/billing/{billingId}/overuse")
    public ResponseEntity<?> getBillingOveruseDetail(@PathVariable Long billingId,HttpServletRequest request) {

        Admin superAdmin = adminService.getTokenAdmin(request);
        if (!superAdmin.isSuperAdmin()) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }

        BillingOveruseResponse response = superAdminOrganizationService.getOveruseDetail(billingId);
        return ResponseEntity.ok(new DataResponse<>(200, "초과요금 상세 조회 성공", response));
    }

}
