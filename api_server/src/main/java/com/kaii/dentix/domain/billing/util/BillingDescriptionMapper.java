package com.kaii.dentix.domain.billing.util;

import java.util.Map;

public class BillingDescriptionMapper {

    private static final Map<String, String> DESCRIPTION_KO_TO_EN = Map.of(
            "구독상품 변경 결제", "Subscription Plan Changed",
            "기관 등록 시 자동 청구 생성", "Initial Billing on Organization Registration",
            "AI 구강 분석 초과 1건 요금", "Overuse Fee (1 AI Analysis)"
    );

    public static String toEnglish(String desc) {
        if (desc == null) return "";
        return DESCRIPTION_KO_TO_EN.getOrDefault(desc, desc); // 매핑 없으면 원래 값 유지
    }
}