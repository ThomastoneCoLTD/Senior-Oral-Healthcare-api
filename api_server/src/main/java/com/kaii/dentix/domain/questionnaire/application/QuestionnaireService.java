package com.kaii.dentix.domain.questionnaire.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.contents.application.ContentsService;
import com.kaii.dentix.domain.contents.dao.ContentsCustomRepository;
import com.kaii.dentix.domain.contents.dto.ContentsCategoryDto;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckAnalysisDivisionDto;
import com.kaii.dentix.domain.oralCheck.dto.resoponse.OralCheckAnalysisResponse;
import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.oralStatus.jpa.OralStatusRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.questionnaire.dto.*;
import com.kaii.dentix.domain.questionnaire.dto.request.QuestionnaireSubmitRequest;
import com.kaii.dentix.domain.questionnaire.dto.response.QuestionnaireAnalysisResponse;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.PlanName;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.FormValidationException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.util.AiModelService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionnaireService {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final ContentsService contentsService;
    private final QuestionnaireRepository questionnaireRepository;
    private final ContentsCustomRepository contentsCustomRepository;
    private final OralStatusRepository oralStatusRepository;
    private final AiModelService aiModelService;

    @Value("${spring.profiles.active}")
    private String active;

    /**
     * 문진표 양식 조회
     */
    @Transactional(readOnly = true)
    public QuestionnaireTemplateJsonDto getQuestionnaireTemplate(HttpServletRequest request) throws IOException {
        //1. 사용자 및 기관 조회
        User user = userService.getTokenUser(request);
        Organization organization = user.getOrganization();

        if (organization == null || organization.getOrganizationSubscription() == null) {
            throw new BadRequestApiException("기관 정보 또는 구독 정보가 없습니다.");
        }

        OrganizationSubscription subscription = organization.getOrganizationSubscription();
        SubscriptionPlan plan = subscription.getSubscriptionPlan();

        if (plan == null || plan.getPlanName() == null) {
            throw new BadRequestApiException("기관의 구독 플랜 정보가 없습니다.");
        }

        //2. Small 플랜 차단
        if (plan.getPlanName() == PlanName.SMALL) {
            throw new BadRequestApiException("현재 구독 상품(Small)에서는 문진표 서비스를 이용할 수 없습니다.");
        }

        //3. 템플릿 파일 로드
        ClassPathResource resource = new ClassPathResource("template/questionnaire.json");
        if (!resource.exists()) {
            throw new BadRequestApiException("문진표 템플릿 파일이 존재하지 않습니다.");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);

            //TypeReference 사용 (LinkedHashMap 캐스팅 문제 방지)
            return objectMapper.readValue(new String(bytes), new TypeReference<>() {});
        }
    }


    /**
     * 문진표 제출
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionnaireIdDto questionnaireSubmit(HttpServletRequest httpServletRequest, QuestionnaireSubmitRequest request) throws IOException {
        User user = userService.getTokenUser(httpServletRequest);

        QuestionnaireTemplateJsonDto questionnaireTemplate = this.getQuestionnaireTemplate(httpServletRequest);
        this.questionnaireValidate(questionnaireTemplate.getTemplate(), request.getForm());

        // AI 서버로 문진표 전달 후, AI 분석 결과 받아옴
        Map<String, Map<String, Object>> questionnaireForm = new HashMap<>();
        Map<String, Object> form = new LinkedHashMap<>();

        questionnaireTemplate.getTemplate().forEach(template -> {
            Integer[] values = request.getForm().stream().filter(o -> o.getKey().equals(template.getKey()))
                    .findAny().orElseThrow(() -> new FormValidationException(String.format("%s번 문항을 입력해 주세요.", template.getNumber())))
                    .getValue();

            Object value;
            if (template.getMaximum() != null && template.getMaximum() == 1) { // 단일 선택
                value = values.length == 1 ? values[0] : null;
            } else {
                value = values;
            }
            form.put(template.getKey(), value);
        });

        questionnaireForm.put("form", form);

        QuestionnaireAnalysisResponse analysisData;
//        log.info(analysisData.toString());
        try {
            analysisData =
                    aiModelService.getQuestionnaireAiModel(questionnaireForm).join();
                    log.info("data:{}",analysisData.toString());

        } catch (Exception e) {
            if (active.equals("dev")) { // 개발서버의 경우 테스트 데이터 연동
                log.warn("AI 모델 연동 실패로 테스트 데이터 연동됨 (문진표 제출)");
                Random random = new Random();
                int typeCount = random.nextInt(2) + 1;
                String[] chars = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};
                List<String> typeList = new ArrayList<>();
                for (int i = 0; i < typeCount; i++) {
                    int randomIndex = random.nextInt(chars.length);
                    if (typeList.stream().anyMatch(type -> type.equals(chars[randomIndex]))) {
                        i--; continue;
                    }
                    typeList.add(chars[randomIndex]);
                }
                analysisData = new QuestionnaireAnalysisResponse(typeList);
                log.info(analysisData.toString());
            } else {
                throw new BadRequestApiException("AI 모델 연동에 실패했어요.\n관리자에게 문의해 주세요.");
            }
        }

        List<OralStatus> oralStatusList = oralStatusRepository.findAllByOralStatusTypeInOrderByOralStatusPriority(analysisData.getContentsType());
        List<String> oralStatusTypeList = oralStatusList.subList(0, Math.min(2, oralStatusList.size())) // 최대 2개
                .stream().map(OralStatus::getOralStatusType).toList();

        Questionnaire questionnaire = questionnaireRepository.save(
                new Questionnaire(
                        user.getUserId(),
                        questionnaireTemplate.getVersion(),
                        objectMapper.writeValueAsString(request),
                        oralStatusTypeList
                )
        );

        return new QuestionnaireIdDto(questionnaire.getQuestionnaireId());
    }

    /**
     * 문진표 결과 조회
     */
