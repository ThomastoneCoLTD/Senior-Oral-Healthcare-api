package com.kaii.dentix.domain.questionnaire.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.questionnaire.application.QuestionnaireService;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireDto;
import com.kaii.dentix.global.common.response.DataResponse;
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

    @PostMapping(value = "/submit", name = "문진표 제출")
    public DataResponse<QuestionnaireDto.IdResponse> questionnaireSubmit(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody QuestionnaireDto.SubmitRequest request
    ) throws IOException {
        return new DataResponse<>(questionnaireService.questionnaireSubmit(httpServletRequest, request));
    }

    @GetMapping(value = "/result", name = "문진표 결과 조회")
    public DataResponse<QuestionnaireDto.ResultResponse> questionnaireResult(
            HttpServletRequest httpServletRequest,
            @RequestParam @Min(1) long questionnaireId
    ) {
        return new DataResponse<>(
                questionnaireService.questionnaireResult(httpServletRequest, questionnaireId)
        );
    }

    @GetMapping("/template")
    public ResponseEntity<?> getTemplate(@RequestParam(defaultValue = "ko") String lang) throws IOException {

        // 프론트 표준 언어코드로 통일
        Map<String, String> langMap = Map.of(
                "zh-CN", "zh-CN",
                "zh-TW", "zh-TW",
                "ko", "ko",
                "en", "en",
                "vi", "vi"
        );

        // fallback
        lang = langMap.getOrDefault(lang, "ko");

        InputStream jsonStream = new ClassPathResource("template/questionnaire.json").getInputStream();
        JsonNode root = objectMapper.readTree(jsonStream);

        ArrayNode template = (ArrayNode) root.get("template");
        ArrayNode localized = objectMapper.createArrayNode();

        for (JsonNode q : template) {
            ObjectNode question = objectMapper.createObjectNode();

            question.put("sort", q.path("sort").asInt());
            question.put("key", q.path("key").asText());
            question.put("number", q.path("number").asText());

            // title/description
            question.put("title", q.path("title").path(lang).asText());

            JsonNode descNode = q.path("description").path(lang);
            question.put("description", descNode.isNull() ? null : descNode.asText());

            question.put("minimum", q.path("minimum").asInt());

            JsonNode maxNode = q.path("maximum");
            question.put("maximum", maxNode.isNull() ? null : maxNode.asInt());

            // options
            ArrayNode contents = objectMapper.createArrayNode();
            for (JsonNode c : q.get("contents")) {
                ObjectNode opt = objectMapper.createObjectNode();
                opt.put("id", c.path("id").asInt());
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
