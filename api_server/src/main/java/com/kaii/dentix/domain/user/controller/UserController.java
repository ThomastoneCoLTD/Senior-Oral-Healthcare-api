package com.kaii.dentix.domain.user.controller;

import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dto.*;
import com.kaii.dentix.domain.user.dto.request.*;
import com.kaii.dentix.domain.userServiceAgreement.dao.UserServiceAgreementRepository;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    /**
     *  사용자 자동 로그인
     */
    @PutMapping(value = "/auto-login", name = "사용자 자동 로그인")
    public DataResponse<UserLoginDto> userAutoLogin(HttpServletRequest servletRequest, @Valid @RequestBody UserAutoLoginRequest request){
        DataResponse<UserLoginDto> response = new DataResponse<>(userService.userAutoLogin(servletRequest, request));
        return response;
    }

    /**
     *  사용자 비밀번호 확인
     */
    @PostMapping(value = "/password-verify", name = "사용자 비밀번호 확인")
    public SuccessResponse userPasswordVerify(HttpServletRequest httpServletRequest, @Valid @RequestBody UserPasswordVerifyRequest request){
        userService.userPasswordVerify(httpServletRequest, request);
        return new SuccessResponse();
    }

    /**
     *  사용자 보안정보수정 - 비밀번호 변경
     */
    @PutMapping(value = "/password", name = "사용자 보안정보수정 - 비밀번호 변경")
    public SuccessResponse userModifyPassword(HttpServletRequest httpServletRequest, @Valid @RequestBody UserInfoModifyPasswordRequest request){
        userService.userModifyPassword(httpServletRequest, request);
        return new SuccessResponse();
    }

    /**
     *  사용자 보안정보수정 - 질문과 답변 수정
     */
    @PutMapping(value = "/qna", name = "사용자 보안정보수정 - 질문과 답변 수정")
    public DataResponse<UserInfoModifyQnADto> userModifyQnA(HttpServletRequest httpServletRequest, @Valid @RequestBody UserInfoModifyQnARequest request){
        DataResponse<UserInfoModifyQnADto> response = new DataResponse<>(userService.userModifyQnA(httpServletRequest, request));
        return response;
    }

    /**
     *  사용자 회원 정보 수정
     */
    @PutMapping(name = "사용자 회원 정보 수정")
    public DataResponse<UserInfoModifyDto> userModifyInfo(HttpServletRequest httpServletRequest, @Valid @RequestBody UserInfoModifyRequest request){
        DataResponse<UserInfoModifyDto> response = new DataResponse<>(userService.userModifyInfo(httpServletRequest, request));
        return response;
    }

    /**
     *  사용자 서비스 이용 동의 수정
     */
    @PutMapping(value = "/service-agreement", name = "사용자 서비스 이용 동의 수정")
    public DataResponse<UserModifyServiceAgreeDto> userModifyServiceAgree(HttpServletRequest httpServletRequest, @Valid @RequestBody UserModifyServiceAgreeRequest request){
        DataResponse<UserModifyServiceAgreeDto> response = new DataResponse<>(userService.userModifyServiceAgree(httpServletRequest, request));
        return response;
    }

    /**
     *  사용자 회원 정보 조회
     */
    @GetMapping(name = "사용자 회원정보 조회")
    public DataResponse<UserInfoDto> userInfo(HttpServletRequest httpServletRequest){
        DataResponse<UserInfoDto> response = new DataResponse<>(userService.userInfo(httpServletRequest));
        return response;
    }
    @GetMapping("/info")
    public DataResponse<UserInfoDto> getUserInfo(HttpServletRequest httpServletRequest) {
        UserInfoDto dto = userService.userInfo(httpServletRequest);
        return new DataResponse<>(dto);
    }

    /**
     *  사용자 로그아웃
     */
    @PutMapping(value = "/logout", name = "사용자 로그아웃")
    public SuccessResponse userLogout(HttpServletRequest httpServletRequest){
        userService.userLogout(httpServletRequest);
        return new SuccessResponse();
    }

    /**
     *  사용자 회원탈퇴
     */
    @DeleteMapping(name = "사용자 회원탈퇴")
    public SuccessResponse userRevoke(HttpServletRequest httpServletRequest){
        userService.userRevoke(httpServletRequest);
        return new SuccessResponse();
    }
    // 서비스 변경 API
    @PostMapping("/service/update")
    public ResponseEntity<?> updateUserServices(
            HttpServletRequest request,
            @RequestBody UserServiceUpdateRequest body
    ) {
        UserServiceChangeDto response = userService.updateUserServices(request, body);
        return ResponseEntity.ok(Map.of(
                "rt", 200,
                "rtMsg", "서비스 목록이 변경되었습니다.",
                "response", response
        ));
    }

    /**
     * 사용자 서비스 동의 내역 조회
     * @return 사용자가 가입한 서비스별 동의 현황
     */
    @GetMapping("/serviceAgreement")
    public ResponseEntity<Map<String, Object>> getUserServiceAgreements(HttpServletRequest request) {
        List<UserServiceAgreementResponse> response = userService.getUserServiceAgreements(request);
        return ResponseEntity.ok(Map.of(
                "rt", 200,
                "rtMsg", "service agreement list 조회완료",
                "response", response
        ));
    }
}
