package com.kaii.dentix.domain.admin.controller;
import com.kaii.dentix.domain.admin.application.AdminUserBulkService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/user")
public class AdminUserTemplateController {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AdminRepository adminRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
        /**
         * ✅ 사용자 일괄등록 엑셀 양식 다운로드
         */
        @GetMapping("/bulk-upload/template")
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
     * ✅ 사용자 일괄등록 (엑셀 업로드)
     * 로그인한 관리자의 기관에 자동 소속
     */

    /** ✅ 셀 값 문자열 변환 */
    private String getStringValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    /** ✅ 엑셀 날짜 변환 (LocalDate or Numeric 지원) */
    private Date parseExcelDate(Cell cell) {
        try {
            if (cell == null) return null;

            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                String value = cell.toString().trim();
                if (value.isEmpty()) return null;

                LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return java.sql.Date.valueOf(localDate);
            }
        } catch (Exception e) {
            log.warn("⚠️ 날짜 변환 실패: {}", cell);
            return null;
        }
    }

    private final AdminUserBulkService bulkUploadService;

    /** ✅ 사용자 일괄등록 업로드 */
    @PostMapping("/bulk-upload")
    public DataResponse<String> uploadUsers(@RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
// ✅ 토큰 추출
        String token = jwtTokenUtil.getAccessToken(request);
        UserRole role = jwtTokenUtil.getRoles(token, TokenType.AccessToken);

        // ✅ 관리자 권한 검증
        if (!(role == UserRole.ROLE_ADMIN || role == UserRole.ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("관리자 권한이 필요합니다.");
        }

        // ✅ JWT에서 관리자 ID 추출
        Long adminId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
        log.info("✅ [Excel Upload] 관리자 ID: {}", adminId);

        // ✅ 관리자 조회
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("관리자 정보를 찾을 수 없습니다."));

        // ✅ 서비스 호출
        String result = bulkUploadService.processExcelUpload(file, admin);

        return new DataResponse<>(result);
    }
}