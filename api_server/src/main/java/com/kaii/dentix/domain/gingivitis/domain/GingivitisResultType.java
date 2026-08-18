package com.kaii.dentix.domain.gingivitis.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GingivitisResultType {
    S("정상 범위", "현재의 구강 관리 습관을 유지해 주세요."),
    G("주의", "약간의 치은염이 의심됩니다. 치실·치간칫솔과 꼼꼼한 잇솔질을 권장합니다."),
    A("약간위험", "치은염이 의심됩니다. 스케일링 시기와 치과 상담을 검토해 주세요."),
    D("위험", "치은염이 강하게 의심되어 치과 검진을 권장합니다.");

    private final String label;
    private final String comment;

    public static GingivitisResultType fromPercent(double percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100");
        }
        if (percent < 10) return S;
        if (percent < 40) return G;
        if (percent < 70) return A;
        return D;
    }
}
