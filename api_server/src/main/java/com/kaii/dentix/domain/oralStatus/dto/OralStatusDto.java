package com.kaii.dentix.domain.oralStatus.dto;

import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class OralStatusDto {
    // =================================================================
    // 2. 구강 상태 타입 정보 (OralStatusType)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class OralStatusType {
        private String type;
        private String title;

        // Entity -> DTO 변환 메서드
        public static OralStatusType from(OralStatus entity) {
            return OralStatusType.builder()
                    .type(entity.getOralStatusType())
                    .title(entity.getOralStatusTitle())
                    .build();
        }
    }

    // =================================================================
    // 2. 구강 상태 상세 정보 (Info: 다국어 제목/설명 포함)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Info {
        private String type;
        private String title;
        private String description;
        private String subDescription;

        public static Info from(OralStatus entity, String lang) {
            return switch (lang.toLowerCase()) {
                case "en" -> Info.builder()
                        .type(entity.getOralStatusType())
                        .title(entity.getOralStatusTitleEn() != null ? entity.getOralStatusTitleEn() : entity.getOralStatusTitle())
                        .description(entity.getOralStatusDescriptionEn() != null ? entity.getOralStatusDescriptionEn() : entity.getOralStatusDescription())
                        .subDescription(entity.getOralStatusSubDescriptionEn() != null ? entity.getOralStatusSubDescriptionEn() : entity.getOralStatusSubDescription())
                        .build();
                case "vi" -> Info.builder()
                        .type(entity.getOralStatusType())
                        .title(entity.getOralStatusTitleVi() != null ? entity.getOralStatusTitleVi() : entity.getOralStatusTitle())
                        .description(entity.getOralStatusDescriptionVi() != null ? entity.getOralStatusDescriptionVi() : entity.getOralStatusDescription())
                        .subDescription(entity.getOralStatusSubDescriptionVi() != null ? entity.getOralStatusSubDescriptionVi() : entity.getOralStatusSubDescription())
                        .build();
                default -> Info.builder() // "ko" included
                        .type(entity.getOralStatusType())
                        .title(entity.getOralStatusTitle())
                        .description(entity.getOralStatusDescription())
                        .subDescription(entity.getOralStatusSubDescription())
                        .build();
            };
        }
    }
}