package com.kaii.dentix.domain.contents.dto;

import com.kaii.dentix.domain.type.ContentsType;
import lombok.*;

import java.util.Date;
import java.util.List;


public class ContentsDto {

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class MenuTab {
        private String id;
        private String name;
    }

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
        @Setter
        private boolean personalized;
        @Setter
        private String personalizationSource;
        @Setter
        private Integer rank;

        public Summary(Long id, String title, Integer sort, ContentsType type,
                       String typeColor, String thumbnail, String videoURL,
                       List<Integer> categoryIds) {
            this.id = id;
            this.title = title;
            this.sort = sort;
            this.type = type;
            this.typeColor = typeColor;
            this.thumbnail = thumbnail;
            this.videoURL = videoURL;
            this.categoryIds = categoryIds;
        }
    }

    // =================================================================
    // 3. 콘텐츠 전체 목록 응답 (ListResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ListResponse {
        private List<MenuTab> menuTabs;
        private List<Category> categories;
        private List<Summary> contents;
        private List<PersonalizedSection> sections;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PersonalizedSection {
        private String source;
        private boolean enabled;
        private boolean completed;
        private Long resultId;
        private String resultKey;
        private Date created;
        private String actionPath;
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
