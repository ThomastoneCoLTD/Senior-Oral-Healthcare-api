package com.kaii.dentix.domain.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
//@AllArgsConstructor
public enum OralSectionType {

    ORAL_CHECK(1, "구강 관련 활동", "구강 촬영"),
    TOOTH_BRUSHING(2, "구강 관련 활동", "양치질"),
    QUESTIONNAIRE(3, "구강 관련 활동", "문진표");

    private final int id;
    private final String description;
    private final String value;

    OralSectionType(int id, String description, String value) {
        this.id = id;
        this.description = description;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }
}
