package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/user")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminUserController {

    private final AdminService adminService; //토큰/권한 처리용 서비스 추가
    private final AdminUserService adminUserService;
    private final JwtTokenUtil jwtTokenUtil;
    // 불필요한 Repository, JwtTokenUtil 의존성 제거 (AdminService가 담당)

    /**
     * 일반관리자 - 본인 기관의 사용자 목록 조회
     */
    @GetMapping(name = "사용자 목록 조회")
    public ResponseEntity<DataResponse<AdminUserDto.ListResponse>> userList(
            @ModelAttribute AdminUserDto.SearchRequest request, //DTO 교체
            HttpServletRequest servletRequest
    ) {
        // Service가 변경된 DTO(ListResponse)를 반환하므로 그대로 응답
        return ResponseEntity.ok(
                new DataResponse<>(200, "사용자 목록 조회 성공",
                        adminUserService.userList(request, servletRequest))
        );
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 인증
     */
    @PutMapping(value = "/verify", name = "사용자 인증")
    public SuccessResponse userVerify(@RequestParam Long userId) {
        adminUserService.userVerify(userId);
        return new SuccessResponse();
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 인증 취소
     */
    @PutMapping(value = "/unverify", name = "사용자 인증 취소")
    public SuccessResponse userUnverify(@RequestParam Long userId) {
        adminUserService.userUnverify(userId);
        return new SuccessResponse();
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 정보 조회
     */
    @GetMapping(value = "/info", name = "사용자 정보 조회")
    public DataResponse<AdminUserDto.DetailResponse> userInfo(@RequestParam Long userId) {
        //리턴 타입 변경 (AdminUserModifyInfoDto -> AdminUserDto.DetailResponse)
        return new DataResponse<>(adminUserService.userInfo(userId));
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 정보 수정
     */
    @PutMapping(name = "사용자 정보 수정")
    public SuccessResponse userModify(@Valid @RequestBody AdminUserDto.ModifyRequest request) { //DTO 교체
        adminUserService.userModify(request);
        return new SuccessResponse();
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 삭제
     */
    @DeleteMapping(name = "사용자 삭제")
    public SuccessResponse userDelete(@RequestParam Long userId) {
        adminUserService.userDelete(userId);
        return new SuccessResponse();
    }

    /**
     * 일반관리자 - 기관 사용자 사용량 조회
     */
    @GetMapping(value = "/usage", name = "사용자 사용량 조회")
    public ResponseEntity<DataResponse<Map<String, Object>>> getUsage(HttpServletRequest request) {
        return ResponseEntity.ok(adminUserService.getOrganizationUserUsage(request));
    }

    /**
     * 일반관리자 - 기관 사용자 일괄등록 엑셀 양식 다운로드
     */
    @GetMapping(value = "/bulk-upload/template", name = "기관 사용자 일괄등록 엑셀 양식 다운로드")
    public ResponseEntity<byte[]> downloadUserTemplate() {
        byte[] templateBytes = adminUserService.createBulkUploadTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + new String("user_bulk_template.xlsx".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(templateBytes);
    }

    /**
     * 일반관리자 - 기관 사용자 일괄등록 업로드
     */
    @PostMapping(value = "/bulk-upload", name = "기관 사용자 일괄등록 업로드")
    public DataResponse<AdminUserDto.BulkUploadResponse> uploadUsers(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        Admin admin = adminService.getTokenAdmin(request);
        AdminUserDto.BulkUploadResponse result = adminUserService.processExcelUpload(file, admin);
        return new DataResponse<>(result);
    }
}
