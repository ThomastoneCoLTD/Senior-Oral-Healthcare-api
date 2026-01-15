package com.kaii.dentix.domain.superAdmin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminStatisticService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.dto.BillingDto;
import com.kaii.dentix.domain.billing.dto.BillingOveruseResponse;
import com.kaii.dentix.domain.billing.dto.BillingStatusHistoryResponse;
import com.kaii.dentix.domain.billing.dto.BillingStatusUpdateRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.superAdmin.application.SuperAdminOrganizationService;
import com.kaii.dentix.domain.superAdmin.dto.*;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.dto.PagingRequest;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;

    /** 사용자 통계 */
    @GetMapping("/statistics")
    // DataResponse<SuperAdminStatisticDto.TotalUserStats> 로 변경
    public DataResponse<SuperAdminStatisticDto.TotalUserStats> getSuperAdminStatistics(
            HttpServletRequest request
    ) {
        Admin admin = adminService.getTokenAdmin(request);

        return new DataResponse<>(
                superAdminOrganizationService.getSuperAdminTotalUserStatistics(admin)
        );
    }
    /** 전체 기관 목록 조회 */
    @GetMapping("/all")
    public ResponseEntity<DataResponse<List<OrganizationListResponse>>> getAllOrganizations() {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 목록 조회 성공",
                        superAdminOrganizationService.getAllOrganizations()));
    }

    /** 특정 기관의 사용자 사용량 조회 */
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

    /** 특정 기관의 사용자 조회 */
    @GetMapping("/{organizationId}/users")
    public ResponseEntity<DataResponse<Page<AdminUserDto.Info>>> getUsersByOrganization( // 1. 리턴 타입 변경
                                                                                         @PathVariable Long organizationId,
                                                                                         @ModelAttribute AdminUserDto.SearchRequest request // 2. 파라미터 타입 변경
    ) {
        request.setOrganizationId(organizationId);

        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 사용자 목록 조회 성공",
                        adminUserService.getUsersByOrganization(request))
        );
    }

    /** 기관별 현재 구독 상품 조회 */
    @GetMapping("/{organizationId}/subscription/current")
    public ResponseEntity<DataResponse<SuperAdminCurrentSubscriptionDto>> getCurrentSubscription(
            @PathVariable Long organizationId) {

        SuperAdminCurrentSubscriptionDto dto =
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
    public ResponseEntity<DataResponse<BillingDto.ListResponse>> getBillingList( // 1. 리턴 타입 변경
                                                                                 @PathVariable Long organizationId,
                                                                                 HttpServletRequest request
    ) {
        Admin superAdmin = adminService.getTokenAdmin(request);

        // 2. 슈퍼관리자 권한 체크 (Entity 필드 확인 필요: getAdminIsSuper() == YnType.Y 등)
        // superAdmin.isSuperAdmin() 메서드가 없다면 아래와 같이 수정하세요.
        if (superAdmin.getAdminIsSuper() != YnType.Y) {
            throw new UnauthorizedException("슈퍼관리자 권한이 없습니다.");
        }

        // 3. Service 호출 및 응답
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
    public ResponseEntity<DataResponse<OrganizationDetailResponse>> getOrganizationDetail(
            @PathVariable Long organizationId) {
        return ResponseEntity.ok(
                new DataResponse<>(200, "기관 상세 조회 성공",
                        superAdminOrganizationService.getOrganizationDetail(organizationId)));
    }



    /**
     * 기관별 빌링 내역 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public BillingDto.PagedResponse getBillingList(Long orgId, String status, String sort, PagingRequest pagingRequest) {

        // 1. 정렬 조건 설정
        Sort pageableSort = "ASC".equalsIgnoreCase(sort)
                ? Sort.by("periodStart").ascending()
                : Sort.by("periodStart").descending();

        Pageable pageable = PageRequest.of(pagingRequest.getPage() - 1, pagingRequest.getSize(), pageableSort);

        // 2. 검색 조건(Status)에 따라 조회
        Page<Billing> result;
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        if ("ALL".equalsIgnoreCase(status)) {
            // 전체 조회
            result = billingRepository.findByOrganization(org, pageable);
        } else {
            // 상태별 조회
            try {
                BillingStatus statusEnum = BillingStatus.valueOf(status.toUpperCase());
                result = billingRepository.findByOrganizationAndBillingStatus(org, statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 빌링 상태입니다: " + status);
            }
        }

        // 3. DTO 변환
        List<BillingDto.Summary> content = result.getContent().stream()
                .map(BillingDto.Summary::from)
                .toList();

        PagingDTO pagingInfo = PagingDTO.builder()
                .number(result.getNumber() + 1) // 1-based page index로 변환
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();

        return BillingDto.PagedResponse.builder()
                .paging(pagingInfo)
                .content(content)
                .build();
    }

    /** 빌링 상태 변경  */
    @PatchMapping("/billing/{billingId}/status")
    public ResponseEntity<DataResponse<BillingStatusHistoryResponse>> updateBillingStatus(
            HttpServletRequest request,
            @PathVariable Long billingId,
            @Valid @RequestBody BillingStatusUpdateRequest requestDto
    ) {
        Admin admin = adminService.getTokenAdmin(request);
        String changedBy = admin.getAdminLoginIdentifier();
        BillingStatusHistoryResponse response =
                billingService.updateBillingStatus(billingId, requestDto, changedBy);

        return ResponseEntity.ok(
                new DataResponse<>(200, "빌링 상태 변경 성공", response)
        );
    }

    /**특정 Billing의 상태 변경 로그 조회 */
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

    @GetMapping("/{organizationId}/billing")
    public ResponseEntity<?> getOrganizationBilling(
            @PathVariable Long organizationId, HttpServletRequest request) {

        Admin superAdmin = adminService.getTokenAdmin(request);
        if (!superAdmin.isSuperAdmin()) {
            return ResponseEntity.status(403).body("권한이 없습니다.");
        }
        SuperAdminBillingListResponse response =
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
