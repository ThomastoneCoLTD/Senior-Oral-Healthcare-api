package com.kaii.dentix.domain.auth.controller;

import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.domain.auth.dto.AuthDto;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor

public class AuthController {

    private final AdminLoginService adminLoginService;
    private final UserLoginService userLoginService;

    @PostMapping
    public ResponseEntity<?> login(
            HttpServletRequest request,
            @Valid @RequestBody AuthDto.IntegratedLoginRequest loginRequest
    ) {
        String userType = loginRequest.getUserType().toLowerCase();

        switch (userType) {
            case "admin":
                if (loginRequest.getPassword() == null || loginRequest.getPassword().isBlank()) {
                    throw new BadRequestApiException("관리자 비밀번호를 입력해주세요.");
                }

                // 1. Admin DTO로 변환
                AdminAuthDto.LoginRequest adminReq = AdminAuthDto.LoginRequest.builder()
                        .loginId(loginRequest.getLoginId())
                        .password(loginRequest.getPassword())
                        .build();

                // 2. Admin Service 호출 및 응답
                return ResponseEntity.ok(
                        new DataResponse<>(adminLoginService.login(adminReq))
                );

            case "user":
                // 1. 통합 DTO(UserDto.LoginRequest)로 변환
                // 주의: UserDto.LoginRequest의 필드명(userLoginIdentifier, userPassword)에 맞춰서 매핑합니다.
                UserDto.LoginRequest userReq = UserDto.LoginRequest.builder()
                        .userLoginIdentifier(loginRequest.getLoginId()) // loginId -> userLoginIdentifier
                        .userPassword(loginRequest.getPassword())       // password -> userPassword
                        .build();

                // 2. User Service 호출 및 응답
                // userLoginService.userLogin()은 UserDto.LoginResponse를 반환합니다.
                return ResponseEntity.ok(
                        new DataResponse<>(userLoginService.userLogin(userReq))
                );
            default:
                throw new IllegalArgumentException("지원하지 않는 사용자 타입입니다: " + userType);
        }
    }
}
