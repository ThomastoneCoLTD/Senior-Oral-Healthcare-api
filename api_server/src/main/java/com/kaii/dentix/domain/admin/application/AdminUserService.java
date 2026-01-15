package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserRepositoryImpl;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final ModelMapper modelMapper;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLoginService userLoginService;
    private final OralCheckRepository oralcheckRepository;
    private final AdminUserRepositoryImpl adminUserRepository;
    private final AdminUserCustomRepository adminUserCustomRepository;
    private final OrganizationSubscriptionService organizationSubscriptionService;

    /**
     * 일반관리자 - 본인 기관의 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public AdminUserDto.ListResponse userList(AdminUserDto.SearchRequest request, HttpServletRequest servletRequest) {
        Admin admin = adminService.getTokenAdmin(servletRequest);

        // 기본값 설정
        if (request.getPage() == null) request.setPage(1);
        if (request.getSize() == null) request.setSize(50);

        Page<AdminUserDto.Info> pageResult;

        // 슈퍼/일반 관리자 분기 처리
        if (admin.isSuperAdmin()) {
            pageResult = adminUserCustomRepository.findAll(request);
        } else {
            if (admin.getOrganization() == null) {
                throw new BadRequestApiException("소속 기관이 없습니다.");
            }
            request.setOrganizationId(admin.getOrganization().getOrganizationId());
            pageResult = adminUserCustomRepository.findAll(request);
        }

        PagingDTO pagingDTO = modelMapper.map(pageResult, PagingDTO.class);
        return AdminUserDto.ListResponse.of(pagingDTO, pageResult.getContent());
    }

    /**
     * 기관별 사용자 목록 조회 (슈퍼관리자용)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDto.Info> getUsersByOrganization(AdminUserDto.SearchRequest request) {
        return adminUserRepository.findAllByOrganization(request);
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 정보 조회 (수정용)
     */
    @Transactional(readOnly = true)
    public AdminUserDto.DetailResponse userInfo(Long userId) {
        User user = getUser(userId);

        // ✅ 정적 팩토리 메서드로 DTO 생성 (깔끔)
        return AdminUserDto.DetailResponse.from(
                user.getUserLoginIdentifier(),
                user.getUserName(),
                user.getUserGender()
        );
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 정보 수정
     */
    @Transactional
    public void userModify(AdminUserDto.ModifyRequest request) { // ✅ DTO 교체
        User user = getUser(request.getUserId());

        // 아이디 변경 시 중복 체크
        if (!user.getUserLoginIdentifier().equals(request.getLoginId())) {
            userLoginService.loginIdCheck(request.getLoginId());
        }

        user.adminModifyInfo(request.getLoginId(), request.getName(), request.getGender());
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 인증
     */
    @Transactional
    public void userVerify(Long userId) {
        User user = getUser(userId);
        if (user.getIsVerify() == YnType.Y) throw new BadRequestApiException("이미 인증된 사용자입니다.");
        user.setIsVerify(YnType.Y);
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 인증 취소
     */
    @Transactional
    public void userUnverify(Long userId) {
        User user = getUser(userId);
        if (user.getIsVerify() == YnType.N) throw new BadRequestApiException("이미 인증 취소된 사용자입니다.");
        user.setIsVerify(YnType.N);
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 삭제
     */
    @Transactional
    public void userDelete(Long userId) {
        User user = getUser(userId);
        user.revoke(); // 회원 탈퇴(삭제) 처리
    }

    /**
     * 기관 사용자 사용량 조회
     */
    public DataResponse<Map<String, Object>> getOrganizationUserUsage(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        Organization org = admin.getOrganization();

        if (org == null) throw new BadRequestApiException("기관 정보가 없습니다.");

        OrganizationSubscription activeSubscription = organizationSubscriptionService.getActiveSubscription(org);

        // 날짜 변환 로직
        Date startDate = toDate(activeSubscription.getSubscriptionStartDate());
        Date endDate = toDate(activeSubscription.getUsageResetDate());

        List<OralCheckUsageDto> usageList = oralcheckRepository.findUserUsageByOrganizationAndPeriod(
                org.getOrganizationId(), startDate, endDate
        );

        long total = usageList.stream().mapToLong(OralCheckUsageDto::getSuccessCount).sum();

        return new DataResponse<>(200, "기관 사용자 사용량 조회 성공", Map.of("totalCount", total, "users", usageList));
    }

    /**
     * 일반관리자 - 사용자 일괄등록 업로드 (리팩토링됨)
     */
    @Transactional
    public String processExcelUpload(MultipartFile file, Admin admin) {
        Organization org = admin.getOrganization();
        if (org == null) throw new RuntimeException("관리자에 연결된 기관이 없습니다.");

        // 파싱 결과를 담을 객체 (성공 목록 + 실패 카운트)
        ExcelParseResult result = parseUserExcel(file, org);

        // 저장
        if (!result.users.isEmpty()) {
            userRepository.saveAll(result.users);
        }

        return "성공: " + result.users.size() + "명, 실패: " + result.failCount + "명";
    }

    // --- Private Helper Methods ---

    // 파싱 결과를 담기 위한 내부 클래스 (DTO)
    private static class ExcelParseResult {
        List<User> users = new ArrayList<>();
        int failCount = 0;
    }

    // 사용자 조회 헬퍼
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));
    }

    // LocalDateTime -> Date 변환 헬퍼
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // 엑셀 파싱 로직
    private ExcelParseResult parseUserExcel(MultipartFile file, Organization org) {
        ExcelParseResult result = new ExcelParseResult();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // 헤더 스킵

                try {
                    // 필수 값 체크
                    String loginId = getStringValue(row.getCell(0), dataFormatter);
                    String password = getStringValue(row.getCell(1), dataFormatter);
                    String name = getStringValue(row.getCell(2), dataFormatter);

                    if (loginId.isEmpty() || password.isEmpty() || name.isEmpty()) {
                        result.failCount++;
                        continue;
                    }

                    // User 생성 및 매핑
                    User user = createUserFromRow(row, org, loginId, password, name, dataFormatter);
                    result.users.add(user);

                } catch (Exception e) {
                    result.failCount++; // 파싱 중 에러 발생 시 실패 카운트 증가
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("엑셀 파일 처리 중 오류가 발생했습니다.", e);
        }
        return result;
    }

    // Row -> User 변환 로직 (누락된 필드 복구)
    private User createUserFromRow(Row row, Organization org, String loginId, String pwd, String name, DataFormatter formatter) {
        // 성별 파싱 및 변환
        String genderValue = getStringValue(row.getCell(3), formatter).trim().toUpperCase();
        if (genderValue.equals("F")) genderValue = "W";
        if (!genderValue.equals("M") && !genderValue.equals("W")) {
            throw new IllegalArgumentException("Invalid gender");
        }

        User user = new User();
        user.setUserLoginIdentifier(loginId);
        user.setUserPassword(passwordEncoder.encode(pwd));
        user.setUserName(name);
        user.setUserGender(GenderType.valueOf(genderValue));
        user.setUserPhoneNumber(getStringValue(row.getCell(4), formatter)); // 연락처
        user.setIsVerify(YnType.N);
        user.setOrganization(org);
        user.setSuccessCount(0); // 기존 코드에 있던 초기화 복구

        // ✅ [복구 완료] 비밀번호 찾기 질문 & 답변 매핑
        String qIdStr = getStringValue(row.getCell(6), formatter);
        if (!qIdStr.isEmpty()) {
            user.setFindPwdQuestionId(Long.parseLong(qIdStr));
        }
        user.setFindPwdAnswer(getStringValue(row.getCell(7), formatter));

        return user;
    }

    private String getStringValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }
}