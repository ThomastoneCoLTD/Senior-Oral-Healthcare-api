package com.kaii.dentix.domain.organization.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dto.AdminAutoLoginDto;
import com.kaii.dentix.domain.admin.dto.AdminListDto;
import com.kaii.dentix.domain.admin.dto.AdminPasswordResetDto;
import com.kaii.dentix.domain.admin.dto.AdminSignUpDto;
import com.kaii.dentix.domain.admin.dto.request.AdminModifyPasswordRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminSignUpRequest;
import com.kaii.dentix.domain.organization.application.OrganizationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations")
@CrossOrigin(origins = "*", allowedHeaders = "*")

public class OrganizationController {
    private final OrganizationService organizationService;

    //기관 등록
    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@RequestBody OrganizationRequest request) {
        log.info("▶ 기관 등록 요청 body: name={}, phone={}, plan={}",
                request.getOrganizationName(),
                request.getOrganizationPhoneNumber(),
                request.getSubscriptionPlanId());
        return ResponseEntity.ok(organizationService.createOrganization(request));
    }

    //기관 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    //기관 리스트 조회
    @GetMapping
    public ResponseEntity<Page<OrganizationResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(organizationService.getAllOrganizations(pageable));
    }

    //기관 수정
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable Long id,
            @RequestBody OrganizationUpdateRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    //기관 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> softDelete(@PathVariable Long id) {
        organizationService.softDelete(id);
        return ResponseEntity.ok("삭제되었습니다.");
    }

    //기관 삭제
    @DeleteMapping("/hard/{id}")
    public ResponseEntity<String> hardDelete(@PathVariable Long id) {
        organizationService.hardDelete(id);
        return ResponseEntity.ok("삭제되었습니다.");
    }

    /**
     * 기관의 구독상품 변경
     */
    @PutMapping("/{organizationId}/subscription/{subscriptionPlanId}")
    public DataResponse<OrganizationResponse> changeSubscriptionPlan(
            @PathVariable Long organizationId,
            @PathVariable Long subscriptionPlanId
    ) {
        OrganizationResponse response = organizationService.changeSubscriptionPlan(organizationId, subscriptionPlanId);
        return new DataResponse<>(response);
    }

    // 기관 가입정보 조회
    @GetMapping("/check-duplicate")
    public ResponseEntity<Boolean> checkDuplicate(
            @RequestParam String organizationName,
            @RequestParam String organizationPhoneNumber
    ) {
        boolean isDuplicate = organizationService.isDuplicate(organizationName, organizationPhoneNumber);
        return ResponseEntity.ok(isDuplicate);
    }
    /**
     * 기관번호로 기관 정보 조회 (회원가입 시 기관 확인용)
     */
    @GetMapping("/check/{organizationId}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@PathVariable Long organizationId) {
        OrganizationResponse response = organizationService.getCheckOrganizationById(organizationId);
        return ResponseEntity.ok(response);
    }
}


