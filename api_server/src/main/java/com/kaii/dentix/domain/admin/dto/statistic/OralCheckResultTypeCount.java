package com.kaii.dentix.domain.admin.dto.statistic;

import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import lombok.*;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class OralCheckResultTypeCount {
    private OralCheckResultType oralCheckResultType; // ✅ 상태 (HEALTHY, GOOD 등)
    private Long count;


    private int countHealthy;

    private int countGood;

    private int countAttention;

    private int countDanger;

    // ✅ JPQL용 생성자 (추가)
    public OralCheckResultTypeCount(OralCheckResultType oralCheckResultType, Long count) {
        this.oralCheckResultType = oralCheckResultType;
        this.count = count;
    }

}
