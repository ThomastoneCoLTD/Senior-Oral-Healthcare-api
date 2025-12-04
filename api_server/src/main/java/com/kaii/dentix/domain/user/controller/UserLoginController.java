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

@RestController
@RequiredArgsConstructor
@RequestMapping("/login")
public class UserLoginController {

    private final UserLoginService userLoginService;

    /**
     *  사용자 회원인증
     */
    @PostMapping(value = "/verify", name = "사용자 회원 인증")
    public DataResponse<UserVerifyDto> userVerify(@Valid @RequestBody UserVerifyRequest request){
        DataResponse<UserVerifyDto> response = new DataResponse<>(userLoginService.userVerify(request));
        return response;
    }

    /**
     * 사용자 회원가입
     */
    @PostMapping(value = "/signUp", name = "사용자 회원가입")
    public DataResponse<UserSignUpDto> userSignUp(HttpServletRequest httpServletRequest, @Valid @RequestBody UserSignUpRequest request){
        DataResponse<UserSignUpDto> response = new DataResponse<>(userLoginService.userSignUp(httpServletRequest, request));
        return response;
    }

    /**
     *  아이디 중복 확인
     */
    @GetMapping(value = "/loginIdentifier-check", name = "아이디 중복 확인")
    public SuccessResponse loginIdCheck(@RequestParam @NotBlank @Size(min = 4, max = 12) String userLoginIdentifier){
        userLoginService.loginIdCheck(userLoginIdentifier);
        return new SuccessResponse();
    }

    /**
     *  사용자 비밀번호 찾기
     */
    @PostMapping(value = "/find-password", name = "사용자 비밀번호 찾기")
    public DataResponse<UserFindPasswordDto> userFindPassword(@Valid @RequestBody UserFindPasswordRequest request){
        DataResponse<UserFindPasswordDto> response = new DataResponse<>(userLoginService.userFindPassword(request));
        return response;
    }

    /**
     *  사용자 비밀번호 재설정
     */
    @PutMapping(value = "/password", name = "사용자 비밀번호 재설정")
    public SuccessResponse userModifyPassword(@Valid @RequestBody UserModifyPasswordRequest request){
        userLoginService.userModifyPassword(request);
        return new SuccessResponse();
    }

    /**
     * AccessToken 재발급
     */
    @PutMapping(value = "/access-token", name = "AccessToken 재발급")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public DataResponse<AccessTokenDto> accessTokenReissue(HttpServletRequest httpServletRequest) {
        DataResponse<AccessTokenDto> response = new DataResponse<>(userLoginService.accessTokenReissue(httpServletRequest));
        return response;
    }
}
