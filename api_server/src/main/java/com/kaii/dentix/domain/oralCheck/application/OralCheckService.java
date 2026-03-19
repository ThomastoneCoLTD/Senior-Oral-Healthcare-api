package com.kaii.dentix.domain.oralCheck.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.oralCheck.dto.resoponse.OralCheckAnalysisResponse;
import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.oralStatus.dto.OralStatusDto;
import com.kaii.dentix.domain.oralStatus.jpa.OralStatusRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.application.OrganizationSubscriptionHistoryService;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireCustomRepository;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireDto;
import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingCustomRepository;
import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingRepository;
import com.kaii.dentix.domain.toothBrushing.domain.ToothBrushing;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDailyCountDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDto;
import com.kaii.dentix.domain.type.OralDateStatusType;
import com.kaii.dentix.domain.type.OralSectionType;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.oralStatusAssignment.dao.OralStatusAssignmentRepository;
import com.kaii.dentix.global.common.aws.AWSS3Service;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.util.AiModelService;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.kaii.dentix.global.common.util.Utils;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.kaii.dentix.domain.type.oral.OralCheckDivisionCommentType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OralCheckService {

    private final UserService userService;
    private final AWSS3Service awss3Service;
    private final ObjectMapper objectMapper;
    private final AiModelService aiModelService;
    private final BillingService billingService;
    private final UserRepository userRepository;
    private final OralCheckRepository oralCheckRepository;
    private final OralStatusRepository oralStatusRepository;
    private final ToothBrushingRepository toothBrushingRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final OralStatusAssignmentRepository oralStatusAssignmentRepository;
    private final ToothBrushingCustomRepository toothBrushingCustomRepository;
    private final QuestionnaireCustomRepository questionnaireCustomRepository;
    private final OrganizationSubscriptionHistoryService organizationSubscriptionHistoryService;


    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${s3.folderPath.oralCheck}")
    private String folderPath;

    /**
     * 구강검진 사진 촬영 + AI 분석 + 과금 처리
     */
    @Transactional
    @CacheEvict(
            value = "dashboard",
            key = "@userService.getTokenUser(#p0).getUserId() + '_' + T(java.time.LocalDate).now()"
    )
    public DataResponse<OralCheckDto.PhotoResponse> oralCheckPhoto(
            HttpServletRequest request,
            MultipartFile file,
            String type
    ) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {

        long startTime = System.currentTimeMillis();

        // 업로드 API 요청 / 서버 수신 로그
        log.info("[POST_UPLOAD_API_REQUEST] requestUri={}, method={}, contentType={}, type={}",
                request.getRequestURI(),
                request.getMethod(),
                request.getContentType(),
                type
        );

        log.info("[Server reception] 파일 수신 시작 - originalFilename={}, size={}, contentType={}",
                file != null ? file.getOriginalFilename() : "null",
                file != null ? file.getSize() : 0,
                file != null ? file.getContentType() : "null"
        );

        // 1. 사용자 인증 및 기관 확인
        User user = userService.getTokenUser(request);
        Organization organization = user.getOrganization();

        log.info("[Server reception] 사용자 인증 완료 - userId={}, organizationId={}",
                user.getUserId(),
                organization != null ? organization.getOrganizationId() : null
        );

        if (organization == null) {
            log.error("[Server reception] 기관 정보 없음 - userId={}", user.getUserId());
            throw new BadRequestApiException("기관 정보를 찾을 수 없습니다.");
        }

        OrganizationSubscriptionHistory activeHistory =
                organizationSubscriptionHistoryService.getActiveHistory(organization);

        if (activeHistory == null) {
            log.error("[Server reception] 활성 구독 정보 없음 - organizationId={}", organization.getOrganizationId());
            throw new BadRequestApiException("기관의 구독 정보가 없습니다.");
        }

        // 2. 파일 업로드
        String uploadedUrl = awss3Service.upload(file, folderPath, true);

        if (StringUtils.isBlank(uploadedUrl)) {
            log.error("[Image Upload Fail] 파일 업로드 실패 - userId={}, organizationId={}, originalFilename={}",
                    user.getUserId(),
                    organization.getOrganizationId(),
                    file != null ? file.getOriginalFilename() : "null"
            );
            throw new BadRequestApiException("구강 촬영 결과 저장에 실패했어요.\n관리자에게 문의해 주세요.");
        }

        log.info("[Upload Image URL] 업로드 완료 - userId={}, organizationId={}, originalFilename={}, uploadedUrl={}",
                user.getUserId(),
                organization.getOrganizationId(),
                file != null ? file.getOriginalFilename() : "null",
                uploadedUrl
        );

        // 3. AI 분석 요청 및 데이터 변환
        OralCheckDto.AnalysisResponse analysisData;

        try {
            log.info("[Analysis started] AI 분석 요청 시작 - userId={}, organizationId={}, uploadedUrl={}",
                    user.getUserId(),
                    organization.getOrganizationId(),
                    uploadedUrl
            );

            OralCheckAnalysisResponse oldResponse = aiModelService.getPyDentalAiModel(file).get();

            analysisData = OralCheckDto.AnalysisResponse.builder()
                    .statusCode(oldResponse.getStatusCode())
                    .statusMsg(oldResponse.getStatusMsg())
                    .plaqueStats(OralCheckDto.AnalysisDivision.builder()
                            .topRight(oldResponse.getPlaqueStats().getTopRight())
                            .topLeft(oldResponse.getPlaqueStats().getTopLeft())
                            .btmRight(oldResponse.getPlaqueStats().getBtmRight())
                            .btmLeft(oldResponse.getPlaqueStats().getBtmLeft())
                            .build())
                    .build();

            log.info("[Analysis Result Response] AI 분석 응답 수신 - statusCode={}, statusMsg={}",
                    analysisData.getStatusCode(),
                    analysisData.getStatusMsg()
            );

        } catch (ExecutionException e) {
            log.error("[Analysis fail] AI 모델 실행 실패 - userId={}, organizationId={}",
                    user.getUserId(),
                    organization.getOrganizationId(),
                    e.getCause()
            );

            if ("dev".equals(activeProfile)) {
                log.warn("[Analysis replacement] 개발 환경 더미 데이터 사용");
                Random random = new Random();
                analysisData = OralCheckDto.AnalysisResponse.builder()
                        .statusCode(200)
                        .statusMsg("DEV_DUMMY")
                        .plaqueStats(OralCheckDto.AnalysisDivision.builder()
                                .topRight(random.nextFloat(50))
                                .topLeft(random.nextFloat(50))
                                .btmRight(random.nextFloat(50))
                                .btmLeft(random.nextFloat(50))
                                .build())
                        .build();
            } else {
                Throwable cause = e.getCause();
                log.error("AI 모델 실행 실패. type={}, message={}",
                        cause != null ? cause.getClass().getName() : "null",
                        cause != null ? cause.getMessage() : "null",
                        cause);
                return new DataResponse<>(502, "AI 분석 서버 오류가 발생했습니다.", null);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Throwable cause = e.getCause();
            log.error("[Analysis fail] AI 요청 인터럽트 - type={}, message={}",
                    cause != null ? cause.getClass().getName() : "null",
                    cause != null ? cause.getMessage() : "null",
                    cause
            );
            return new DataResponse<>(500, "요청 처리 중 오류가 발생했습니다.", null);
        }

        // 4. 분석 결과 저장
        OralCheck oralCheck;
        boolean success = false;
        int statusCode = analysisData.getStatusCode();

        if (statusCode == 200) {
            oralCheck = registAnalysisSuccessData(
                    user.getUserId(),
                    uploadedUrl,
                    analysisData,
                    activeHistory
            );
            success = true;
        } else {
            if (statusCode == 402 || statusCode == 403 || statusCode == 404) {
                log.warn("[Analysis Fail] status={}", statusCode);
            } else {
                log.error("[Analysis Fail] status={}", statusCode);
            }

            oralCheck = registAnalysisFailedData(
                    user.getUserId(),
                    uploadedUrl,
                    analysisData,
                    activeHistory
            );
        }

        if (oralCheck == null) {
            log.error("[Analysis fail] oralCheck 저장 실패 - userId={}, uploadedUrl={}",
                    user.getUserId(),
                    uploadedUrl
            );
            throw new BadRequestApiException("구강 촬영 결과 저장에 실패했어요.\n관리자에게 문의해 주세요.");
        }

        // 결과 저장 로그
        log.info("[Analysis Finish] oralCheckId={}, success={}, analysisState={}, userId={}, organizationId={}, uploadedUrl={}",
                oralCheck.getOralCheckId(),
                success,
                oralCheck.getOralCheckAnalysisState(),
                user.getUserId(),
                organization.getOrganizationId(),
                uploadedUrl
        );

        // 5. 과금 처리
        if (success) {
            activeHistory.increaseSuccessCount();
            if (activeHistory.isOverused()) {
                log.warn("[OVERUSES] 기관 [{}] 사용량 초과 감지 → 자동 과금 생성", organization.getOrganizationId());
                billingService.createOveruseBatchBilling(organization);
            }
        }

        int remaining = Math.max(activeHistory.getRemainingResponses(), 0);

        String responseMessage = success ? "AI 분석이 완료되었습니다." : "AI 분석에 실패했습니다.";

        OralCheckDto.PhotoResponse responseBody = OralCheckDto.PhotoResponse.builder()
                .oralCheckId(oralCheck.getOralCheckId())
                .success(success)
                .remainingResponses(remaining)
                .organizationId(organization.getOrganizationId())
                .build();

        DataResponse<OralCheckDto.PhotoResponse> response = new DataResponse<>(
                200,
                responseMessage,
                responseBody
        );

        // 응답 로그
        log.info("[POST_UPLOAD_API_REQUEST] status=200, message={}, oralCheckId={}, success={}, remainingResponses={}, elapsedMs={}",
                responseMessage,
                responseBody.getOralCheckId(),
                responseBody.isSuccess(),
                responseBody.getRemainingResponses(),
                System.currentTimeMillis() - startTime
        );

        return response;
    }

    /**
     * 4등분 점수 유형 계산
     *
     * @param divisionRange : 영역 비율
     * @return ToothColoringDivisionScoreType : 4등분 점수 유형 결과
     */
    public OralCheckResultType calcDivisionScoreType(Float divisionRange) {
        return divisionRange < 1 ? OralCheckResultType.HEALTHY
                : divisionRange < 10 ? OralCheckResultType.GOOD
                : divisionRange < 30 ? OralCheckResultType.ATTENTION
                : OralCheckResultType.DANGER;
    }

    /**
     * 4등분 코멘트 유형 계산
     */
    public List<String> calcDivisionCommentType(OralCheck oralCheck) {

        // 부위별 구강 상태 Comment
        List<String> divisionCommentTypeList = new ArrayList<>();

        // 모든 부위의 플라그 수치가 동일한 경우 true
        boolean allEquals = (oralCheck.getOralCheckUpRightRange().equals(oralCheck.getOralCheckUpLeftRange())) &&
                (oralCheck.getOralCheckUpLeftRange().equals(oralCheck.getOralCheckDownRightRange()) &&
                        (oralCheck.getOralCheckDownRightRange().equals(oralCheck.getOralCheckDownLeftRange())));

        if (allEquals && oralCheck.getOralCheckResultTotalType().equals(OralCheckResultType.HEALTHY)) { // 모든 부위의 플라그 비율이 동일하고 , '건강'인 경우
            return divisionCommentTypeList; // 빈 배열 return
        } else {

            if (allEquals) { // 모든 부위의 플라그 비율이 동일하고 , '건강'이 아닌 경우
                divisionCommentTypeList.add(UR.getSummaryComment());
                divisionCommentTypeList.add(UL.getSummaryComment());
                divisionCommentTypeList.add(DL.getSummaryComment());
                divisionCommentTypeList.add(DR.getSummaryComment());
                return divisionCommentTypeList;
            }

            // 플라그 비율이 가장 높은 부위
            Float highestOralCheckRange = Math.max(oralCheck.getOralCheckUpRightRange(), Math.max(oralCheck.getOralCheckUpLeftRange(), Math.max(oralCheck.getOralCheckDownLeftRange(), oralCheck.getOralCheckDownRightRange())));

            // 플라그 비율이 가장 높은 부위와 동일한 값을 가진 부위 List 에 추가
            if (oralCheck.getOralCheckUpRightRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(UR.getSummaryComment());
            if (oralCheck.getOralCheckUpLeftRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(UL.getSummaryComment());
            if (oralCheck.getOralCheckDownLeftRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(DL.getSummaryComment());
            if (oralCheck.getOralCheckDownRightRange().equals(highestOralCheckRange)) divisionCommentTypeList.add(DR.getSummaryComment());

        }

        return divisionCommentTypeList;
    }

    /**
     * 플라그 사진 분석 결과 데이터 추출 및 저장 처리
     *
     * @param resource : 플라그 사진 분석 결과
     * @return OralCheck : 구강 촬영 정보
     * @throws JsonProcessingException : Json to String 시 예외
     */
    @Transactional
    public OralCheck registAnalysisSuccessData(
            Long userId,
            String filePath,
            OralCheckDto.AnalysisResponse resource,
            OrganizationSubscriptionHistory subscriptionHistory
    ) throws JsonProcessingException {
        OralCheckDto.AnalysisDivision tDivision = resource.getPlaqueStats();

        Float upRightRange = Utils.getDeleteDecimalValue(tDivision.getTopRight(), 1);
        Float upLeftRange = Utils.getDeleteDecimalValue(tDivision.getTopLeft(), 1);
        Float downRightRange = Utils.getDeleteDecimalValue(tDivision.getBtmRight(), 1);
        Float downLeftRange = Utils.getDeleteDecimalValue(tDivision.getBtmLeft(), 1);

        Float totalGroupRatio = (tDivision.getTopRight() + tDivision.getTopLeft() + tDivision.getBtmRight() + tDivision.getBtmLeft()) / 4;
        Float totalRange = Utils.getDeleteDecimalValue(totalGroupRatio, 1);

        OralCheckResultType upRightScoreType = this.calcDivisionScoreType(upRightRange);
        OralCheckResultType upLeftScoreType = this.calcDivisionScoreType(upLeftRange);
        OralCheckResultType downRightScoreType = this.calcDivisionScoreType(downRightRange);
        OralCheckResultType downLeftScoreType = this.calcDivisionScoreType(downLeftRange);
        OralCheckResultType resultTotalType = this.calcDivisionScoreType(totalRange);

        String resultJsonData = objectMapper.writeValueAsString(resource);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        OralCheck insertData = OralCheck.builder()
                .user(user)
                .oralCheckPicturePath(filePath)
                .subscriptionHistory(subscriptionHistory)
                .oralCheckAnalysisState(OralCheckAnalysisState.SUCCESS)
                .oralCheckTotalRange(totalRange)
                .oralCheckUpRightRange(upRightRange)
                .oralCheckUpLeftRange(upLeftRange)
                .oralCheckDownRightRange(downRightRange)
                .oralCheckDownLeftRange(downLeftRange)
                .oralCheckResultJsonData(resultJsonData)
                .oralCheckResultTotalType(resultTotalType)
                .oralCheckUpRightScoreType(upRightScoreType)
                .oralCheckUpLeftScoreType(upLeftScoreType)
                .oralCheckDownRightScoreType(downRightScoreType)
                .oralCheckDownLeftScoreType(downLeftScoreType)
                .build();

        return oralCheckRepository.save(insertData);
    }
    /**
     * 구강 사진 분석 실패
     */
    @Transactional
    public OralCheck registAnalysisFailedData(
            Long userId,
            String filePath,
            OralCheckDto.AnalysisResponse resource,
            OrganizationSubscriptionHistory subscriptionHistory
    ) throws JsonProcessingException {

        String resultJsonData = objectMapper.writeValueAsString(resource);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        OralCheck insertData = OralCheck.builder()
                .user(user)
                .subscriptionHistory(subscriptionHistory)
                .oralCheckPicturePath(filePath)
                .oralCheckAnalysisState(OralCheckAnalysisState.FAIL)
                .oralCheckResultJsonData(resultJsonData)
                .build();

        return oralCheckRepository.save(insertData);
    }

    /**
     *  구강 검진 결과
     */
    @Transactional(readOnly = true)
    public OralCheckDto.ResultResponse oralCheckResult(HttpServletRequest httpServletRequest, Long oralCheckId) {
        User user = userService.getTokenUser(httpServletRequest);

        OralCheck oralCheck = oralCheckRepository.findById(oralCheckId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 구강 검진입니다."));

        if (!oralCheck.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestApiException("회원 정보와 구강 검진 정보가 일치하지 않습니다.");
        }

        if (oralCheck.getOralCheckAnalysisState() != OralCheckAnalysisState.SUCCESS
                || oralCheck.getOralCheckResultTotalType() == null) {
            return OralCheckDto.ResultResponse.builder()
                    .userId(user.getUserId())
                    .organizationId(user.getOrganization().getOrganizationId())
                    .success(false)
                    .created(oralCheck.getCreated())
                    .oralCheckCommentList(List.of())
                    .build();
        }

        List<String> oralCheckCommentList = this.calcDivisionCommentType(oralCheck);

        // [수정] 통합 DTO의 내부 클래스(ResultResponse) 빌더 사용
        return OralCheckDto.ResultResponse.builder()
                .userId(user.getUserId())
                .organizationId(user.getOrganization().getOrganizationId())
                .success(oralCheck.getOralCheckAnalysisState() == OralCheckAnalysisState.SUCCESS)
                .oralCheckResultTotalType(oralCheck.getOralCheckResultTotalType())
                .created(oralCheck.getCreated())
                .oralCheckTotalRange(oralCheck.getOralCheckTotalRange())
                .oralCheckUpRightRange(oralCheck.getOralCheckUpRightRange())
                .oralCheckUpRightScoreType(oralCheck.getOralCheckUpRightScoreType())
                .oralCheckUpLeftRange(oralCheck.getOralCheckUpLeftRange())
                .oralCheckUpLeftScoreType(oralCheck.getOralCheckUpLeftScoreType())
                .oralCheckDownLeftRange(oralCheck.getOralCheckDownLeftRange())
                .oralCheckDownLeftScoreType(oralCheck.getOralCheckDownLeftScoreType())
                .oralCheckDownRightRange(oralCheck.getOralCheckDownRightRange())
                .oralCheckDownRightScoreType(oralCheck.getOralCheckDownRightScoreType())
                .oralCheckCommentList(oralCheckCommentList)
                // 필요하다면 remainingResponses 값도 설정 (구독 정보 등에서 조회 필요 시)
                .build();
    }

    /**
     *  구강 상태 조회
     */
    @Transactional(readOnly = true)
    public OralCheckDto.TimelineResponse oralCheck(HttpServletRequest httpServletRequest) {
        User user = userService.getTokenUser(httpServletRequest);

        List<OralCheck> oralCheckList = oralCheckRepository.findAllByUser_UserIdOrderByCreatedDesc(user.getUserId());
        List<OralCheck> successfulOralCheckList = oralCheckList.stream()
                .filter(oralCheck -> oralCheck.getOralCheckAnalysisState() == OralCheckAnalysisState.SUCCESS)
                .filter(oralCheck -> oralCheck.getOralCheckResultTotalType() != null)
                .toList();
        List<ToothBrushing> toothBrushingList = toothBrushingRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
        List<Questionnaire> questionnaireList = questionnaireRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
        Map<Long, List<OralStatusDto.OralStatusType>> oralStatusByQuestionnaireId = buildOralStatusByQuestionnaireId(questionnaireList);

        final String datePattern = "yyyy-MM-dd";
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        String todayString = DateFormatUtil.dateToString(datePattern, today);
        Date timelineStartDate = resolveTimelineStartDate(today, oralCheckList, toothBrushingList, questionnaireList);

        // 상단 섹션
        List<OralCheckDto.Section> sectionList = new ArrayList<>();

        // 1. 구강 촬영
        OralCheck latestOralCheck = !successfulOralCheckList.isEmpty() ? successfulOralCheckList.get(0) : null;
        sectionList.add(OralCheckDto.Section.builder()
                .sectionType(OralSectionType.ORAL_CHECK)
                .date(latestOralCheck != null ? latestOralCheck.getCreated() : null)
                .timeInterval(latestOralCheck != null ? (today.getTime() - latestOralCheck.getCreated().getTime()) / 1000 : null)
                .build());

        // 권장 촬영 기간 계산
        String oralCheckPeriodBefore = null;
        String oralCheckPeriodAfter = null;
        if (latestOralCheck != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(latestOralCheck.getCreated());
            c.add(Calendar.DATE, 6);
            oralCheckPeriodBefore = DateFormatUtil.dateToString(datePattern, c.getTime());
            c.add(Calendar.DATE, 2);
            oralCheckPeriodAfter = DateFormatUtil.dateToString(datePattern, c.getTime());
        }

        Calendar recentWindowCalendar = Calendar.getInstance();
        recentWindowCalendar.setTime(today);
        recentWindowCalendar.add(Calendar.DATE, -30);

        // 2. 양치질
        ToothBrushing latestToothBrushing = !toothBrushingList.isEmpty() ? toothBrushingList.get(0) : null;
        sectionList.add(OralCheckDto.Section.builder()
                .sectionType(OralSectionType.TOOTH_BRUSHING)
                .date(latestToothBrushing != null ? latestToothBrushing.getCreated() : null)
                .timeInterval(latestToothBrushing != null ? (today.getTime() - latestToothBrushing.getCreated().getTime()) / 1000 : null)
                .toothBrushingList(toothBrushingList.stream()
                        .filter(tb -> DateFormatUtil.dateToString(datePattern, tb.getCreated()).equals(todayString))
                        .map(tb -> ToothBrushingDto.builder().toothBrushingId(tb.getToothBrushingId()).created(tb.getCreated()).build())
                        .sorted(Comparator.comparing(ToothBrushingDto::getCreated))
                        .collect(Collectors.toList()))
                .build());

        // 3. 문진표
        Questionnaire latestQuestionnaire = !questionnaireList.isEmpty() ? questionnaireList.get(0) : null;
        sectionList.add(latestQuestionnaire == null || latestQuestionnaire.getCreated().before(recentWindowCalendar.getTime()) ? 1 : 2,
                OralCheckDto.Section.builder()
                        .sectionType(OralSectionType.QUESTIONNAIRE)
                        .date(latestQuestionnaire != null ? latestQuestionnaire.getCreated() : null)
                        .timeInterval(latestQuestionnaire != null ? (today.getTime() - latestQuestionnaire.getCreated().getTime()) / 1000 : null)
                        .build());

        for (int i = 0; i < sectionList.size(); i++) {
            sectionList.get(i).setSort(i + 1);
        }

        // 일별 상세 (Daily)
        List<OralCheckDto.Daily> dailyList = new ArrayList<>();
        calendar.setTime(timelineStartDate);
        calendar.add(Calendar.DATE, 1 - calendar.get(Calendar.DAY_OF_WEEK));

        while (true) {
            List<OralCheckDto.Detail> detailList = new ArrayList<>();
            String dateString = DateFormatUtil.dateToString(datePattern, calendar.getTime());

            // 구강 촬영 상세
            detailList.addAll(successfulOralCheckList.stream()
                    .filter(oc -> DateFormatUtil.dateToString(datePattern, oc.getCreated()).equals(dateString))
                    .map(oc -> OralCheckDto.Detail.builder()
                            .sectionType(OralSectionType.ORAL_CHECK)
                            .date(oc.getCreated())
                            .identifier(oc.getOralCheckId())
                            .oralCheckId(oc.getOralCheckId())
                            .oralCheckResultTotalType(oc.getOralCheckResultTotalType())
                            .build())
                    .toList());

            // 양치질 상세
            List<ToothBrushing> dailyToothBrushingList = toothBrushingList.stream()
                    .filter(tb -> DateFormatUtil.dateToString(datePattern, tb.getCreated()).equals(dateString)).toList();
            for (int i = 0; i < dailyToothBrushingList.size(); i++) {
                detailList.add(OralCheckDto.Detail.builder()
                        .sectionType(OralSectionType.TOOTH_BRUSHING)
                        .date(dailyToothBrushingList.get(i).getCreated())
                        .identifier(dailyToothBrushingList.get(i).getToothBrushingId())
                        .toothBrushingId(dailyToothBrushingList.get(i).getToothBrushingId())
                        .toothBrushingCount(dailyToothBrushingList.size() - i)
                        .build());
            }

            // 문진표 상세
            // 1. 해당 날짜의 문진표 필터링
            List<Questionnaire> dailyQuestionnaireList = questionnaireList.stream()
                    .filter(q -> DateFormatUtil.dateToString(datePattern, q.getCreated()).equals(dateString))
                    .toList();

            // 2. DTO 변환 및 리스트 추가
            detailList.addAll(dailyQuestionnaireList.stream()
                    .map(q -> OralCheckDto.Detail.builder()
                            .sectionType(OralSectionType.QUESTIONNAIRE)
                            .date(q.getCreated())
                            .identifier(q.getQuestionnaireId())
                            .questionnaireId(q.getQuestionnaireId())
                            .oralStatusList(oralStatusByQuestionnaireId.getOrDefault(q.getQuestionnaireId(), List.of()))
                            .build())
                    .toList());

            detailList.sort(Comparator.comparing(OralCheckDto.Detail::getDate).reversed());

            // 일별 상태 계산
            OralDateStatusType dailyStatusType = null;
            if (dateString.equals(todayString)) {
                dailyStatusType = OralDateStatusType.TODAY;
            } else if (!detailList.isEmpty()) {
                for (OralCheckDto.Detail dto : detailList) {
                    if (dto.getSectionType() == OralSectionType.ORAL_CHECK) {
                        if (dto.getOralCheckResultTotalType() == null) {
                            continue;
                        }
                        switch (dto.getOralCheckResultTotalType()) {
                            case HEALTHY -> dailyStatusType = OralDateStatusType.HEALTHY;
                            case GOOD -> dailyStatusType = OralDateStatusType.GOOD;
                            case ATTENTION -> dailyStatusType = OralDateStatusType.ATTENTION;
                            case DANGER -> dailyStatusType = OralDateStatusType.DANGER;
                        }
                        break;
                    } else if (dto.getSectionType() == OralSectionType.QUESTIONNAIRE) {
                        dailyStatusType = OralDateStatusType.QUESTIONNAIRE;
                        break;
                    }
                }
            }
            if (dailyStatusType == null && latestOralCheck != null && dateString.compareTo(oralCheckPeriodBefore) >= 0 && dateString.compareTo(oralCheckPeriodAfter) <= 0) {
                dailyStatusType = OralDateStatusType.ORAL_CHECK_PERIOD;
            }

            dailyList.add(OralCheckDto.Daily.builder()
                    .date(calendar.getTime())
                    .status(dailyStatusType)
                    .questionnaire(!dailyQuestionnaireList.isEmpty())
                    .detailList(detailList)
                    .build());

            if (dateString.compareTo(todayString) >= 0 && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) break;
            calendar.add(Calendar.DATE, 1);
        }

        return OralCheckDto.TimelineResponse.builder()
                .sectionList(sectionList)
                .dailyList(dailyList)
                .build();
    }

    private Date resolveTimelineStartDate(
            Date today,
            List<OralCheck> oralCheckList,
            List<ToothBrushing> toothBrushingList,
            List<Questionnaire> questionnaireList
    ) {
        Date earliest = today;

        if (!oralCheckList.isEmpty()) {
            earliest = oralCheckList.get(oralCheckList.size() - 1).getCreated();
        }
        if (!toothBrushingList.isEmpty() && toothBrushingList.get(toothBrushingList.size() - 1).getCreated().before(earliest)) {
            earliest = toothBrushingList.get(toothBrushingList.size() - 1).getCreated();
        }
        if (!questionnaireList.isEmpty() && questionnaireList.get(questionnaireList.size() - 1).getCreated().before(earliest)) {
            earliest = questionnaireList.get(questionnaireList.size() - 1).getCreated();
        }

        return earliest;
    }

    private Map<Long, List<OralStatusDto.OralStatusType>> buildOralStatusByQuestionnaireId(List<Questionnaire> questionnaireList) {
        if (questionnaireList.isEmpty()) {
            return Map.of();
        }

        List<Long> questionnaireIds = questionnaireList.stream()
                .map(Questionnaire::getQuestionnaireId)
                .toList();

        List<OralStatusAssignmentRepository.QuestionnaireOralStatusProjection> rows =
                oralStatusAssignmentRepository.findQuestionnaireOralStatuses(questionnaireIds);

        if (rows.isEmpty()) {
            return Map.of();
        }

        List<String> oralStatusTypes = rows.stream()
                .map(OralStatusAssignmentRepository.QuestionnaireOralStatusProjection::getOralStatusType)
                .distinct()
                .toList();

        Map<String, OralStatusDto.OralStatusType> oralStatusMap = oralStatusRepository.findAllByOralStatusTypeInOrderByOralStatusPriority(oralStatusTypes)
                .stream()
                .collect(Collectors.toMap(
                        OralStatus::getOralStatusType,
                        OralStatusDto.OralStatusType::from
                ));

        return rows.stream()
                .filter(row -> oralStatusMap.containsKey(row.getOralStatusType()))
                .collect(Collectors.groupingBy(
                        OralStatusAssignmentRepository.QuestionnaireOralStatusProjection::getQuestionnaireId,
                        Collectors.mapping(
                                row -> oralStatusMap.get(row.getOralStatusType()),
                                Collectors.toList()
                        )
                ));
    }

    /**
     * 대시보드 조회
     */
    @Cacheable(value = "dashboard", key = "@userService.getTokenUser(#p0).getUserId() + '_' + T(java.time.LocalDate).now()")
    public OralCheckDto.DashboardResponse dashboard(HttpServletRequest httpServletRequest) {
        User user = userService.getTokenUser(httpServletRequest);
        List<OralCheck> oralCheckList = oralCheckRepository.findAllByUser_UserIdOrderByCreatedDesc(user.getUserId());

        if (oralCheckList.isEmpty()) {
            return OralCheckDto.DashboardResponse.builder().build();
        }

        OralCheck latestOralCheck = oralCheckList.get(0);
        List<ToothBrushingDailyCountDto> toothBrushingDailyCountList = toothBrushingCustomRepository.getDailyCount(user.getUserId());
        int toothBrushingTotalCount = toothBrushingDailyCountList.stream().mapToInt(ToothBrushingDailyCountDto::getCount).sum();

        QuestionnaireDto.Summary latestQuestionnaire = questionnaireCustomRepository.getLatestQuestionnaireAndHigherStatus(user.getUserId());

        int healthy = 0, good = 0, attention = 0, danger = 0;
        List<OralCheckDto.DailyChange> changeList = new ArrayList<>();
        String beforeDate = "";

        for (int i = 0; i < oralCheckList.size(); i++) {
            OralCheck oc = oralCheckList.get(i);
            switch (oc.getOralCheckResultTotalType()) {
                case HEALTHY -> healthy++;
                case GOOD -> good++;
                case ATTENTION -> attention++;
                case DANGER -> danger++;
            }
            if (changeList.size() >= 10) continue;
            String dateString = DateFormatUtil.dateToString("yyyy-MM-dd", oc.getCreated());
            if (beforeDate.equals(dateString)) continue;

            changeList.add(0, new OralCheckDto.DailyChange(oralCheckList.size() - i, oc.getOralCheckResultTotalType()));
            beforeDate = dateString;
        }

        return OralCheckDto.DashboardResponse.builder()
                .latestOralCheckId(latestOralCheck.getOralCheckId())
                .oralCheckTimeInterval((new Date().getTime() - latestOralCheck.getCreated().getTime()) / 1000)
                .oralCheckTotalCount(oralCheckList.size())
                .oralCheckHealthyCount(healthy)
                .oralCheckGoodCount(good)
                .oralCheckAttentionCount(attention)
                .oralCheckDangerCount(danger)
                .toothBrushingTotalCount(toothBrushingTotalCount)
                .toothBrushingAverage(toothBrushingDailyCountList.isEmpty() ? 0 : Utils.getDeleteDecimalValue((float) toothBrushingTotalCount / toothBrushingDailyCountList.size(), 1))
                .oralStatus(latestQuestionnaire != null ? new OralStatusDto.OralStatusType(latestQuestionnaire.getOralStatusType(), latestQuestionnaire.getOralStatusTitle()) : null)
                .questionnaireCreated(latestQuestionnaire != null ? latestQuestionnaire.getCreated() : null)
                .oralCheckCreated(latestOralCheck.getCreated())
                .oralCheckResultTotalType(latestOralCheck.getOralCheckResultTotalType())
                .oralCheckUpRightScoreType(latestOralCheck.getOralCheckUpRightScoreType())
                .oralCheckUpLeftScoreType(latestOralCheck.getOralCheckUpLeftScoreType())
                .oralCheckDownLeftScoreType(latestOralCheck.getOralCheckDownLeftScoreType())
                .oralCheckDownRightScoreType(latestOralCheck.getOralCheckDownRightScoreType())
                .oralCheckDailyList(changeList)
                .build();
    }
    /**
     *  전체 평균 구강 상태
     */
    public OralCheckResultType getState(OralCheckResultTypeCount oralCheckResultTypeCount){
        if (oralCheckResultTypeCount.getCountHealthy() >= oralCheckResultTypeCount.getCountGood() &&
                oralCheckResultTypeCount.getCountHealthy() >= oralCheckResultTypeCount.getCountAttention() &&
                oralCheckResultTypeCount.getCountHealthy() >= oralCheckResultTypeCount.getCountDanger())
            return OralCheckResultType.HEALTHY;

        if (oralCheckResultTypeCount.getCountGood() >= oralCheckResultTypeCount.getCountAttention() &&
                oralCheckResultTypeCount.getCountGood() >= oralCheckResultTypeCount.getCountDanger())
            return OralCheckResultType.GOOD;

        if (oralCheckResultTypeCount.getCountAttention() >= oralCheckResultTypeCount.getCountDanger())
            return OralCheckResultType.ATTENTION;

        return OralCheckResultType.DANGER;
    }


}