//    @Transactional(readOnly = true)
//    public QuestionnaireResultDto questionnaireResult(HttpServletRequest httpServletRequest, long questionnaireId) {
//        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId).orElseThrow(() -> new NotFoundDataException("문진표가 존재하지 않습니다."));
//
//        List<OralStatusTypeInfoDto> oralStatusList = questionnaire.getUserOralStatusList().stream()
//                .map(userOralStatus -> {
//                    OralStatus oralStatus = userOralStatus.getOralStatus();
//                    return OralStatusTypeInfoDto.builder()
//                            .type(oralStatus.getOralStatusType())
//                            .title(oralStatus.getOralStatusTitle())
//                            .description(oralStatus.getOralStatusDescription())
//                            .subDescription(oralStatus.getOralStatusSubDescription())
//                            .build();
//                }).toList();
//
//        List<ContentsCategoryDto> categories = contentsService.getCategoryList(null);
//        List<ContentsDto> contents = contentsCustomRepository.getCustomizedContents(questionnaireId);
//        contents = contents.subList(0, Math.min(contents.size(), 2)); // 최대 2개
//
//        return new QuestionnaireResultDto(questionnaire.getCreated(), oralStatusList, categories, contents);
//    }
    @Transactional(readOnly = true)
    public QuestionnaireResultDto questionnaireResult(HttpServletRequest request, long questionnaireId) {
        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new NotFoundDataException("문진표가 존재하지 않습니다."));

        //언어 감지 (헤더에서)
        String lang = Optional.ofNullable(request.getHeader("Accept-Language"))
                .map(l -> l.split(",")[0]) // "en-US,en;q=0.9" → "en"
                .map(String::toLowerCase)
                .orElse("ko");

        //언어별 OralStatus 변환
        List<OralStatusTypeInfoDto> oralStatusList = questionnaire.getUserOralStatusList().stream()
                .map(userOralStatus -> {
                    OralStatus oralStatus = userOralStatus.getOralStatus();

                    String title;
                    String description;
                    String subDescription;

                    switch (lang) {
                        case "en" -> {
                            title = Optional.ofNullable(oralStatus.getOralStatusTitleEn()).orElse(oralStatus.getOralStatusTitle());
                            description = Optional.ofNullable(oralStatus.getOralStatusDescriptionEn()).orElse(oralStatus.getOralStatusDescription());
                            subDescription = Optional.ofNullable(oralStatus.getOralStatusSubDescriptionEn()).orElse(oralStatus.getOralStatusSubDescription());
                        }
                        case "vi" -> {
                            title = Optional.ofNullable(oralStatus.getOralStatusTitleVi()).orElse(oralStatus.getOralStatusTitle());
                            description = Optional.ofNullable(oralStatus.getOralStatusDescriptionVi()).orElse(oralStatus.getOralStatusDescription());
                            subDescription = Optional.ofNullable(oralStatus.getOralStatusSubDescriptionVi()).orElse(oralStatus.getOralStatusSubDescription());
                        }
                        default -> {
                            title = oralStatus.getOralStatusTitle();
                            description = oralStatus.getOralStatusDescription();
                            subDescription = oralStatus.getOralStatusSubDescription();
                        }
                    }

                    return OralStatusTypeInfoDto.builder()
                            .type(oralStatus.getOralStatusType())
                            .title(title)
                            .description(description)
                            .subDescription(subDescription)
                            .build();
                })
                .toList();

        //카테고리 및 콘텐츠 (언어 필터는 필요 시 확장)
