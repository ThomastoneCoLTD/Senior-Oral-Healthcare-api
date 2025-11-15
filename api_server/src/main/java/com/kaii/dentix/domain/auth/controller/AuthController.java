package com.kaii.dentix.domain.auth.controller;

import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.dto.AdminLoginDto;
import com.kaii.dentix.domain.admin.dto.request.AdminLoginRequest;
import com.kaii.dentix.domain.auth.dto.LoginDto;
import com.kaii.dentix.domain.auth.dto.LoginRequestDto;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dto.UserLoginDto;
import com.kaii.dentix.domain.user.dto.request.UserLoginRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor

public class AuthController {

    private final AdminLoginService adminLoginService;
    private final UserLoginService userLoginService;

    @PostMapping
    public ResponseEntity<?> login(
            HttpServletRequest request,
            @Valid @RequestBody LoginRequestDto loginRequest
    ) {
        String userType = loginRequest.getUserType().toLowerCase();

        switch (userType) {
            case "admin":
                AdminLoginRequest adminReq = new AdminLoginRequest();
                adminReq.setAdminLoginIdentifier(loginRequest.getLoginId());
                adminReq.setAdminPassword(loginRequest.getPassword());

                AdminLoginDto adminDto = adminLoginService.login(adminReq);
                return ResponseEntity.ok(adminDto);

            case "user":
                UserLoginRequest userReq = new UserLoginRequest();
                userReq.setUserLoginIdentifier(loginRequest.getLoginId());
                userReq.setUserPassword(loginRequest.getPassword());
                System.out.println(">>> loginId: " + loginRequest.getLoginId());
                System.out.println(">>> mapped userLoginIdentifier: " + userReq.getUserLoginIdentifier());
                UserLoginDto userDto = userLoginService.userLogin(request, userReq);
                return ResponseEntity.ok(userDto);

            default:
                return ResponseEntity.badRequest()
                        .body("지원하지 않는 userType입니다: " + loginRequest.getUserType());
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
