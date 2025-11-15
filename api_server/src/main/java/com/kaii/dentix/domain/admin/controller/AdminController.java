package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dto.*;
import com.kaii.dentix.domain.admin.dto.request.AdminModifyPasswordRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminSignUpRequest;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationUpdateRequest;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/account")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    private final AdminService adminService;
    private final AdminLoginService adminLoginService;
    private final OrganizationService organizationService;
    /**
     *  관리자 등록
     */
    @PostMapping(name = "관리자 등록")
    public DataResponse<AdminSignUpDto> adminSignUp(@Valid @RequestBody AdminSignUpRequest request){
        DataResponse<AdminSignUpDto> response = new DataResponse<>(adminService.adminSignUp(request));
        return response;
    }

    /**
     *  관리자 비밀번호 변경
     */
    @PutMapping(value = "/password", name = "관리자 비밀번호 변경")
    public SuccessResponse adminModifyPassword(HttpServletRequest httpServletRequest, @Valid @RequestBody AdminModifyPasswordRequest request){
        adminService.adminModifyPassword(httpServletRequest, request);
        return new SuccessResponse();
    }
//    @PutMapping(value = "/reset-password", name = "관리자 비밀번호 재설정")
//    public SuccessResponse adminResetPassword(
//            @Valid @RequestBody AdminResetPasswordRequest request) {
//
//        adminService.adminResetPassword(request);
//        return new SuccessResponse();
//    }
    /**
     *  관리자 삭제
     */
    @DeleteMapping(name = "관리자 삭제")
    public SuccessResponse adminDelete(@RequestParam Long adminId){
        adminService.adminDelete(adminId);
        return new SuccessResponse();
    }

    /**
     *  관리자 비밀번호 초기화
     */
    @PutMapping(value = "/reset-password", name = "관리자 비밀번호 초기화")
    public DataResponse<AdminPasswordResetDto> adminPasswordReset(@RequestParam Long adminId){
        DataResponse<AdminPasswordResetDto> response = new DataResponse<>(adminService.adminPasswordReset(adminId));
        return response;
    }

    /**
     *  관리자 목록 조회
     */
    @GetMapping(value = "/list", name = "관리자 목록 조회")
    public DataResponse<AdminListDto> adminList(PageAndSizeRequest request){
        DataResponse<AdminListDto> response = new DataResponse<>(adminService.adminList(request));
        return response;
    }

    /**
     *  관리자 자동 로그인
     */
    @PutMapping(value = "/auto-login", name = "관리자 자동 로그인")
    public DataResponse<AdminAutoLoginDto> adminAutoLogin(HttpServletRequest httpServletRequest){
        DataResponse<AdminAutoLoginDto> response = new DataResponse<>(adminService.adminAutoLogin(httpServletRequest));
        return response;
    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<OrganizationResponse> update(
//            @PathVariable Long id,
//            @RequestBody OrganizationUpdateRequest request) {
//        return ResponseEntity.ok(organizationService.update(id, request));
//    }



}
