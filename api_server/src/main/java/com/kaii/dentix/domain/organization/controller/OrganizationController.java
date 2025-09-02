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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations")
@CrossOrigin(origins = "*", allowedHeaders = "*")

public class OrganizationController {
    private final OrganizationService organizationService;

    //기관 등록
    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@RequestBody OrganizationRequest request) {
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
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<String> hardDelete(@PathVariable Long id) {
        organizationService.hardDelete(id);
        return ResponseEntity.ok("삭제되었습니다.");
    }
}


