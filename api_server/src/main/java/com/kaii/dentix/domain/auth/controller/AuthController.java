package com.kaii.dentix.domain.auth.controller;

import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.domain.auth.dto.AuthDto;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dto.UserAuthDto;
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
                // 1. User DTO로 변환
                UserAuthDto.LoginRequest userReq = UserAuthDto.LoginRequest.builder()
                        .loginId(loginRequest.getLoginId())
                        .password(loginRequest.getPassword())
                        .build();

                // 2. User Service 호출 및 응답
                return ResponseEntity.ok(
                        new DataResponse<>(userLoginService.userLogin(userReq))
                );

            default:
                throw new IllegalArgumentException("지원하지 않는 사용자 타입입니다: " + userType);
        }
    }
}
//    @PostMapping(value = "/login", name = "관리자 로그인")
//    public DataResponse<AdminLoginDto> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
//        DataResponse<AdminLoginDto> response = new DataResponse<>(adminLoginService.adminLogin(request));
//        return response;
//    }
//
//    /**
//     *  사용자 로그인
//     */
//    @PostMapping(name = "사용자 로그인")
//    public DataResponse<UserLoginDto> userLogin(HttpServletRequest httpServletRequest, @Valid @RequestBody UserLoginRequest request){
//        DataResponse<UserLoginDto> response = new DataResponse<>(userLoginService.userLogin(httpServletRequest, request));
//        return response;
//    }
//    @PostMapping("/login")
//    public DataResponse<LoginDto> login(
//            HttpServletRequest httpServletRequest,
//            @Valid @RequestBody LoginRequestDto dto
//    ) {
//        if ("ADMIN".equalsIgnoreCase(dto.getUserType())) {
//            // 관리자 로그인 처리
//            AdminLoginRequest adminReq = AdminLoginRequest.builder()
//                    .adminLoginIdentifier(dto.getLoginIdentifier())
//                    .adminPassword(dto.getPassword())
//                    .build();
//
//            AdminLoginDto adminLoginDto = adminLoginService.adminLogin(adminReq);
//
//            // AdminLoginDto → LoginDto 변환
//            LoginDto loginDto = LoginDto.builder()
//                    .id(adminLoginDto.getAdminId())
//                    .name(adminLoginDto.getAdminName())
//                    .userType("ADMIN")
//                    .isFirstLogin(adminLoginDto.getIsFirstLogin())
//                    .adminIsSuper(adminLoginDto.getAdminIsSuper())
//                    .accessToken(adminLoginDto.getAccessToken())
//                    .refreshToken(adminLoginDto.getRefreshToken())
//                    .build();
//
//            return new DataResponse<>(loginDto);
//
//        } else if ("USER".equalsIgnoreCase(dto.getUserType())) {
//            // 사용자 로그인 처리
//            UserLoginRequest userReq = UserLoginRequest.builder()
//                    .userLoginIdentifier(dto.getLoginIdentifier())
//                    .userPassword(dto.getPassword())
//                    .build();
//
//            UserLoginDto userLoginDto = userLoginService.userLogin(httpServletRequest, userReq);
//
//            // UserLoginDto → LoginDto 변환
//            LoginDto loginDto = LoginDto.builder()
//                    .id(userLoginDto.getUserId())
//                    .name(userLoginDto.getUserName())
//                    .userType("USER")
//                    .accessToken(userLoginDto.getAccessToken())
//                    .refreshToken(userLoginDto.getRefreshToken())
//                    .build();
//
//            return new DataResponse<>(loginDto);
//
//        } else {
//            throw new IllegalArgumentException("userType은 USER 또는 ADMIN만 허용됩니다.");
//        }
//    }
//}
