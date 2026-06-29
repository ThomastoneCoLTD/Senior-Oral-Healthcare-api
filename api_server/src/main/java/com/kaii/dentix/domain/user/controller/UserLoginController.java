package com.kaii.dentix.domain.user.controller;

import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dto.*;
import com.kaii.dentix.domain.user.dto.request.*;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/login")
public class UserLoginController {

    private final UserLoginService userLoginService;

    /** 회원 인증 (가입 여부 확인) */
    @PostMapping("/verify")
    public DataResponse<UserDto.VerifyResponse> userVerify(@Valid @RequestBody UserDto.VerifyRequest request) {
        return new DataResponse<>(userLoginService.userVerify(request));
    }

    /** Check whether a phone number is already registered before sign-up. */
    @PostMapping("/phone-check")
    public DataResponse<UserDto.VerifyResponse> userPhoneCheck(@RequestBody Map<String, String> request) {
        return new DataResponse<>(userLoginService.userPhoneCheck(request.get("userPhoneNumber")));
    }

    /** 회원가입 */
    @PostMapping("/signUp")
    public DataResponse<UserDto.SignUpResponse> userSignUp(@Valid @RequestBody UserDto.SignUpRequest request) {
        return new DataResponse<>(userLoginService.userSignUp(request));
    }

    /** DID 간편 회원가입 */
    @PostMapping("/signUp/did")
    public DataResponse<UserDto.SignUpResponse> userDidSignUp(@Valid @RequestBody UserDto.DidSignUpRequest request) {
        return new DataResponse<>(userLoginService.userDidSignUp(request));
    }

    /** DID 로그인 */
    @PostMapping("/did")
    public DataResponse<UserDto.LoginResponse> userDidLogin(@Valid @RequestBody UserDto.DidLoginRequest request) {
        return new DataResponse<>(userLoginService.userDidLogin(request));
    }

    /** 아이디 중복 확인 */
    @GetMapping("/loginIdentifier-check")
    public SuccessResponse loginIdCheck(@RequestParam @NotBlank String userLoginIdentifier) {
        userLoginService.loginIdCheck(userLoginIdentifier);
        return new SuccessResponse();
    }

    /** 비밀번호 찾기 (질문/답변 검증) */
    @PostMapping("/find-password")
    public DataResponse<UserDto.FindPasswordResponse> userFindPassword(@Valid @RequestBody UserDto.FindPasswordRequest request) {
        return new DataResponse<>(userLoginService.userFindPassword(request));
    }

    /** 비밀번호 재설정 (인증 후) */
    @PutMapping("/password")
    public SuccessResponse userModifyPassword(@RequestBody UserDto.ModifyPasswordRequest request, @RequestParam Long userId) {
        userLoginService.userModifyPassword(userId, request);
        return new SuccessResponse();
    }

    /** AccessToken 재발급 */
    @PutMapping("/access-token")
    public DataResponse<UserDto.AccessTokenResponse> accessTokenReissue(HttpServletRequest request) {
        return new DataResponse<>(userLoginService.accessTokenReissue(request));
    }
}
