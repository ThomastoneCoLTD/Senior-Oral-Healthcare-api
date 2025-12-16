package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.application.OrganizationUsageService;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.*;
import com.kaii.dentix.domain.organizationSubscriptionHistory.application.OrganizationSubscriptionHistoryService;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dto.OrganizationSubscriptionHistoryResponse;
import com.kaii.dentix.domain.subscription.dto.SubscriptionHistoryResponse;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/organization")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;
    private final AdminService adminService;
    private final OrganizationService organizationService;
    private final OralCheckService oralCheckService;
    private final AdminUserService adminUserService;
    private final OrganizationUsageService organizationUsageService;
    private final OrganizationHistoryRepository organizationHistoryRepository;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 기관등록
     */
//    @PostMapping
//    public ResponseEntity<OrganizationResponse> create(@RequestBody OrganizationRequest request) {
//        log.info("▶ 기관 등록 요청 body: name={}, phone={}, plan={}",
//                request.getOrganizationName(),
//                request.getOrganizationPhoneNumber(),
//                request.getSubscriptionPlanId());
//        return ResponseEntity.ok(organizationService.createOrganization(request));
//    }

    /** 일반관리자 - 기관등록 */
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @RequestBody OrganizationRequest request) {
        OrganizationResponse response = organizationService.createOrganization(request);
        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관 정보 조회 */
    @GetMapping("/my")
    public ResponseEntity<OrganizationReResponse> getMyOrganization(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        OrganizationReResponse response = adminOrganizationService.getMyOrganization(admin);
        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관 정보 수정 */
    @PutMapping("/{organizationId}")
    public SuccessResponse updateOrganization(
            HttpServletRequest httpServletRequest,
            @PathVariable Long organizationId,
            @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        //로그인한 관리자 정보 가져오기
        Admin admin = adminService.getTokenAdmin(httpServletRequest);

        //adminId까지 같이 넘김 (3개 인자)
        organizationService.updateOrganization(organizationId, request, admin.getAdminId());

        return new SuccessResponse(200, "기관 정보 수정 완료");
    }

    /** 일반관리자 - 본인 기관 수정 이력 조회 */
    @GetMapping("/{organizationId}/history")
    public DataResponse<List<OrganizationHistoryResponse>> getHistory(
            @PathVariable Long organizationId
    ) {
        List<OrganizationHistoryResponse> list =
                adminOrganizationService.getOrganizationHistory(organizationId);

        return new DataResponse<>(200, "기관 수정 이력 조회 성공", list);
    }

    /** SUPER_ADMIN 기관 단건 조회 */
    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable Long organizationId) {
        OrganizationResponse response = organizationService.getOrganizationById(organizationId);
        return ResponseEntity.ok(response);
    }

    /** SUPER_ADMIN 모든 기관 정보 조회
     * - 각 기관의 기본정보 + 구독정보 함께 반환
     */
    @GetMapping("/super")
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        log.info("🧾 [슈퍼관리자] 전체 기관 정보 조회 요청");

        List<OrganizationResponse> organizations = organizationService.getAllOrganizations();

        return ResponseEntity.ok(organizations);
    }



    @GetMapping("/usage/my")
    public ResponseEntity<?> getMyOrganizationUsage(HttpServletRequest request) {
        Long adminId = adminService.getTokenAdmin(request).getAdminId();
//        Admin admin = adminService.getTokenAdmin(request);

        OrganizationUsageResponse data =
                organizationUsageService.getMyOrganizationUsage(adminId);

        return ResponseEntity.ok(
                Map.of(
                        "rt", 200,
                        "rtMsg", "기관 사용자 사용량 조회 성공",
                        "response", data
                )
        );
    }
//    @GetMapping("/usage/my")
//    public DataResponse<OrganizationUsageResponse> getMyUsage(HttpServletRequest request) {
//        Long adminId = adminService.getTokenAdmin(request).getAdminId();
//        return new DataResponse<>(200, "기관 사용량 조회 성공",
//                organizationUsageService.getMyOrganizationUsage(adminId));
//    }
}