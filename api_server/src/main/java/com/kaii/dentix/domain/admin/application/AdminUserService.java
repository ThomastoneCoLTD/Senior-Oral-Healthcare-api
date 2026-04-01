package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserRepositoryImpl;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.agreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.agreement.domain.ServiceAgreement;
import com.kaii.dentix.domain.appService.dao.AppServiceRepository;
import com.kaii.dentix.domain.appService.dao.UserToAppServiceRepository;
import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.appService.domain.UserToAppService;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.findPwdQuestion.domain.FindPwdQuestion;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String[] TEMPLATE_HEADERS = {
            "userLoginIdentifier", "userPassword", "userName", "userGender",
            "userPhoneNumber", "findPwdQuestionId", "findPwdAnswer",
            "appServiceIds", "userServiceAgreementIds"
    };
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*0-9]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,11}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]+$");

    private final ModelMapper modelMapper;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLoginService userLoginService;
    private final OralCheckRepository oralcheckRepository;
    private final AdminUserRepositoryImpl adminUserRepository;
    private final AdminUserCustomRepository adminUserCustomRepository;
    private final OrganizationSubscriptionService organizationSubscriptionService;
    private final AppServiceRepository appServiceRepository;
    private final UserToAppServiceRepository userToAppServiceRepository;
    private final FindPwdQuestionRepository findPwdQuestionRepository;
    private final ServiceAgreementRepository serviceAgreementRepository;
    private final ServiceAgreementConsentService serviceAgreementConsentService;
    private final PlatformTransactionManager transactionManager;

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
    public void userModify(AdminUserDto.ModifyRequest request) {
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
        Date endDate = toDate(activeSubscription.getUsageResetDate() != null
                ? activeSubscription.getUsageResetDate()
                : activeSubscription.getSubscriptionEndDate());

        List<OralCheckDto.Usage> usageList = oralcheckRepository.findUserUsageByOrganizationAndPeriod(
                org.getOrganizationId(), startDate, endDate
        );

        long total = usageList.stream().mapToLong(OralCheckDto.Usage::getSuccessCount).sum();
        return new DataResponse<>(200, "기관 사용자 사용량 조회 성공", Map.of("totalCount", total, "users", usageList));
    }

    /**
     * 일반관리자 - 기관 사용자 일괄등록 엑셀 양식 다운로드
     */
    @Transactional(readOnly = true)
    public byte[] createBulkUploadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            createTemplateSheet(workbook);
            createGuideSheet(workbook);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BadRequestApiException("엑셀 양식을 생성하지 못했어요.");
        }
    }

    /**
     * 일반관리자 - 사용자 일괄등록 업로드
     */
    public AdminUserDto.BulkUploadResponse processExcelUpload(MultipartFile file, Admin admin) {
        Organization org = admin.getOrganization();
        if (org == null) throw new BadRequestApiException("관리자에 연결된 기관이 없습니다.");
        if (file == null || file.isEmpty()) throw new BadRequestApiException("업로드할 엑셀 파일을 선택해 주세요.");

        BulkUploadReference reference = getBulkUploadReference();
        Set<String> fileLoginIdSet = new HashSet<>();
        List<AdminUserDto.FailInfo> failList = new ArrayList<>();
        int successCount = 0;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || isEmptyRow(row, formatter)) {
                    continue;
                }

                int excelRowNumber = row.getRowNum() + 1;
                String failureReason = transactionTemplate.execute(status -> {
                    try {
                        registerUserFromRow(row, org, formatter, reference, fileLoginIdSet);
                        return null;
                    } catch (RuntimeException e) {
                        status.setRollbackOnly();
                        return getFailReason(e);
                    }
                });

                if (failureReason == null) {
                    successCount++;
                } else {
                    failList.add(AdminUserDto.FailInfo.builder()
                            .row(excelRowNumber)
                            .reason(failureReason)
                            .build());
                }
            }
        } catch (BadRequestApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestApiException("엑셀 파일을 읽지 못했어요. .xlsx 파일인지 확인해 주세요.");
        }

        return AdminUserDto.BulkUploadResponse.of(successCount, failList);
    }

    // --- Private Helper Methods ---

    // 사용자 조회 헬퍼
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));
    }

    // LocalDateTime -> Date 변환 헬퍼
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void createTemplateSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Template");
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(TEMPLATE_HEADERS[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6500);
        }

        Row example = sheet.createRow(1);
        example.createCell(0).setCellValue("user01");
        example.createCell(1).setCellValue("test1234!");
        example.createCell(2).setCellValue("홍길동");
        example.createCell(3).setCellValue("M");
        example.createCell(4).setCellValue("01012345678");
        example.createCell(5).setCellValue("1");
        example.createCell(6).setCellValue("우리집");
        example.createCell(7).setCellValue("1,2");
        example.createCell(8).setCellValue("1,2,3");
    }

    private void createGuideSheet(Workbook workbook) {
        Sheet guideSheet = workbook.createSheet("Guide");
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row titleRow = guideSheet.createRow(0);
        titleRow.createCell(0).setCellValue("column");
        titleRow.createCell(1).setCellValue("description");
        titleRow.createCell(2).setCellValue("example");
        for (int i = 0; i < 3; i++) {
            titleRow.getCell(i).setCellStyle(headerStyle);
            guideSheet.setColumnWidth(i, 9000);
        }

        Object[][] guides = new Object[][]{
                {"userLoginIdentifier", "4~12자의 영문/숫자 아이디", "user01"},
                {"userPassword", "8~20자, 영문과 특수문자 포함", "test1234!"},
                {"userName", "이름", "홍길동"},
                {"userGender", "M 또는 W(F 입력 시 W로 처리)", "M"},
                {"userPhoneNumber", "숫자만 10~11자리", "01012345678"},
                {"findPwdQuestionId", "비밀번호 찾기 질문 ID", "1"},
                {"findPwdAnswer", "비밀번호 찾기 답변", "우리집"},
                {"appServiceIds", "서비스 ID를 쉼표로 구분, 비우면 전체 서비스로 등록", "1,2"},
                {"userServiceAgreementIds", "약관 ID를 쉼표로 구분, 비우면 필수 약관만 동의 처리", "1,2,3"}
        };

        int rowIndex = 1;
        for (Object[] guide : guides) {
            Row row = guideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(Objects.toString(guide[0], ""));
            row.createCell(1).setCellValue(Objects.toString(guide[1], ""));
            row.createCell(2).setCellValue(Objects.toString(guide[2], ""));
        }

        rowIndex = writeQuestionGuideRows(guideSheet, rowIndex, headerStyle);
        rowIndex = writeAppServiceGuideRows(guideSheet, rowIndex, headerStyle);
        writeAgreementGuideRows(guideSheet, rowIndex, headerStyle);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    private int writeQuestionGuideRows(Sheet guideSheet, int rowIndex, CellStyle headerStyle) {
        Row questionHeader = guideSheet.createRow(rowIndex++);
        questionHeader.createCell(0).setCellValue("findPwdQuestionId");
        questionHeader.createCell(1).setCellValue("questionTitle");
        questionHeader.getCell(0).setCellStyle(headerStyle);
        questionHeader.getCell(1).setCellStyle(headerStyle);

        List<FindPwdQuestion> questionList = findPwdQuestionRepository.findAll(Sort.by(Sort.Direction.ASC, "findPwdQuestionSort"));
        for (FindPwdQuestion question : questionList) {
            Row row = guideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(question.getFindPwdQuestionId());
            row.createCell(1).setCellValue(question.getFindPwdQuestionTitle());
        }
        return rowIndex + 1;
    }

    private int writeAppServiceGuideRows(Sheet guideSheet, int rowIndex, CellStyle headerStyle) {
        Row appServiceHeader = guideSheet.createRow(rowIndex++);
        appServiceHeader.createCell(0).setCellValue("appServiceId");
        appServiceHeader.createCell(1).setCellValue("serviceName");
        appServiceHeader.getCell(0).setCellStyle(headerStyle);
        appServiceHeader.getCell(1).setCellStyle(headerStyle);

        List<AppService> appServices = appServiceRepository.findAll();
        for (AppService appService : appServices) {
            Row row = guideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(appService.getAppServiceId());
            row.createCell(1).setCellValue(appService.getName());
        }
        return rowIndex + 1;
    }

    private void writeAgreementGuideRows(Sheet guideSheet, int rowIndex, CellStyle headerStyle) {
        Row agreementHeader = guideSheet.createRow(rowIndex++);
        agreementHeader.createCell(0).setCellValue("serviceAgreementId");
        agreementHeader.createCell(1).setCellValue("agreementName");
        agreementHeader.createCell(2).setCellValue("required");
        agreementHeader.getCell(0).setCellStyle(headerStyle);
        agreementHeader.getCell(1).setCellStyle(headerStyle);
        agreementHeader.getCell(2).setCellStyle(headerStyle);

        List<ServiceAgreement> agreements = serviceAgreementRepository.findAll(Sort.by(Sort.Direction.ASC, "serviceAgreeSort"));
        for (ServiceAgreement agreement : agreements) {
            Row row = guideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(agreement.getServiceAgreeId());
            row.createCell(1).setCellValue(agreement.getServiceAgreeName());
            row.createCell(2).setCellValue(agreement.getIsServiceAgreeRequired().name());
        }
    }

    private void registerUserFromRow(Row row,
                                     Organization organization,
                                     DataFormatter formatter,
                                     BulkUploadReference reference,
                                     Set<String> fileLoginIdSet) {
        String loginId = getStringValue(row.getCell(0), formatter);
        String password = getStringValue(row.getCell(1), formatter);
        String name = getStringValue(row.getCell(2), formatter);
        String genderRaw = getStringValue(row.getCell(3), formatter);
        String phoneNumber = getStringValue(row.getCell(4), formatter);
        String questionIdRaw = getStringValue(row.getCell(5), formatter);
        String findPwdAnswer = getStringValue(row.getCell(6), formatter);
        String appServiceIdsRaw = getStringValue(row.getCell(7), formatter);
        String agreementIdsRaw = getStringValue(row.getCell(8), formatter);

        validateRequiredText(loginId, "아이디");
        validateRequiredText(password, "비밀번호");
        validateRequiredText(name, "이름");
        validateRequiredText(phoneNumber, "전화번호");
        validateRequiredText(questionIdRaw, "비밀번호 찾기 질문");
        validateRequiredText(findPwdAnswer, "비밀번호 찾기 답변");

        validateLoginId(loginId);
        validatePassword(password);
        validateUserName(name);
        validatePhoneNumber(phoneNumber);

        if (fileLoginIdSet.contains(loginId)) {
            throw new AlreadyDataException("파일 내 아이디 중복");
        }
        if (userRepository.findByUserLoginIdentifier(loginId).isPresent()) {
            throw new AlreadyDataException("아이디 중복");
        }

        Long questionId = parseLong(questionIdRaw, "비밀번호 찾기 질문 ID");
        if (!reference.questionMap.containsKey(questionId)) {
            throw new NotFoundDataException("존재하지 않는 비밀번호 찾기 질문입니다.");
        }

        GenderType gender = parseGender(genderRaw);
        List<Long> appServiceIds = parseIdList(appServiceIdsRaw, "서비스 ID");
        if (appServiceIds.isEmpty()) {
            appServiceIds = new ArrayList<>(reference.allAppServiceIds);
        }
        if (appServiceIds.isEmpty()) {
            throw new BadRequestApiException("등록 가능한 서비스가 없습니다.");
        }
        validateIds(appServiceIds, reference.appServiceMap.keySet(), "존재하지 않는 서비스 ID가 포함되어 있습니다.");

        List<Long> agreementIds = parseIdList(agreementIdsRaw, "약관 ID");
        if (agreementIds.isEmpty()) {
            agreementIds = new ArrayList<>(reference.requiredAgreementIds);
        }
        validateIds(agreementIds, reference.serviceAgreementMap.keySet(), "존재하지 않는 약관 ID가 포함되어 있습니다.");

        User user = userRepository.save(User.builder()
                .userLoginIdentifier(loginId)
                .userPassword(passwordEncoder.encode(password))
                .userName(name)
                .userPhoneNumber(phoneNumber)
                .userGender(gender)
                .findPwdQuestionId(questionId)
                .findPwdAnswer(findPwdAnswer)
                .organization(organization)
                .isVerify(YnType.N)
                .successCount(0)
                .build());

        List<UserToAppService> userToAppServices = appServiceIds.stream()
                .distinct()
                .map(serviceId -> UserToAppService.builder()
                        .user(user)
                        .appService(reference.appServiceMap.get(serviceId))
                        .build())
                .toList();
        userToAppServiceRepository.saveAll(userToAppServices);
        serviceAgreementConsentService.saveUserServiceAgreements(user.getUserId(), agreementIds);
        fileLoginIdSet.add(loginId);
    }

    private BulkUploadReference getBulkUploadReference() {
        List<AppService> appServices = appServiceRepository.findAll();
        List<FindPwdQuestion> findPwdQuestions = findPwdQuestionRepository.findAll(Sort.by(Sort.Direction.ASC, "findPwdQuestionSort"));
        List<ServiceAgreement> serviceAgreements = serviceAgreementRepository.findAll(Sort.by(Sort.Direction.ASC, "serviceAgreeSort"));

        Map<Long, AppService> appServiceMap = new HashMap<>();
        for (AppService appService : appServices) {
            appServiceMap.put(appService.getAppServiceId(), appService);
        }

        Map<Long, FindPwdQuestion> questionMap = new HashMap<>();
        for (FindPwdQuestion findPwdQuestion : findPwdQuestions) {
            questionMap.put(findPwdQuestion.getFindPwdQuestionId(), findPwdQuestion);
        }

        Map<Long, ServiceAgreement> serviceAgreementMap = new HashMap<>();
        List<Long> requiredAgreementIds = new ArrayList<>();
        for (ServiceAgreement serviceAgreement : serviceAgreements) {
            serviceAgreementMap.put(serviceAgreement.getServiceAgreeId(), serviceAgreement);
            if (serviceAgreement.getIsServiceAgreeRequired() == YnType.Y) {
                requiredAgreementIds.add(serviceAgreement.getServiceAgreeId());
            }
        }

        return new BulkUploadReference(
                appServiceMap,
                questionMap,
                serviceAgreementMap,
                appServices.stream().map(AppService::getAppServiceId).toList(),
                requiredAgreementIds
        );
    }

    private void validateRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestApiException(fieldName + "는 필수입니다.");
        }
    }

    private void validateLoginId(String loginId) {
        if (loginId.length() < 4 || loginId.length() > 12 || !LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new BadRequestApiException("아이디 형식 오류");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 20 || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestApiException("비밀번호 형식 오류");
        }
    }

    private void validateUserName(String name) {
        if (name.length() < 2 || name.length() > 100 || !NAME_PATTERN.matcher(name).matches()) {
            throw new BadRequestApiException("이름 형식 오류");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new BadRequestApiException("전화번호 형식 오류");
        }
    }

    private GenderType parseGender(String genderValue) {
        if (!StringUtils.hasText(genderValue)) {
            throw new BadRequestApiException("성별은 필수입니다.");
        }

        String normalizedGender = genderValue.trim().toUpperCase();
        if ("F".equals(normalizedGender)) {
            normalizedGender = "W";
        }
        if (!"M".equals(normalizedGender) && !"W".equals(normalizedGender)) {
            throw new BadRequestApiException("성별 형식 오류");
        }
        return GenderType.valueOf(normalizedGender);
    }

    private void validateIds(List<Long> ids, Set<Long> validIds, String errorMessage) {
        if (ids.stream().anyMatch(id -> !validIds.contains(id))) {
            throw new NotFoundDataException(errorMessage);
        }
    }

    private List<Long> parseIdList(String rawValue, String fieldName) {
        if (!StringUtils.hasText(rawValue)) {
            return new ArrayList<>();
        }

        try {
            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::parseLong)
                    .distinct()
                    .toList();
        } catch (NumberFormatException e) {
            throw new BadRequestApiException(fieldName + " 형식 오류");
        }
    }

    private Long parseLong(String rawValue, String fieldName) {
        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestApiException(fieldName + " 형식 오류");
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }

        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            if (StringUtils.hasText(getStringValue(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String getFailReason(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        String message = throwable.getMessage();
        if (!StringUtils.hasText(message) && current != null) {
            message = current.getMessage();
        }
        return StringUtils.hasText(message) ? message.replace("\n", " ") : "행 처리 중 오류가 발생했습니다.";
    }

    private String getStringValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private record BulkUploadReference(
            Map<Long, AppService> appServiceMap,
            Map<Long, FindPwdQuestion> questionMap,
            Map<Long, ServiceAgreement> serviceAgreementMap,
            List<Long> allAppServiceIds,
            List<Long> requiredAgreementIds
    ) {}
}
