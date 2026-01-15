package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public void downloadUserTemplate(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("UserTemplate");

        // 헤더
        String[] headers = {
                "userLoginIdentifier", "userPassword", "userName", "userGender",
                "userPhoneNumber", "birth", "findPwdQuestionId", "findPwdAnswer",
                "organizationId", "appServiceIds", "userServiceAgreementRequest"
        };
        Row header = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6000);
        }

        // 예시 데이터
        Row example = sheet.createRow(1);
        example.createCell(0).setCellValue("user01");
        example.createCell(1).setCellValue("test1234!");
        example.createCell(2).setCellValue("홍길동");
        example.createCell(3).setCellValue("M");
        example.createCell(4).setCellValue("01012345678");
        example.createCell(5).setCellValue("1990-08-21");
        example.createCell(6).setCellValue("1");
        example.createCell(7).setCellValue("테스트답변");
        example.createCell(8).setCellValue("2");
        example.createCell(9).setCellValue("1,2");
        example.createCell(10).setCellValue("1,2,3,4,5");

        // 응답
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=user_bulk_template.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * 일반관리자 - 기관 사용자 일괄등록 업로드
     */
    @PostMapping(value = "/bulk-upload", name = "기관 사용자 일괄등록 업로드")
    public DataResponse<String> uploadUsers(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        //중복 로직 제거: AdminService를 통해 토큰 검증 및 Admin 객체 획득
        Admin admin = adminService.getTokenAdmin(request);

        // 서비스 호출
        String result = adminUserService.processExcelUpload(file, admin);

        return new DataResponse<>(result);
    }
}