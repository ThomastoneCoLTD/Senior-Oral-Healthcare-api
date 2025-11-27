package com.kaii.dentix.domain.subscription.domain;

import com.kaii.dentix.domain.type.PlanName;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subscription_plan")
@Where(clause = "deleted IS NULL")
public class SubscriptionPlan extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_plan_id")
    private Long id; // PK

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_name", nullable = false, length = 50)
    private PlanName planName;// 구독상품 플랜명 (예: Small, Growth, Enterprise)

    @Column(name = "plan_cycle", length = 20, nullable = false)
    private String planCycle; // monthly / yearly

    @Column(name = "plan_sort", nullable = false)
    private Integer planSort; // 정렬 순서

    @Column(nullable = false)
    private Long price; // 구독상품 기본 가격

    @Column(name = "max_success_responses", nullable = false)
    private Integer maxSuccessResponses; // 기본 제공 성공 응답 횟수

    @Column(name = "deleted")
    private LocalDateTime deleted;

    // =====================================================
    // ✅ [추가] SaaS 구독 정책 관련 필드
    // =====================================================

    /** 초과 1회당 요금 (원 단위) */
    @Column(name = "overuse_unit_price")
    private Integer overuseUnitPrice = 100; // 기본값 100원

    /** 커스텀 설문 템플릿 생성 가능 여부 */
    @Column(name = "custom_survey_enabled")
    private Boolean customSurveyEnabled = false;

    /** PDF 리포트 내보내기 기능 사용 가능 여부 */
    @Column(name = "report_export_enabled")
    private Boolean reportExportEnabled = false;

    /** 플랜 활성화 여부 (비활성화 시 신규 구독 불가) */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // =====================================================
    // ✅ 기능 헬퍼 메서드
    // =====================================================

    /** Soft delete 처리 */
    public void deletePlan() {
        this.deleted = LocalDateTime.now();
    }

    /** 플랜 사용 가능 여부 */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active) && this.deleted == null;
    }

    /** 기능 사용 가능 여부 검사 */
    public boolean canUseFeature(FeatureType feature) {
        return switch (feature) {
            case CUSTOM_SURVEY -> Boolean.TRUE.equals(customSurveyEnabled);
            case REPORT_EXPORT -> Boolean.TRUE.equals(reportExportEnabled);
        };
    }

    /** 기능 Enum */
    public enum FeatureType {
        CUSTOM_SURVEY,
        REPORT_EXPORT
    }

    public boolean isYearly() {
        return "yearly".equalsIgnoreCase(this.planCycle);
    }
    public boolean isMonthly() {
        return "monthly".equalsIgnoreCase(this.planCycle);
    }
}