//        List<ContentsCategoryDto> categories = contentsService.getCategoryList(null);
        List<ContentsDto> contents = contentsCustomRepository.getCustomizedContents(questionnaireId);
        contents = contents.subList(0, Math.min(contents.size(), 2)); // 최대 2개

        return new QuestionnaireResultDto(
                questionnaire.getCreated(),
                oralStatusList,
//                categories,
                contents
        );
    }
    /**
     * 문진표 양식 기준으로 validation 진행
     */
    private void questionnaireValidate(List<QuestionnaireTemplateDto> questionnaireTemplate, List<QuestionnaireKeyValueDto> form) {
        questionnaireTemplate.forEach(template -> {
            // 값 존재 확인
            Integer[] values = form.stream().filter(o -> o.getKey().equals(template.getKey()))
                    .findAny().orElseThrow(() -> new FormValidationException(String.format("%s번 문항을 입력해 주세요.", template.getNumber())))
                    .getValue();

            // 개수 확인
            if (template.getMinimum() != null && values.length < template.getMinimum()) {
                if (template.getMinimum() > 1) {
                    throw new FormValidationException(String.format("%s번 문항을 %d개 이상 입력해 주세요.", template.getNumber(), template.getMinimum()));
                } else {
                    throw new FormValidationException(String.format("%s번 문항을 입력해 주세요.", template.getNumber()));
                }
            }
            if (template.getMaximum() != null && values.length > template.getMaximum()) {
                throw new FormValidationException(String.format("%s번 문항은 %d개까지만 입력할 수 있습니다.", template.getNumber(), template.getMaximum()));
            }

            int[] normalValues = template.getContents().stream().mapToInt(QuestionnaireTemplateContentDto::getId).toArray();
            List<Integer> alreadyValues = new ArrayList<>();
            Arrays.stream(values).forEach(value -> {
                // 유효하지 않은 값 존재 확인
                if (Arrays.stream(normalValues).noneMatch(nv -> nv == value)) {
                    throw new FormValidationException(String.format("%s번 문항에 %d 값은 유효하지 않습니다.", template.getNumber(), value));
                }
                // 중복 값 존재 확인
                if (alreadyValues.stream().anyMatch(nv -> nv.equals(value))) {
                    throw new FormValidationException(String.format("%s번 문항에 %d 값이 중복으로 존재합니다.", template.getNumber(), value));
                }
                alreadyValues.add(value);
            });
        });
    }
}
