package com.kaii.dentix.domain.questionnaire.dto;

import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
@AllArgsConstructor
public class OralStatusTypeInfoDto {

    private String type;
    private String title;
    private String description;
    private String subDescription;

    public static OralStatusTypeInfoDto from(OralStatus entity, String lang) {
        return switch (lang.toLowerCase()) {
            case "en" -> new OralStatusTypeInfoDto(
                    entity.getOralStatusType(),
                    entity.getOralStatusTitleEn() != null ? entity.getOralStatusTitleEn() : entity.getOralStatusTitle(),
                    entity.getOralStatusDescriptionEn() != null ? entity.getOralStatusDescriptionEn() : entity.getOralStatusDescription(),
                    entity.getOralStatusSubDescriptionEn() != null ? entity.getOralStatusSubDescriptionEn() : entity.getOralStatusSubDescription()
            );
            case "vi" -> new OralStatusTypeInfoDto(
                    entity.getOralStatusType(),
                    entity.getOralStatusTitleVi() != null ? entity.getOralStatusTitleVi() : entity.getOralStatusTitle(),
                    entity.getOralStatusDescriptionVi() != null ? entity.getOralStatusDescriptionVi() : entity.getOralStatusDescription(),
                    entity.getOralStatusSubDescriptionVi() != null ? entity.getOralStatusSubDescriptionVi() : entity.getOralStatusSubDescription()
            );
            default -> new OralStatusTypeInfoDto(
                    entity.getOralStatusType(),
                    entity.getOralStatusTitle(),
                    entity.getOralStatusDescription(),
                    entity.getOralStatusSubDescription()
            );
        };
    }
}
