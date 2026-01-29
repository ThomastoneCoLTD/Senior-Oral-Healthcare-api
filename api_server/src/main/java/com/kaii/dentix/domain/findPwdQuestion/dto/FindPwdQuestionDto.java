package com.kaii.dentix.domain.findPwdQuestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
public class FindPwdQuestionDto {

    // =================================================================
    // 1. 비밀번호 찾기 질문 목록 응답 (Response)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private List<Question> questions;
    }

    // =================================================================
    // 2. 단일 질문 정보 (Question)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Question {
        private Long id;
        private Long sort;
        private String title;
    }
}