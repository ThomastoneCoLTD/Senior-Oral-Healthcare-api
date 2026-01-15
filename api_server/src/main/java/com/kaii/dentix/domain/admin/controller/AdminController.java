package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.domain.admin.dto.AdminDto;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/account")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 등록
     */
    @PostMapping(name = "관리자 등록")
    public DataResponse<AdminAuthDto.SignUpResponse> adminSignUp(
            @Valid @RequestBody AdminAuthDto.SignUpRequest request //DTO 교체
    ) {
        return new DataResponse<>(adminService.adminSignUp(request));
    }

    /**
     * 관리자 비밀번호 변경
     */
    @PutMapping(value = "/password", name = "관리자 비밀번호 변경")
    public SuccessResponse adminModifyPassword(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody AdminAuthDto.ModifyPasswordRequest request //DTO 교체
    ) {
        adminService.adminModifyPassword(httpServletRequest, request);
        return new SuccessResponse();
    }

    /**
     * 관리자 삭제
     */
    @DeleteMapping(name = "관리자 삭제")
    public SuccessResponse adminDelete(@RequestParam Long adminId) {
        adminService.adminDelete(adminId);
        return new SuccessResponse();
    }

    /**
     * 관리자 비밀번호 초기화
     */
    @PutMapping(value = "/reset-password", name = "관리자 비밀번호 초기화")
    public DataResponse<AdminAuthDto.ModifyPasswordRequest> adminPasswordReset(
            @RequestParam Long adminId
    ) {
        // Service에서 초기화된 비밀번호를 담은 객체(ModifyPasswordRequest)를 반환함
        return new DataResponse<>(adminService.adminPasswordReset(adminId));
    }

    /**
     * 관리자 목록 조회
     * (AdminListDto는 아직 통합되지 않았으므로 기존 유지)
     */
    @GetMapping(value = "/list", name = "관리자 목록 조회")
    public DataResponse<AdminDto.ListResponse> adminList( // 리턴 타입 변경
                                                          @ModelAttribute AdminDto.SearchRequest request // 파라미터 변경
    ) {
        return new DataResponse<>(adminService.adminList(request));
    }
    /**
     * 관리자 자동 로그인
     */
    @PutMapping(value = "/auto-login", name = "관리자 자동 로그인")
    public DataResponse<AdminAuthDto.AutoLoginResponse> adminAutoLogin(
            HttpServletRequest httpServletRequest
    ) {
        //DTO 교체 (AdminAutoLoginDto -> AdminAuthDto.AutoLoginResponse)
        return new DataResponse<>(adminService.adminAutoLogin(httpServletRequest));
    }
}