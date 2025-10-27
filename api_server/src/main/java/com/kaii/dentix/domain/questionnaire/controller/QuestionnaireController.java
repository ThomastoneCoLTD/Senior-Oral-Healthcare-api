package com.kaii.dentix.domain.questionnaire.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.questionnaire.application.QuestionnaireService;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireIdDto;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireResultDto;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireTemplateJsonDto;
import com.kaii.dentix.domain.questionnaire.dto.request.QuestionnaireSubmitRequest;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/questionnaire")
public class QuestionnaireController {
    private final ObjectMapper objectMapper;
    private final QuestionnaireService questionnaireService;

//    @GetMapping(value = "/template", name = "문진표 양식 조회")
//    public DataResponse<QuestionnaireTemplateJsonDto> questionnaireTemplate() throws IOException {
//        return new DataResponse<>(questionnaireService.getQuestionnaireTemplate());
//    }

    @PostMapping(value = "/submit", name = "문진표 제출")
    public DataResponse<QuestionnaireIdDto> questionnaireSubmit(HttpServletRequest httpServletRequest, @Valid @RequestBody QuestionnaireSubmitRequest request) throws IOException {
        return new DataResponse<>(questionnaireService.questionnaireSubmit(httpServletRequest, request));
    }

    @GetMapping(value = "/result", name = "문진표 결과 조회")
    public DataResponse<QuestionnaireResultDto> questionnaireResult(
            HttpServletRequest httpServletRequest,
            @RequestParam @Min(1) long questionnaireId
    ) {
        return new DataResponse<>(
                questionnaireService.questionnaireResult(httpServletRequest, questionnaireId)
        );
    }
    /**
     * ✅ 문진표 결과 조회 (다국어 지원)
     *
     * 프론트에서 Axios 헤더로 "Accept-Language: ko/en/vi" 전송 시
     * 해당 언어로 oralStatusList title/description/subDescription이 반환됩니다.
     */
//    @GetMapping("/result")
//    public SuccessResponse getQuestionnaireResult(
//            HttpServletRequest request,
//            @RequestParam long questionnaireId
//    ) {
//        QuestionnaireResultDto result = questionnaireService.questionnaireResult(request, questionnaireId);
//        return new DataResponse<>(questionnaireService.questionnaireResult(request, result));
//    }

    @GetMapping("/template")
    public ResponseEntity<?> getTemplate(@RequestParam(defaultValue = "ko") String lang) throws IOException {
        InputStream jsonStream = new ClassPathResource("template/questionnaire.json").getInputStream();
        JsonNode root = objectMapper.readTree(jsonStream);

        // ✅ 언어 필터링해서 lang만 추출
        ArrayNode template = (ArrayNode) root.get("template");
        ArrayNode localized = objectMapper.createArrayNode();

        for (JsonNode q : template) {
            ObjectNode question = objectMapper.createObjectNode();
            question.put("sort", q.get("sort").asInt());
            question.put("key", q.get("key").asText());
            question.put("number", q.get("number").asText());
            question.put("title", q.path("title").path(lang).asText());
            question.put("description", q.path("description").path(lang).asText(null));
            question.put("minimum", q.path("minimum").asInt());
            question.put("maximum", q.path("maximum").isNull() ? null : q.path("maximum").asInt());

            ArrayNode contents = objectMapper.createArrayNode();
            for (JsonNode c : q.get("contents")) {
                ObjectNode opt = objectMapper.createObjectNode();
                opt.put("id", c.get("id").asInt());
                opt.put("text", c.path("text").path(lang).asText());
                contents.add(opt);
            }

            question.set("contents", contents);
            localized.add(question);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.set("template", localized);
        response.put("version", root.path("version").asText());
        return ResponseEntity.ok(Map.of("response", response));
    }
}
