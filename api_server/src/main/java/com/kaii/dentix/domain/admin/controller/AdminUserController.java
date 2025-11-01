package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.dto.AdminUserListDto;
import com.kaii.dentix.domain.admin.dto.AdminUserModifyInfoDto;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserModifyRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import io.micrometer.core.instrument.MultiGauge;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/user")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminUserController {

    private final AdminUserService adminUserService;

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
    /** ✅ 사용자 목록 조회 (슈퍼관리자: 전체 / 일반관리자: 자기 기관) */
    @GetMapping(name = "사용자 목록 조회")
    public DataResponse<AdminUserListDto> userList(AdminUserListRequest request, HttpServletRequest servletRequest) {
        AdminUserListDto result = adminUserService.userList(request, servletRequest);
        return new DataResponse<>(result);
    }


}