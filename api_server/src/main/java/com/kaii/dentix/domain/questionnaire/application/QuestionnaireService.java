package com.kaii.dentix.domain.questionnaire.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.contents.dao.ContentsCustomRepository;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.oralStatus.dto.OralStatusDto;
import com.kaii.dentix.domain.oralStatus.jpa.OralStatusRepository;
import com.kaii.dentix.domain.oralStatusAssignment.dao.OralStatusAssignmentRepository;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireDto;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.FormValidationException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.util.AiModelService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final AiModelService aiModelService;
    private final OralStatusRepository oralStatusRepository;
    private final OralStatusAssignmentRepository oralStatusAssignmentRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final ContentsCustomRepository contentsCustomRepository;
    private final QuestionnaireFallbackAnalyzer fallbackAnalyzer;

    /**
     * 문진표 양식 조회
     */
    @Transactional(readOnly = true)
    public QuestionnaireDto.TemplateJson getQuestionnaireTemplate(HttpServletRequest request) throws IOException {
        ClassPathResource resource = new ClassPathResource("template/questionnaire.json");
        if (!resource.exists()) {
            throw new BadRequestApiException("문진표 템플릿 파일이 존재하지 않습니다.");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            // 통합된 TemplateJson DTO로 매핑
            return objectMapper.readValue(new String(bytes), new TypeReference<QuestionnaireDto.TemplateJson>() {});
        }
    }


    /**
     * 문진표 제출
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionnaireDto.IdResponse questionnaireSubmit(HttpServletRequest httpServletRequest, QuestionnaireDto.SubmitRequest request) throws IOException {
        User user = userService.getTokenUser(httpServletRequest);

        QuestionnaireDto.TemplateJson questionnaireTemplate = this.getQuestionnaireTemplate(httpServletRequest);
        this.questionnaireValidate(questionnaireTemplate.getTemplate(), request.getForm());

        // AI 서버 분석 요청 데이터 구성
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

        QuestionnaireDto.AnalysisResponse analysisData;
        try {
            var aiResult = aiModelService.getQuestionnaireAiModel(questionnaireForm).join();
            if (aiResult == null || aiResult.getStatusCode() != 200
                    || aiResult.getContentsType() == null || aiResult.getContentsType().isEmpty()) {
                throw new IllegalStateException("문진표 AI 분석 결과가 올바르지 않습니다.");
            }
            analysisData = QuestionnaireDto.AnalysisResponse.builder()
                    .contentsType(aiResult.getContentsType())
                    .build();

        } catch (Exception e) {
            log.warn("Questionnaire AI analysis failed; using rules fallback: {}", e.getMessage());
            analysisData = fallbackAnalyzer.analyze(questionnaireForm);
        }

        List<OralStatus> oralStatusList = oralStatusRepository.findAllByOralStatusTypeInOrderByOralStatusPriority(analysisData.getContentsType());
        List<String> oralStatusTypeList = oralStatusList.subList(0, Math.min(2, oralStatusList.size()))
                .stream().map(OralStatus::getOralStatusType).toList();

        Questionnaire questionnaire = questionnaireRepository.save(
                new Questionnaire(
                        user.getUserId(),
                        questionnaireTemplate.getVersion(),
                        objectMapper.writeValueAsString(request),
                        oralStatusTypeList
                )
        );

        return QuestionnaireDto.IdResponse.builder()
                .questionnaireId(questionnaire.getQuestionnaireId())
                .nextStep("QUESTIONNAIRE_RESULT_MODAL")
                .nextPath("/questionnaire/result?questionnaireId=" + questionnaire.getQuestionnaireId())
                .build();
    }

    /**
     * 문진표 결과 조회
     */
    @Transactional(readOnly = true)
    public QuestionnaireDto.ResultResponse questionnaireResult(HttpServletRequest request, long questionnaireId) {
        Questionnaire questionnaire = questionnaireRepository.findById(questionnaireId)
                .orElseThrow(() -> new NotFoundDataException("문진표가 존재하지 않습니다."));

        // 언어 감지
        String lang = Optional.ofNullable(request.getHeader("Accept-Language"))
                .map(l -> l.split(",")[0].toLowerCase())
                .orElse("ko");

        // 구강 상태 매핑
        List<String> oralStatusTypes = oralStatusAssignmentRepository.findOralStatusTypesByQuestionnaireId(questionnaireId);
        List<OralStatusDto.Info> oralStatusList = oralStatusRepository.findAllByOralStatusTypeInOrderByOralStatusPriority(oralStatusTypes).stream()
                .map(oralStatus -> OralStatusDto.Info.from(oralStatus, lang))
                .toList();

        // 맞춤 콘텐츠 조회 (ContentsDto.Summary 사용)
        List<ContentsDto.Summary> contents = contentsCustomRepository.getCustomizedContents(questionnaireId);
        if (contents.size() > 2) {
            contents = contents.subList(0, 2);
        }

        return QuestionnaireDto.ResultResponse.builder()
                .created(questionnaire.getCreated())
                .oralStatusList(oralStatusList)
                .contents(contents)
                .build();
    }

    /**
     * 문진표 검증 로직 (통합 DTO 타입 적용)
     */
    private void questionnaireValidate(List<QuestionnaireDto.Template> questionnaireTemplate, List<QuestionnaireDto.Answer> form) {
        questionnaireTemplate.forEach(template -> {
            // Answer의 Integer[] 타입을 그대로 사용
            Integer[] values = form.stream().filter(o -> o.getKey().equals(template.getKey()))
                    .findAny().orElseThrow(() -> new FormValidationException(String.format("%s번 문항을 입력해 주세요.", template.getNumber())))
                    .getValue();

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

            int[] normalValues = template.getContents().stream().mapToInt(QuestionnaireDto.TemplateContent::getId).toArray();
            List<Integer> alreadyValues = new ArrayList<>();
            Arrays.stream(values).forEach(value -> {
                if (Arrays.stream(normalValues).noneMatch(nv -> nv == value)) {
                    throw new FormValidationException(String.format("%s번 문항에 %d 값은 유효하지 않습니다.", template.getNumber(), value));
                }
                if (alreadyValues.contains(value)) {
                    throw new FormValidationException(String.format("%s번 문항에 %d 값이 중복으로 존재합니다.", template.getNumber(), value));
                }
                alreadyValues.add(value);
            });
        });
    }
}
