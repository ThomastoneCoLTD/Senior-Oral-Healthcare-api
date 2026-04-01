package com.kaii.dentix.domain.contents.dto;

import com.kaii.dentix.domain.type.ContentsType;
import lombok.*;

import java.util.List;


public class ContentsDto {

    // =================================================================
    // 1. 콘텐츠 카테고리 정보 (Category)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Category {
        private int id;
        private String name;
        private String color;
        @Setter
        private int sort;
    }

    // =================================================================
    // 2. 콘텐츠 요약 정보 (Summary) - 목록 조회용
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String title;
        private Integer sort;
        private ContentsType type;
        private String typeColor;
        private String thumbnail;
        private String videoURL; // Entity field: contentsPath
        @Setter
        private List<Integer> categoryIds;
    }

    // =================================================================
    // 3. 콘텐츠 전체 목록 응답 (ListResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ListResponse {
        private List<Category> categories;
        private List<Summary> contents;
    }

    // =================================================================
    // 4. 카드뉴스 단건 정보 (Card)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Card {
        private int number;
        private String path;
    }

    // =================================================================
    // 5. 카드뉴스 목록 응답 (CardListResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class CardListResponse {
        private String title;
        private List<Card> cardList;
    }
}
