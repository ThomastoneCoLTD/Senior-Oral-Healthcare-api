package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.application.OrganizationUsageService;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
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
    private final AdminService adminService;
    private final OrganizationService organizationService;
    private final AdminOrganizationService adminOrganizationService;
    private final OrganizationUsageService organizationUsageService;

    /** 일반관리자 - 기관등록 */
    @PostMapping
    public ResponseEntity<OrganizationDto.Response> createOrganization(
            @RequestBody OrganizationDto.Request request
    ){
        OrganizationDto.Response response = organizationService.createOrganization(request);
        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관 정보 조회 */
    @GetMapping("/my")
    public ResponseEntity<OrganizationDto.Response> getMyOrganization(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);

        OrganizationDto.Response response = adminOrganizationService.getMyOrganization(admin);

        return ResponseEntity.ok(response);
    }

    /** 일반관리자 - 본인 기관 정보 수정 */
    @PutMapping("/{organizationId}")
    public SuccessResponse updateOrganization(
            HttpServletRequest httpServletRequest,
            @PathVariable Long organizationId,
            @Valid @RequestBody OrganizationDto.UpdateRequest request
    ) {
        Admin admin = adminService.getTokenAdmin(httpServletRequest);
        organizationService.updateOrganization(organizationId, request, admin.getAdminId());

        return new SuccessResponse(200, "기관 정보 수정 완료");
    }

    /** 일반관리자 - 본인 기관 수정 이력 조회 */
    @GetMapping("/{organizationId}/history")
    public DataResponse<List<OrganizationDto.HistoryResponse>> getHistory(
            @PathVariable Long organizationId
    ) {
        // AdminOrganizationService도 OrganizationDto.HistoryResponse 리스트를 반환하도록 수정되어야 합니다.
        List<OrganizationDto.HistoryResponse> list =
                adminOrganizationService.getOrganizationHistory(organizationId);

        return new DataResponse<>(200, "기관 수정 이력 조회 성공", list);
    }

    /** SUPER_ADMIN 기관 단건 조회 */
    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationDto.Response> getOrganization(@PathVariable Long organizationId) {
        OrganizationDto.Response response = organizationService.getOrganizationById(organizationId);
        return ResponseEntity.ok(response);
    }

    /** SUPER_ADMIN 모든 기관 정보 조회 */
    @GetMapping("/super")
    public ResponseEntity<List<OrganizationDto.Response>> getAllOrganizations() {
        log.info("[슈퍼관리자] 전체 기관 정보 조회 요청");
        List<OrganizationDto.Response> organizations = organizationService.getAllOrganizations();

        return ResponseEntity.ok(organizations);
    }

    /** 기관 사용자 사용량 조회 */
    @GetMapping("/usage/my")
    public ResponseEntity<?> getMyOrganizationUsage(HttpServletRequest request) {
        Long adminId = adminService.getTokenAdmin(request).getAdminId();

        OrganizationDto.UsageResponse data =
                organizationUsageService.getMyOrganizationUsage(adminId);

        return ResponseEntity.ok(
                Map.of(
                        "rt", 200,
                        "rtMsg", "기관 사용자 사용량 조회 성공",
                        "response", data
                )
        );
    }
}