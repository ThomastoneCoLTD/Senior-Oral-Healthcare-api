package com.kaii.dentix.global.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.oralCheck.dto.resoponse.OralCheckAnalysisResponse;
import com.kaii.dentix.domain.oralCheck.dto.resoponse.GingivitisAnalysisResponse;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireAnalysisResponse;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AiModelService {

    @Value("${aiModel.apiUrl.oralCheck}")
    private String oralCheckAiModelApiUrl;

    @Value("${aiModel.apiUrl.gingivitis}")
    private String gingivitisAiModelApiUrl;

    @Value("${aiModel.apiUrl.questionnaire}")
    private String questionnaireAiModelApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiModelService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     *  구강검진 사진 촬영 AI Model
     */
    @SneakyThrows
    @Async
    public CompletableFuture<OralCheckAnalysisResponse> getPyDentalAiModel(MultipartFile picture) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();

        ByteArrayResource fileResource = new ByteArrayResource(picture.getBytes()) {
            @Override
            public String getFilename() {
                return picture.getOriginalFilename();
            }
        };

        params.add("picture", fileResource);

        HttpEntity<MultiValueMap<String, Object>> entity =
                new HttpEntity<>(params, headers);

        OralCheckAnalysisResponse response =
                restTemplate.postForObject(
                        oralCheckAiModelApiUrl,
                        entity,
                        OralCheckAnalysisResponse.class
                );

        return CompletableFuture.completedFuture(response);
    }

    /**
     *  치은염 검출 AI Model
     */
    @SneakyThrows
    @Async
    public CompletableFuture<GingivitisAnalysisResponse> getGingivitisAiModel(MultipartFile picture) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();

        ByteArrayResource fileResource = new ByteArrayResource(picture.getBytes()) {
            @Override
            public String getFilename() {
                return picture.getOriginalFilename();
            }
        };

        params.add("picture", fileResource);

        HttpEntity<MultiValueMap<String, Object>> entity =
                new HttpEntity<>(params, headers);

        GingivitisAnalysisResponse response =
                restTemplate.postForObject(
                        gingivitisAiModelApiUrl,
                        entity,
                        GingivitisAnalysisResponse.class
                );

        return CompletableFuture.completedFuture(response);
    }

    /**
     *  문진표 AI Model
     */
    @SneakyThrows
    @Async
    public CompletableFuture<QuestionnaireAnalysisResponse>
    getQuestionnaireAiModel(Map<String, Map<String, Object>> survey) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("survey", objectMapper.writeValueAsString(survey));

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(params, headers);

        QuestionnaireAnalysisResponse response =
                restTemplate.postForObject(
                        questionnaireAiModelApiUrl,
                        entity,
                        QuestionnaireAnalysisResponse.class
                );

        return CompletableFuture.completedFuture(response);
    }
}
