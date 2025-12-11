package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//import static com.kaii.dentix.DentixApplication.log;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserBulkService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public String processExcelUpload(MultipartFile file, Admin admin) {
        Organization org = admin.getOrganization();
        if (org == null) {
            throw new RuntimeException("관리자에 연결된 기관이 없습니다.");
        }

        int successCount = 0;
        int failCount = 0;
        List<User> saveList = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;

            for (Row row : sheet) {
                if (rowCount++ == 0) continue; // ✅ 헤더 스킵

                try {
                    String loginId = getStringValue(row.getCell(0));
                    String password = getStringValue(row.getCell(1));
                    String name = getStringValue(row.getCell(2));
                    String genderValue = getStringValue(row.getCell(3)).trim().toUpperCase();
                    String phone = getStringValue(row.getCell(4));
                    Date birth = parseExcelDate(row.getCell(5));
                    String findPwdQuestionIdStr = getStringValue(row.getCell(6));
                    String findPwdAnswer = getStringValue(row.getCell(7));

                    if (loginId.isEmpty() || password.isEmpty() || name.isEmpty()) {
                        log.warn("⚠️ 필수값 누락 [Row {}]", row.getRowNum());
                        failCount++;
                        continue;
                    }

                    // ✅ 성별 변환
                    if (genderValue.equals("F")) genderValue = "W";
                    if (!genderValue.equals("M") && !genderValue.equals("W")) {
                        log.warn("⚠️ 잘못된 성별값 [Row {}]: {}", row.getRowNum(), genderValue);
                        failCount++;
                        continue;
                    }

                    User user = new User();
                    user.setUserLoginIdentifier(loginId);
                    user.setUserPassword(passwordEncoder.encode(password));
                    user.setUserName(name);
                    user.setUserGender(GenderType.valueOf(genderValue));
                    user.setUserPhoneNumber(phone);
                    user.setIsVerify(YnType.N);
                    user.setOrganization(org);
                    user.setSuccessCount(0);

                    if (!findPwdQuestionIdStr.isEmpty()) {
                        try {
                            user.setFindPwdQuestionId(Long.parseLong(findPwdQuestionIdStr));
                        } catch (NumberFormatException e) {
                            log.warn("⚠️ 잘못된 비밀번호 질문 ID [Row {}]: {}", row.getRowNum(), findPwdQuestionIdStr);
                        }
                    }
                    user.setFindPwdAnswer(findPwdAnswer);

                    saveList.add(user);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    log.error("[Row {}] 사용자 등록 실패: {}", row.getRowNum(), e.getMessage());
                }
            }

            // ✅ 일괄 저장
            userRepository.saveAll(saveList);

        } catch (Exception e) {
            log.error("❌ 엑셀 파싱 오류: {}", e.getMessage(), e);
            throw new RuntimeException("엑셀 파일 처리 중 오류가 발생했습니다.");
        }

        log.info("✅ 사용자 일괄등록 완료 - 성공: {}, 실패: {}", successCount, failCount);
        return "성공: " + successCount + "명, 실패: " + failCount + "명";
    }

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
}