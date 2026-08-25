package com.kaii.dentix.common;

import org.springframework.restdocs.snippet.Attributes;

import static org.springframework.restdocs.snippet.Attributes.key;

public class DocumentOptionalGenerator {

    public static Attributes.Attribute setFormat(String value) {
        return key("format").value(value);
    }

    public static Attributes.Attribute userNumberFormat() {
        return setFormat("01012345678 (10자리 ~ 11자리)");
    }

    public static Attributes.Attribute yesNoFormat() {
        return setFormat("Y: Yes, N: No");
    }

    public static Attributes.Attribute genderFormat() {
        return setFormat("M: 남성, W: 여성");
    }

    public static Attributes.Attribute dateFormat() {
        return setFormat("yyyy-MM-dd");
    }

    public static Attributes.Attribute dateTimeFormat() {
        return setFormat("yyyy-MM-dd HH:mm:ss");
    }

    public static Attributes.Attribute timeIntervalFormat() {
        return setFormat("초 단위 interval");
    }

    public static Attributes.Attribute oralCheckResultTypeFormat() {
        return setFormat("HEALTHY : 건강, GOOD : 양호, ATTENTION : 주의, DANGER : 위험");
    }

    public static Attributes.Attribute oralCheckDivisionCommentFormat() {
        return setFormat("HEALTHY : 모두 잘 닦인 경우, UR : 상악 우측, UL : 상악 좌측, DR : 하악 우측, DL : 하악 좌측");
    }

    public static Attributes.Attribute oralSectionTypeFormat() {
        return setFormat("ORAL_CHECK : 구강 촬영, QUESTIONNAIRE : 문진표");
    }

    public static Attributes.Attribute oralDateStatusTypeFormat() {
        return setFormat("HEALTHY : 건강, GOOD : 양호, ATTENTION : 주의, DANGER : 위험, QUESTIONNAIRE : 문진표, ORAL_CHECK_PERIOD : 권장 촬영 기간");
    }

    public static Attributes.Attribute contentsTypeFormat() {
        return setFormat("CARD : 카드뉴스, VIDEO : 동영상, ANIMATION : 애니메이션");
    }

    public static Attributes.Attribute isRequiredFormat() {
        return setFormat("Y: 필수, N: 선택");
    }

    public static Attributes.Attribute datePeriodTypeFormat() {
        return setFormat("TODAY : 오늘, WEEK1 : 1주일, MONTH1 : 1개월, MONTH3 : 3개월, YEAR1 : 1년, ALL : 전체");
    }

}
