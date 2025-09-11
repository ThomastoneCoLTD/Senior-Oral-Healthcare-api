//package com.kaii.dentix.domain.oralCheck.application;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
//import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
//import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
//import com.kaii.dentix.domain.oralCheck.dto.*;
//import com.kaii.dentix.domain.oralCheck.dto.resoponse.OralCheckAnalysisResponse;
//import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
//import com.kaii.dentix.domain.oralStatus.jpa.OralStatusRepository;
//import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
//import com.kaii.dentix.domain.organization.domain.Organization;
//import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireCustomRepository;
//import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
//import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
//import com.kaii.dentix.domain.questionnaire.dto.OralStatusTypeDto;
//import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireAndStatusDto;
//import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingCustomRepository;
//import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingRepository;
//import com.kaii.dentix.domain.toothBrushing.domain.ToothBrushing;
//import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDailyCountDto;
//import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDto;
//import com.kaii.dentix.domain.type.OralDateStatusType;
//import com.kaii.dentix.domain.type.OralSectionType;
//import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
//import com.kaii.dentix.domain.type.oral.OralCheckResultType;
//import com.kaii.dentix.domain.user.application.UserService;
//import com.kaii.dentix.domain.user.dao.UserRepository;
//import com.kaii.dentix.domain.user.domain.User;
//import com.kaii.dentix.domain.userOralStatus.dao.UserOralStatusRepository;
//import com.kaii.dentix.domain.userOralStatus.domain.UserOralStatus;
//import com.kaii.dentix.global.common.aws.AWSS3Service;
//import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
//import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
//import com.kaii.dentix.global.common.response.DataResponse;
//import com.kaii.dentix.global.common.util.AiModelService;
//import com.kaii.dentix.global.common.util.DateFormatUtil;
//import com.kaii.dentix.global.common.util.Utils;
//import io.micrometer.common.util.StringUtils;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.util.*;
//import java.util.stream.Collectors;
//
//import static com.kaii.dentix.domain.type.oral.OralCheckDivisionCommentType.*;
//import static com.kaii.dentix.global.common.response.ResponseMessage.SUCCESS_MSG;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OralCheckServiceCopy {
//
//    private final UserService userService;
//
//    private final AWSS3Service awss3Service;
//
//    private final AiModelService aiModelService;
//
//    private final OralCheckRepository oralCheckRepository;
//    private final ToothBrushingRepository toothBrushingRepository;
//    private final ToothBrushingCustomRepository toothBrushingCustomRepository;
//    private final QuestionnaireRepository questionnaireRepository;
//    private final QuestionnaireCustomRepository questionnaireCustomRepository;
//    private final OralStatusRepository oralStatusRepository;
//    private final UserOralStatusRepository userOralStatusRepository;
//    private final UserRepository userRepository;
//
//    private final ObjectMapper objectMapper;
//    private final OrganizationRepository organizationRepository;
//
//    @Value("${spring.profiles.active}")
//    private String active;
//
//    @Value("${s3.folderPath.oralCheck}")
//    private String folderPath;
//
//    /**
//     *  구강검진 사진 촬영
//     */
//    @Transactional
//    @CacheEvict(
//            value = "dashboard",
//            key = "@userService.getTokenUser(#p0).getUserId() + '_' + T(java.time.LocalDate).now()"
//    )
//    public DataResponse<OralCheckPhotoDto> oralCheckPhoto(HttpServletRequest httpServletRequest, MultipartFile file) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
//        User user = userService.getTokenUser(httpServletRequest);
//
//        Long orgId = (user.getOrganization() != null)
//                ? user.getOrganization().getOrganizationId()
//                : null;
//
//        // ✅ 사전 체크: 기관 플랜 사용량 초과 여부
//        if (orgId != null) {
//            Organization org = organizationRepository.findById(orgId)
//                    .orElseThrow(() -> new IllegalArgumentException("기관이 존재하지 않습니다."));
//
//            long successCount = org.getSuccessCount();
//            int maxAllowed = org.getSubscriptionPlan().getMaxSuccessResponses();
//
//            if (successCount >= maxAllowed) {
//                return new DataResponse<>(
//                        410,
//                        "구독 플랜 사용량을 모두 소진했습니다. 관리자에게 문의하세요.",
//                        OralCheckPhotoDto.builder()
//                                .oralCheckId(null)
//                                .success(false)
//                                .remainingResponses(0)
//                                .organizationId(orgId)
//                                .build()
//                );
//            }
//        }
//
//        //파일 업로드
//        String uploadedUrl = awss3Service.upload(file, folderPath, true);
//        if (StringUtils.isBlank(uploadedUrl)) {
//            throw new BadRequestApiException("구강 촬영 결과 저장에 실패했어요.\n관리자에게 문의해 주세요.");
//        }
//
//        //AI 분석 요청
//        OralCheckAnalysisResponse analysisData;
//        try {
//            analysisData = aiModelService.getPyDentalAiModel(file);
//        } catch (Exception e) {
//            if (active.equals("dev")) { // 개발 서버는 랜덤 테스트 데이터
//                log.warn("AI 모델 연동 실패 → 테스트 데이터 연동 (구강 촬영)");
//                Random random = new Random();
//                analysisData = new OralCheckAnalysisResponse(
//                        new OralCheckAnalysisDivisionDto(
//                                random.nextFloat(50),
//                                random.nextFloat(50),
//                                random.nextFloat(50),
//                                random.nextFloat(50)
//                        )
//                );
//            } else {
//                return new DataResponse<>(411, "AI 모델 연동에 실패했어요.\n관리자에게 문의해 주세요.", null);
//            }
//        }
//
//        OralCheck oralCheck;
//        int resultCode = 500;
//
//        switch (analysisData.getStatusCode()) {
//            case 200: // 성공
//                oralCheck = registAnalysisSuccessData(user.getUserId(), uploadedUrl, analysisData);
//                break;
//            case 402: case 403: case 404: // 잘못된 사진
//                oralCheck = registAnalysisFailedData(user.getUserId(), uploadedUrl, analysisData);
//                resultCode = 410;
//                break;
//            default: // 서버 에러
//                oralCheck = registAnalysisFailedData(user.getUserId(), uploadedUrl, analysisData);
//                resultCode = 411;
//                break;
//        }
//
//        //oralCheck가 null이면 바로 예외
//        if (oralCheck == null) {
//            throw new BadRequestApiException("구강 촬영 결과 저장에 실패했어요.\n관리자에게 문의해 주세요.");
//        }
//
//        //성공 케이스 처리
//        if (oralCheck.getOralCheckAnalysisState() == OralCheckAnalysisState.SUCCESS && orgId != null) {
//
//            //DB에서 successCount 안전하게 +1
//            organizationRepository.increaseSuccessCount(orgId);
//
//            //업데이트된 Organization 다시 조회
//            Organization org = organizationRepository.findById(orgId)
//                    .orElseThrow(() -> new IllegalArgumentException("기관이 존재하지 않습니다."));
//
//            int maxAllowed = org.getSubscriptionPlan().getMaxSuccessResponses();
//            int remaining = maxAllowed - org.getSuccessCount();
//
//            if (remaining < 0) {
//                remaining = 0;
//            }
//
//            return new DataResponse<>(
//                    200,
//                    SUCCESS_MSG,
//                    OralCheckPhotoDto.builder()
//                            .oralCheckId(oralCheck.getOralCheckId())
//                            .success(true)
//                            .remainingResponses(remaining)
//                            .organizationId(orgId)
//                            .build()
//            );
//        }
//
//        //실패 케이스 처리
//        return new DataResponse<>(
//                resultCode,
//                "구강 촬영 인식에 실패했어요.\n가이드에 맞게 재촬영 부탁드려요.",
//                null
//        );
//    }
//
//    /**  기관별 플랜 최대 성공 응답수 */
//    @Transactional
//    public void checkPlanUsage(Long orgId) {
//        Organization org = organizationRepository.findById(orgId)
//                .orElseThrow(() -> new IllegalArgumentException("기관이 존재하지 않습니다."));
//
//        int maxAllowed = org.getSubscriptionPlan().getMaxSuccessResponses();
//
//        if (org.getSuccessCount() >= maxAllowed) {
//            throw new IllegalStateException("구독 플랜 사용량을 모두 소진했습니다.");
//        }
//    }
//    /**
//     * 4등분 점수 유형 계산
//     *
//     * @param divisionRange : 영역 비율
//     * @return ToothColoringDivisionScoreType : 4등분 점수 유형 결과
//     */
//    public OralCheckResultType calcDivisionScoreType(Float divisionRange) {
//        return divisionRange < 1 ? OralCheckResultType.HEALTHY
//                : divisionRange < 10 ? OralCheckResultType.GOOD
//                : divisionRange < 30 ? OralCheckResultType.ATTENTION
//                : OralCheckResultType.DANGER;
//    }
//
//    /**
//     * 4등분 코멘트 유형 계산
//     */
//    public List<String> calcDivisionCommentType(OralCheck oralCheck) {
//
//        // 부위별 구강 상태 Comment
//        List<String> divisionCommentTypeList = new ArrayList<>();
//
//        // 모든 부위의 플라그 수치가 동일한 경우 true
//        boolean allEquals = (oralCheck.getOralCheckUpRightRange().equals(oralCheck.getOralCheckUpLeftRange())) &&
//                        (oralCheck.getOralCheckUpLeftRange().equals(oralCheck.getOralCheckDownRightRange()) &&
//                        (oralCheck.getOralCheckDownRightRange().equals(oralCheck.getOralCheckDownLeftRange())));
//
//        if (allEquals && oralCheck.getOralCheckResultTotalType().equals(OralCheckResultType.HEALTHY)) { // 모든 부위의 플라그 비율이 동일하고 , '건강'인 경우
//            return divisionCommentTypeList; // 빈 배열 return
//        } else {
//
//            if (allEquals) { // 모든 부위의 플라그 비율이 동일하고 , '건강'이 아닌 경우
//                divisionCommentTypeList.add(UR.getSummaryComment());
//                divisionCommentTypeList.add(UL.getSummaryComment());
//                divisionCommentTypeList.add(DL.getSummaryComment());
//                divisionCommentTypeList.add(DR.getSummaryComment());
//                return divisionCommentTypeList;
//            }
//
//            // 플라그 비율이 가장 높은 부위
//            Float highestOralCheckRange = Math.max(oralCheck.getOralCheckUpRightRange(), Math.max(oralCheck.getOralCheckUpLeftRange(), Math.max(oralCheck.getOralCheckDownLeftRange(), oralCheck.getOralCheckDownRightRange())));
//
//            // 플라그 비율이 가장 높은 부위와 동일한 값을 가진 부위 List 에 추가
//            if (oralCheck.getOralCheckUpRightRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(UR.getSummaryComment());
//            if (oralCheck.getOralCheckUpLeftRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(UL.getSummaryComment());
//            if (oralCheck.getOralCheckDownLeftRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(DL.getSummaryComment());
//            if (oralCheck.getOralCheckDownRightRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(DR.getSummaryComment());
//
//        }
//
//        return divisionCommentTypeList;
//    }
//
//    /**
//     * 플라그 사진 분석 결과 데이터 추출 및 저장 처리
//     *
//     * @param resource : 플라그 사진 분석 결과
//     * @return OralCheck : 구강 촬영 정보
//     * @throws JsonProcessingException : Json to String 시 예외
//     */
//    @Transactional
//    public OralCheck registAnalysisSuccessData(Long userId, String filePath, OralCheckAnalysisResponse resource) throws JsonProcessingException {
//        OralCheckAnalysisDivisionDto tDivision = resource.getPlaqueStats();
//
//        Float upRightRange = Utils.getDeleteDecimalValue(tDivision.getTopRight(), 1); // 우상 비율
//        Float upLeftRange = Utils.getDeleteDecimalValue(tDivision.getTopLeft(), 1); // 좌상 비율
//        Float downRightRange = Utils.getDeleteDecimalValue(tDivision.getBtmRight(), 1); // 우하 비율
//        Float downLeftRange = Utils.getDeleteDecimalValue(tDivision.getBtmLeft(), 1); // 좌하 비율
//
//        Float totalGroupRatio = (tDivision.getTopRight() + tDivision.getTopLeft() + tDivision.getBtmRight() + tDivision.getBtmLeft()) / 4;
//        Float totalRange = Utils.getDeleteDecimalValue(totalGroupRatio, 1); // 전체 비율
//
//        // 점수 유형
//        OralCheckResultType upRightScoreType = this.calcDivisionScoreType(upRightRange);
//        OralCheckResultType upLeftScoreType = this.calcDivisionScoreType(upLeftRange);
//        OralCheckResultType downRightScoreType = this.calcDivisionScoreType(downRightRange);
//        OralCheckResultType downLeftScoreType = this.calcDivisionScoreType(downLeftRange);
//        OralCheckResultType resultTotalType = this.calcDivisionScoreType(totalRange);
//
//        String resultJsonData = objectMapper.writeValueAsString(resource); // 분석 결과 JSON data 전체
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("유저 없음"));
//
//        // insert 데이터 set
//        OralCheck insertData = OralCheck.builder()
//                .userId(userId)
//                .user(user)
//                .oralCheckPicturePath(filePath)
//                .oralCheckAnalysisState(OralCheckAnalysisState.SUCCESS)
//                .oralCheckTotalRange(totalRange)
//                .oralCheckUpRightRange(upRightRange)
//                .oralCheckUpLeftRange(upLeftRange)
//                .oralCheckDownRightRange(downRightRange)
//                .oralCheckDownLeftRange(downLeftRange)
//                .oralCheckResultJsonData(resultJsonData)
//                .oralCheckResultTotalType(resultTotalType)
//                .oralCheckUpRightScoreType(upRightScoreType)
//                .oralCheckUpLeftScoreType(upLeftScoreType)
//                .oralCheckDownRightScoreType(downRightScoreType)
//                .oralCheckDownLeftScoreType(downLeftScoreType)
//                .build();
//
//        return oralCheckRepository.save(insertData);
//    }
//
//    /**
//     * 구강 사진 분석 실패
//     */
//    @Transactional
//    public OralCheck registAnalysisFailedData(Long userId, String filePath, OralCheckAnalysisResponse resource) throws JsonProcessingException {
//        String resultJsonData = objectMapper.writeValueAsString(resource); // 분석 결과 JSON data 전체
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("유저 없음"));
//        // insert 데이터 set
//        OralCheck insertData = OralCheck.builder()
//                .userId(userId)
//                .oralCheckPicturePath(filePath)
//                .oralCheckAnalysisState(OralCheckAnalysisState.FAIL)
//                .oralCheckResultJsonData(resultJsonData)
//                .build();
//
//        return oralCheckRepository.save(insertData);
//    }
//
//    /**
//     *  구강 검진 결과
//     */
//    @Transactional(readOnly = true)
//    public OralCheckResultDto oralCheckResult(HttpServletRequest httpServletRequest, Long oralCheckId){
//        User user = userService.getTokenUser(httpServletRequest);
//
//        OralCheck oralCheck = oralCheckRepository.findById(oralCheckId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 구강 검진입니다."));
//        if (!oralCheck.getUserId().equals(user.getUserId())) throw new BadRequestApiException("회원 정보와 구강 검진 정보가 일치하지 않습니다.");
//
////        if (!oralCheck.getUserId().equals(user.getUserId())) throw new BadRequestApiException("회원 정보와 구강 검진 정보가 일치하지 않습니다.");
//
//        // 부위별 코멘트 리스트
//        List<String> oralCheckCommentList = this.calcDivisionCommentType(oralCheck);
//
//        return OralCheckResultDto.builder()
//                .userId(user.getUserId())
//                .oralCheckResultTotalType(oralCheck.getOralCheckResultTotalType())
//                .created(oralCheck.getCreated())
//                .oralCheckTotalRange(oralCheck.getOralCheckTotalRange())
//                .oralCheckUpRightRange(oralCheck.getOralCheckUpRightRange())
//                .oralCheckUpRightScoreType(oralCheck.getOralCheckUpRightScoreType())
//                .oralCheckUpLeftRange(oralCheck.getOralCheckUpLeftRange())
//                .oralCheckUpLeftScoreType(oralCheck.getOralCheckUpLeftScoreType())
//                .oralCheckDownLeftRange(oralCheck.getOralCheckDownLeftRange())
//                .oralCheckDownLeftScoreType(oralCheck.getOralCheckDownLeftScoreType())
//                .oralCheckDownRightRange(oralCheck.getOralCheckDownRightRange())
//                .oralCheckDownRightScoreType(oralCheck.getOralCheckDownRightScoreType())
//                .oralCheckCommentList(oralCheckCommentList)
//                .build();
//
//    }
//
//    /**
//     *  구강 상태 조회
//     */
//    @Transactional(readOnly = true)
//    public OralCheckDto oralCheck(HttpServletRequest httpServletRequest){
//        User user = userService.getTokenUser(httpServletRequest);
//
//        List<OralCheck> oralCheckList = oralCheckRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
//        List<ToothBrushing> toothBrushingList = toothBrushingRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
//        List<Questionnaire> questionnaireList = questionnaireRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
//        List<UserOralStatus> userOralStatusList = userOralStatusRepository.findAllByQuestionnaireIn(questionnaireList);
//        List<OralStatus> oralStatusList = oralStatusRepository.findAll();
//
//        final String datePattern = "yyyy-MM-dd";
//
//        Calendar calendar = Calendar.getInstance();
//
//        Date today = calendar.getTime();
//        String todayString = DateFormatUtil.dateToString(datePattern, today);
//
//        // 구강 상태 화면 최상단 섹션 순서
//        List<OralCheckSectionListDto> sectionList = new ArrayList<>();
//        // 구강 촬영
//        OralCheck latestOralCheck = oralCheckList.size() > 0 ? oralCheckList.get(0) : null;
//        sectionList.add(OralCheckSectionListDto.builder()
//            .sectionType(OralSectionType.ORAL_CHECK)
//            .date(latestOralCheck != null ? latestOralCheck.getCreated() : null)
//            .timeInterval(latestOralCheck != null ? (today.getTime() - latestOralCheck.getCreated().getTime()) / 1000 : null)
//            .build());
//
//        // 권장 촬영 기간
//        String oralCheckPeriodBefore = null;
//        String oralCheckPeriodAfter = null;
//        if (latestOralCheck != null) {
//            calendar.setTime(latestOralCheck.getCreated());
//            calendar.add(Calendar.DATE, 6);
//            oralCheckPeriodBefore = DateFormatUtil.dateToString(datePattern, calendar.getTime());
//            calendar.add(Calendar.DATE, 2);
//            oralCheckPeriodAfter = DateFormatUtil.dateToString(datePattern, calendar.getTime());
//        }
//
//        calendar.setTime(today);
//        calendar.add(Calendar.DATE, -30); // 30일 전 기준
//
//        // 양치질
//        ToothBrushing latestToothBrushing = toothBrushingList.size() > 0 ? toothBrushingList.get(0) : null;
//        sectionList.add(OralCheckSectionListDto.builder()
//            .sectionType(OralSectionType.TOOTH_BRUSHING)
//            .date(latestToothBrushing != null ? latestToothBrushing.getCreated() : null)
//            .timeInterval(latestToothBrushing != null ? (today.getTime() - latestToothBrushing.getCreated().getTime()) / 1000 : null)
//            .toothBrushingList(toothBrushingList.stream()
//                .filter(toothBrushing -> DateFormatUtil.dateToString(datePattern, toothBrushing.getCreated())
//                    .equals(DateFormatUtil.dateToString(datePattern, today)))
//                .map(toothBrushing -> ToothBrushingDto.builder()
//                    .toothBrushingId(toothBrushing.getToothBrushingId())
//                    .created(toothBrushing.getCreated())
//                    .build())
//                .sorted(Comparator.comparing(ToothBrushingDto::getCreated))
//                .collect(Collectors.toList())
//            )
//            .build());
//        // 문진표
//        Questionnaire latestQuestionnaire = questionnaireList.size() > 0 ? questionnaireList.get(0) : null;
//        // 문진표 작성 이력이 없거나 30일이 지난 경우 두번째 아니면 세번째
//        sectionList.add(latestQuestionnaire == null || latestQuestionnaire.getCreated().before(calendar.getTime()) ? 1 : 2, OralCheckSectionListDto.builder()
//            .sectionType(OralSectionType.QUESTIONNAIRE)
//            .date(latestQuestionnaire != null ? latestQuestionnaire.getCreated() : null)
//            .timeInterval(latestQuestionnaire != null ? (today.getTime() - latestQuestionnaire.getCreated().getTime()) / 1000 : null)
//            .build());
//
//        // sectionList에 sort 값 추가
//        for (int i = 0; i < sectionList.size(); i++) {
//            sectionList.get(i).setSort(i + 1);
//        }
//
//        // 요일별 나의 구강 상태
//        List<OralCheckDailyDto> dailyList = new ArrayList<>();
//        calendar.add(Calendar.DATE, 1 - calendar.get(Calendar.DAY_OF_WEEK)); // 30일 전날이 포함된 일요일부터 시작
//
//        while (true) {
//            List<OralCheckListDto> detailList = new ArrayList<>();
//            String dateString = DateFormatUtil.dateToString(datePattern, calendar.getTime());
//
//            // 구강 촬영
//            detailList.addAll(oralCheckList.stream()
//                .filter(oralCheck -> DateFormatUtil.dateToString(datePattern, oralCheck.getCreated()).equals(dateString))
//                .map(oralCheck -> OralCheckListDto.builder()
//                    .sectionType(OralSectionType.ORAL_CHECK)
//                    .date(oralCheck.getCreated())
//                    .identifier(oralCheck.getOralCheckId())
//                    .oralCheckResultTotalType(oralCheck.getOralCheckResultTotalType())
//                    .build()
//                ).toList());
//
//            // 양치질
//            List<ToothBrushing> dailyToothBrushingList = toothBrushingList.stream()
//                .filter(toothBrushing -> DateFormatUtil.dateToString(datePattern, toothBrushing.getCreated()).equals(dateString)).toList();
//            for (int i = 0; i < dailyToothBrushingList.size(); i++) {
//                ToothBrushing toothBrushing = dailyToothBrushingList.get(i);
//                detailList.add(OralCheckListDto.builder()
//                    .sectionType(OralSectionType.TOOTH_BRUSHING)
//                    .date(toothBrushing.getCreated())
//                    .identifier(toothBrushing.getToothBrushingId())
//                    .toothBrushingCount(dailyToothBrushingList.size() - i) // 역순
//                    .build()
//                );
//            }
//
//            // 문진표
//            List<Questionnaire> dailyQuestionnaireList = questionnaireList.stream()
//                .filter(questionnaire -> DateFormatUtil.dateToString(datePattern, questionnaire.getCreated()).equals(dateString)).toList();
//            detailList.addAll(dailyQuestionnaireList.stream()
//                .map(questionnaire -> OralCheckListDto.builder()
//                    .sectionType(OralSectionType.QUESTIONNAIRE)
//                    .date(questionnaire.getCreated())
//                    .identifier(questionnaire.getQuestionnaireId())
//                    .oralStatusList(userOralStatusList.stream()
//                        .filter(userOralStatus -> userOralStatus.getQuestionnaire().equals(questionnaire))
//                        .map(userOralStatus -> {
//                            OralStatus oralStatus = oralStatusList.stream()
//                                .filter(o -> o.equals(userOralStatus.getOralStatus()))
//                                .findAny().orElseThrow(() -> new NotFoundDataException("구강 상태 결과 정보가 없습니다."));
//
//                            return OralStatusTypeDto.builder()
//                                .type(oralStatus.getOralStatusType())
//                                .title(oralStatus.getOralStatusTitle())
//                                .build();
//                        })
//                        .toList())
//                    .build()
//                ).toList());
//
//            // 전체 목록을 역순으로 정렬
//            detailList.sort(Comparator.comparing(OralCheckListDto::getDate).reversed());
//
//            OralDateStatusType dailyStatusType = null;
//            if (dateString.equals(todayString)) {
//                // 오늘인 경우 오늘 상태값 적용
//                dailyStatusType = OralDateStatusType.TODAY;
//            } else if (!detailList.isEmpty()) {
//                // 양치를 제외한 가장 최신의 상태값 적용
//                for (OralCheckListDto dto : detailList) {
//                    switch (dto.getSectionType()) {
//                        case ORAL_CHECK -> {
//                            switch (dto.getOralCheckResultTotalType()) {
//                                case HEALTHY -> dailyStatusType = OralDateStatusType.HEALTHY;
//                                case GOOD -> dailyStatusType = OralDateStatusType.GOOD;
//                                case ATTENTION -> dailyStatusType = OralDateStatusType.ATTENTION;
//                                case DANGER -> dailyStatusType = OralDateStatusType.DANGER;
//                            }
//                        }
//                        case QUESTIONNAIRE -> dailyStatusType = OralDateStatusType.QUESTIONNAIRE;
//                        default -> {
//                            continue;
//                        }
//                    }
//                    break;
//                }
//            }
//            if (dailyStatusType == null && latestOralCheck != null && dateString.compareTo(oralCheckPeriodBefore) >= 0 && dateString.compareTo(oralCheckPeriodAfter) <= 0) {
//                // 권장 촬영기간
//                dailyStatusType = OralDateStatusType.ORAL_CHECK_PERIOD;
//            }
//
//            dailyList.add(OralCheckDailyDto.builder()
//                .date(calendar.getTime())
//                .status(dailyStatusType)
//                .questionnaire(dailyQuestionnaireList.size() > 0)
//                .detailList(detailList)
//                .build());
//
//            if (dateString.compareTo(todayString) >= 0 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) break; // 이번 주 토요일까지
//            calendar.add(Calendar.DATE, 1);
//        }
//
//        return OralCheckDto.builder()
//                .sectionList(sectionList)
//                .dailyList(dailyList)
//                .build();
//    }
//
//    /**
//     * 대시보드 조회
//     */
//    @Cacheable(value = "dashboard", key = "@userService.getTokenUser(#p0).getUserId() + '_' + T(java.time.LocalDate).now()")
//    public DashboardDto dashboard(HttpServletRequest httpServletRequest) {
//        User user = userService.getTokenUser(httpServletRequest);
//
//        List<OralCheck> oralCheckList = oralCheckRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
//
//        // 구강 촬영을 한 번이라도 하지 않으면 아무 데이터도 보이지 않음
//        if (oralCheckList.size() == 0) {
//            return new DashboardDto();
//        }
//
//        OralCheck latestOralCheck = oralCheckList.get(0);
//
//        List<ToothBrushingDailyCountDto> toothBrushingDailyCountList = toothBrushingCustomRepository.getDailyCount(user.getUserId());
//        // 양치 수
//        int toothBrushingTotalCount = toothBrushingDailyCountList.stream().mapToInt(ToothBrushingDailyCountDto::getCount).sum();
//
//        QuestionnaireAndStatusDto latestQuestionnaire = questionnaireCustomRepository.getLatestQuestionnaireAndHigherStatus(user.getUserId());
//
//        Calendar calendar = Calendar.getInstance();
//        Date today = calendar.getTime();
//
//        int oralCheckHealthyCount = 0;
//        int oralCheckGoodCount = 0;
//        int oralCheckAttentionCount = 0;
//        int oralCheckDangerCount = 0;
//        List<OralCheckDailyChangeDto> oralCheckDailyChangeList = new ArrayList<>();
//
//        String beforeDate = "";
//        for (int i = 0; i < oralCheckList.size(); i++) {
//            OralCheck oralCheck = oralCheckList.get(i);
//            // 구강 상태값 카운트
//            switch (oralCheck.getOralCheckResultTotalType()) {
//                case HEALTHY -> oralCheckHealthyCount++;
//                case GOOD -> oralCheckGoodCount++;
//                case ATTENTION -> oralCheckAttentionCount++;
//                case DANGER -> oralCheckDangerCount++;
//            }
//
//            // 구강 상태 변화 추이
//            if (oralCheckDailyChangeList.size() >= 10) {
//                continue;
//            }
//            String dateString = DateFormatUtil.dateToString("yyyy-MM-dd", oralCheck.getCreated());
//            if (beforeDate.equals(dateString)) {
//                continue;
//            }
//
//            oralCheckDailyChangeList.add(0, new OralCheckDailyChangeDto(oralCheckList.size() - i, oralCheck.getOralCheckResultTotalType()));
//            beforeDate = dateString;
//        }
//
//        // 5개 미만인 경우 미노출
//        if (oralCheckDailyChangeList.size() < 5) {
//            oralCheckDailyChangeList = new ArrayList<>();
//        }
//
//        return DashboardDto.builder()
//            .latestOralCheckId(latestOralCheck.getOralCheckId())
//            .oralCheckTimeInterval((today.getTime() - latestOralCheck.getCreated().getTime()) / 1000)
//            .oralCheckTotalCount(oralCheckList.size())
//            .oralCheckHealthyCount(oralCheckHealthyCount)
//            .oralCheckGoodCount(oralCheckGoodCount)
//            .oralCheckAttentionCount(oralCheckAttentionCount)
//            .oralCheckDangerCount(oralCheckDangerCount)
//            .toothBrushingTotalCount(toothBrushingTotalCount)
//            .toothBrushingAverage(Utils.getDeleteDecimalValue((float) toothBrushingTotalCount / toothBrushingDailyCountList.size(), 1))
//            .oralStatus(latestQuestionnaire != null ? new OralStatusTypeDto(latestQuestionnaire.getOralStatusType(), latestQuestionnaire.getOralStatusTitle()) : null)
//            .questionnaireCreated(latestQuestionnaire != null ? latestQuestionnaire.getQuestionnaireCreated() : null)
//            .oralCheckResultTotalType(latestOralCheck.getOralCheckResultTotalType())
//            .oralCheckUpRightScoreType(latestOralCheck.getOralCheckUpRightScoreType())
//            .oralCheckUpLeftScoreType(latestOralCheck.getOralCheckUpLeftScoreType())
//            .oralCheckDownLeftScoreType(latestOralCheck.getOralCheckDownLeftScoreType())
//            .oralCheckDownRightScoreType(latestOralCheck.getOralCheckDownRightScoreType())
//            .oralCheckDailyList(oralCheckDailyChangeList)
//            .build();
//    }
//
//    /**
//     *  전체 평균 구강 상태
//     */
//    public OralCheckResultType getState(OralCheckResultTypeCount oralCheckResultTypeCount){
//        if (oralCheckResultTypeCount.getCountHealthy() >= oralCheckResultTypeCount.getCountGood() &&
//                oralCheckResultTypeCount.getCountHealthy() >= oralCheckResultTypeCount.getCountAttention() &&
//                oralCheckResultTypeCount.getCountHealthy() >= oralCheckResultTypeCount.getCountDanger())
//            return OralCheckResultType.HEALTHY;
//
//        if (oralCheckResultTypeCount.getCountGood() >= oralCheckResultTypeCount.getCountAttention() &&
//                oralCheckResultTypeCount.getCountGood() >= oralCheckResultTypeCount.getCountDanger())
//            return OralCheckResultType.GOOD;
//
//        if (oralCheckResultTypeCount.getCountAttention() >= oralCheckResultTypeCount.getCountDanger())
//            return OralCheckResultType.ATTENTION;
//
//        return OralCheckResultType.DANGER;
//    }
//
//    @Transactional
//    public OralCheckDto createOralCheck(MultipartFile file, Long userId) {
//
//        if (file.isEmpty()) {
//            throw new IllegalArgumentException("파일이 비어있습니다.");
//        }
//
//        try {
//            // 파일 저장
//            String uploadDir = "uploads/oralcheck/";
//            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//            Path filePath = Paths.get(uploadDir + fileName);
//            Files.createDirectories(filePath.getParent());
//            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//            // 사용자 조회
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
//
//            // OralCheck 저장
//            OralCheck oralCheck = OralCheck.builder()
//                    .user(user)
//                    .oralCheckPicturePath(filePath.toString())
//                    .oralCheckAnalysisState(OralCheckAnalysisState.WAITING) // 초기값
//                    .build();
//
//            oralCheckRepository.save(oralCheck);
//
//            return new OralCheckDto(oralCheck.getOralCheckId(), oralCheck.getOralCheckPicturePath());
//        } catch (IOException e) {
//            throw new RuntimeException("파일 업로드 실패", e);
//        }
//    }
//
//}
