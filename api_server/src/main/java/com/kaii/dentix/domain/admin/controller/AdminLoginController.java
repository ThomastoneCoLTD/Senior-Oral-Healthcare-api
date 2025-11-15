package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.dto.AdminFindPasswordDto;
import com.kaii.dentix.domain.admin.dto.AdminLoginDto;
import com.kaii.dentix.domain.admin.dto.request.AdminFindPasswordRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminLoginRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminLoginController {

    private final AdminLoginService adminLoginService;

    /**
     * 관리자 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<DataResponse<AdminLoginDto>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginDto response = adminLoginService.login(request);
        return ResponseEntity.ok(new DataResponse<>(200, "로그인 성공", response));
    }

    @PostMapping(value = "/find-password", name = "어드민 비밀번호 찾기")
    public DataResponse<AdminFindPasswordDto> adminFindPassword(
            @Valid @RequestBody AdminFindPasswordRequest request) {

        return new DataResponse<>(adminLoginService.adminFindPassword(request));
    }

}
