package com.kaii.dentix.domain.user.controller;

import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserModifyServiceAgreeDto;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreementResponse;
import com.kaii.dentix.domain.userServiceAgreement.dto.request.UserModifyServiceAgreeRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    /** 자동 로그인 (토큰 갱신) */
    @PutMapping("/auto-login")
    public DataResponse<UserDto.TokenResponse> userAutoLogin(HttpServletRequest request) {
        return new DataResponse<>(userService.userAutoLogin(request));
    }

    /** 회원 정보 조회 */
    @GetMapping("/info")
    public DataResponse<UserDto.InfoResponse> getUserInfo(HttpServletRequest request) {
        return new DataResponse<>(userService.getUserInfo(request));
    }

    /** 회원 정보 수정 */
    @PutMapping
    public DataResponse<UserDto.InfoResponse> userModifyInfo(HttpServletRequest request, @Valid @RequestBody UserDto.ModifyInfoRequest dto) {
        return new DataResponse<>(userService.userModifyInfo(request, dto));
    }

    /** 비밀번호 확인 */
    @PostMapping("/password-verify")
    public SuccessResponse userPasswordVerify(HttpServletRequest request, @Valid @RequestBody UserDto.PasswordVerifyRequest dto) {
        userService.userPasswordVerify(request, dto);
        return new SuccessResponse();
    }

    /** 비밀번호 변경 */
    @PutMapping("/password")
    public SuccessResponse userModifyPassword(HttpServletRequest request, @Valid @RequestBody UserDto.ModifyPasswordRequest dto) {
        userService.userModifyPassword(request, dto);
        return new SuccessResponse();
    }

    /** QnA 수정 */
    @PutMapping("/qna")
    public DataResponse<UserDto.ModifyQnAResponse> userModifyQnA(HttpServletRequest request, @Valid @RequestBody UserDto.ModifyQnARequest dto) {
        return new DataResponse<>(userService.userModifyQnA(request, dto));
    }

    /** 서비스 목록 변경 */
    @PostMapping("/service")
    public ResponseEntity<?> updateUserServices(HttpServletRequest request, @RequestBody UserDto.ServiceUpdateRequest dto) {
        return ResponseEntity.ok(new DataResponse<>(userService.updateUserServices(request, dto)));
    }

    /** 서비스 이용 동의 수정 */
    @PutMapping("/service-agreement")
    public DataResponse<UserModifyServiceAgreeDto> userModifyServiceAgree(HttpServletRequest request, @Valid @RequestBody UserModifyServiceAgreeRequest dto) {
        return new DataResponse<>(userService.userModifyServiceAgree(request, dto));
    }

    /** 서비스 동의 내역 조회 */
    @GetMapping("/serviceAgreement")
    public DataResponse<List<UserServiceAgreementResponse>> getUserServiceAgreements(HttpServletRequest request) {
        return new DataResponse<>(userService.getUserServiceAgreements(request));
    }

    /** 로그아웃 */
    @PutMapping("/logout")
    public SuccessResponse userLogout(HttpServletRequest request) {
        userService.userLogout(request);
        return new SuccessResponse();
    }

    /** 회원탈퇴 */
    @DeleteMapping
    public SuccessResponse userRevoke(HttpServletRequest request) {
        userService.userRevoke(request);
        return new SuccessResponse();
    }
}
