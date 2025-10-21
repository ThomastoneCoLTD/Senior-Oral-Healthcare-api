package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserListDto;
import com.kaii.dentix.domain.admin.dto.AdminUserModifyInfoDto;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserModifyRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/user")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminUserController {

    private final AdminUserService adminUserService;

    private final JwtTokenUtil jwtTokenUtil;
    /**
     *  사용자 인증
     */
    @PutMapping(value = "/verify", name = "사용자 인증")
    public SuccessResponse userVerify(@RequestParam Long userId){
        adminUserService.userVerify(userId);
        return new SuccessResponse();
    }

    /**
     *  사용자 정보 조회
     */
    @GetMapping(value = "/info", name = "사용자 정보 조회")
    public DataResponse<AdminUserModifyInfoDto> userInfo(@RequestParam Long userId) {
        DataResponse<AdminUserModifyInfoDto> response = new DataResponse<>(adminUserService.userInfo(userId));
        return response;
    }

    /**
     *  사용자 정보 수정
     */
    @PutMapping(name = "사용자 정보 수정")
    public SuccessResponse userModify(@Valid @RequestBody AdminUserModifyRequest request){
        adminUserService.userModify(request);
        return new SuccessResponse();
    }

    /**
     *  사용자 삭제
     */
    @DeleteMapping(name = "사용자 삭제")
    public SuccessResponse userDelete(@RequestParam Long userId){
        adminUserService.userDelete(userId);
        return new SuccessResponse();
    }

    /**
     *  사용자 목록 조회
     */
    @GetMapping(name = "사용자 목록 조회")
    public DataResponse<AdminUserListDto> userList(AdminUserListRequest request){
        DataResponse<AdminUserListDto> response = new DataResponse<>(adminUserService.userList(request));
        return response;
    }

    @GetMapping("/users")
    public ResponseEntity<AdminUserListDto> getUsersByOrganization(
            @ModelAttribute AdminUserListRequest request,
            HttpServletRequest httpRequest
    ) {
        Long organizationId = jwtTokenUtil.getOrganizationIdFromAccessToken(httpRequest);
        boolean isSuperAdmin = jwtTokenUtil.isSuperAdmin(httpRequest);
        log.info(organizationId.toString());
        // ✅ 일반 관리자만 기관 필터 적용
        if (!isSuperAdmin && organizationId != null) {
            request.setOrganizationId(organizationId);
        }

        AdminUserListDto result = adminUserService.userList(request);
        log.info(result.toString());
        return ResponseEntity.ok(result);
    }

}
