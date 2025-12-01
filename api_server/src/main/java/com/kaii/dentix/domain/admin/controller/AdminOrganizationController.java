package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminListDto;
import com.kaii.dentix.domain.admin.dto.AdminOrganizationUsageResponse;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.application.OrganizationUsageService;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.dto.OrganizationHistoryResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationUpdateRequest;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
     * 일반관리자 - 기관 사용자 사용량 조회
     */
    @GetMapping("/user")
    public ResponseEntity<DataResponse<Map<String, Object>>> getUsage(HttpServletRequest request) {
        return ResponseEntity.ok(adminUserService.getOrganizationUserUsage(request));
    }

//private final AdminOrganizationService adminOrganizationService;
//
//    @GetMapping("/organization")
//    public ResponseEntity<?> getMyOrganization(HttpServletRequest request)( {
//        return ResponseEntity.ok(adminOrganizationService.getMyOrganization(request));
//    }
//    @GetMapping("/usage")
//    public ResponseEntity<?> getAllOrganizationUsage(HttpServletRequest request) {
//        Admin admin = adminService.getTokenAdmin(request);
//        List<AdminOrganizationUsageResponse> result = adminOrganizationService.getAllOrganizationUsage(request, admin);
//        return ResponseEntity.ok(result);
//    }

    /**
     * ✅ 기관 등록 + 구독 플랜 설정
     * - 관리자(Admin) 로그인 상태에서 기관 등록 가능
     * - 기관 기본정보 + 구독상품 동시 등록
     */
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @RequestBody OrganizationRequest request) {

        log.info("▶ [기관 등록 요청] name={}, phone={}, email={}, planId={}",
                request.getOrganizationName(),
                request.getOrganizationPhoneNumber(),
                request.getOrganizationEmail(),
                request.getSubscriptionPlanId());

        OrganizationResponse response = organizationService.createOrganization(request);

        return ResponseEntity.ok(response);
    }


    /** ADMIN 본인 기관 정보 조회 */
    @GetMapping("/my")
    public ResponseEntity<OrganizationResponse> getMyOrganization(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        Organization organization = adminService.getMyOrganization(admin);

        OrganizationResponse response = OrganizationResponse.from(organization);
        return ResponseEntity.ok(response);
    }

    /** SUPER_ADMIN 기관 단건 조회 */
    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable Long organizationId) {
        OrganizationResponse response = organizationService.getOrganizationById(organizationId);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 슈퍼관리자용 - 모든 기관 정보 조회
     * - 각 기관의 기본정보 + 구독정보 함께 반환
     */
    @GetMapping("/super")
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        log.info("🧾 [슈퍼관리자] 전체 기관 정보 조회 요청");

        List<OrganizationResponse> organizations = organizationService.getAllOrganizations();

        return ResponseEntity.ok(organizations);
    }

    /** 기관 정보 수정 */
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

    /** 기관 수정 이력 조회 */
    @GetMapping("/history/{organizationId}")
    public DataResponse<List<OrganizationHistoryResponse>> getHistory(
            @PathVariable Long organizationId
    ) {
        List<OrganizationHistoryResponse> list =
                adminOrganizationService.getOrganizationHistory(organizationId);

        return new DataResponse<>(200, "기관 수정 이력 조회 성공", list);
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
                        "response", data  // 👈 반드시 여기에 넣어야 프론트가 읽음!
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