package com.kaii.dentix.domain.billing.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.domain.BillingStatusHistory;
import com.kaii.dentix.domain.billing.util.BillingDescriptionMapper;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.global.common.dto.PagingDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class BillingDto {

    // =================================================================
    // 1. 청구 내역 요약 (Summary) - 목록 조회용
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private Long billingId;
        private Long organizationId;
        private String organizationName;
        private String planName;
        private BillingType billingType;    // ENUM 타입 유지 (JSON 직렬화 시 문자열 변환됨)
        private String billingStatus;       // Enum.name()
        private long amount;
        private LocalDateTime billedAt;
        private LocalDateTime paidAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private String description;

        public static Summary from(Billing billing) {
            return Summary.builder()
                    .billingId(billing.getId())
                    .organizationId(billing.getOrganization().getOrganizationId())
                    .organizationName(billing.getOrganization().getOrganizationName())
                    .planName(billing.getSubscriptionPlan().getPlanName().name())
                    .billingType(billing.getBillingType())
                    .billingStatus(billing.getBillingStatus().name())
                    .amount(billing.getAmount())
                    .billedAt(billing.getBilledAt())
                    .paidAt(billing.getPaidAt())
                    .periodStart(billing.getPeriodStart())
                    .periodEnd(billing.getPeriodEnd())
                    .description(BillingDescriptionMapper.toEnglish(billing.getDescription()))
                    .build();
        }
    }

    // =================================================================
    // 2. 청구 내역 상세 (Detail) - 단건 조회용
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Detail {
        private Long billingId;
        private String organizationName;
        private String planName;
        private String billingType;
        private String billingStatus;
        private Long amount;
        private LocalDateTime billedAt;
        private LocalDateTime paidAt;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private String description;
        private String paymentRef;
        private Date created;

        public static Detail from(Billing billing) {
            return Detail.builder()
                    .billingId(billing.getId())
                    .organizationName(billing.getOrganization().getOrganizationName())
                    .planName(billing.getSubscriptionPlan().getPlanName().name())
                    .billingType(billing.getBillingType().name())
                    .billingStatus(billing.getBillingStatus().name())
                    .amount(billing.getAmount())
                    .billedAt(billing.getBilledAt())
                    .paidAt(billing.getPaidAt())
                    .periodStart(billing.getPeriodStart())
                    .periodEnd(billing.getPeriodEnd())
                    .description(billing.getDescription())
                    .paymentRef(billing.getPaymentRef())
                    .created(billing.getCreated())
                    .build();
        }
    }

    // =================================================================
    // 3. 리스트 응답 래퍼 (ListResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ListResponse {
        private Long organizationId;
        private String organizationName;
        private List<Summary> billings;
    }

    // =================================================================
    // 4. 초과 요금 상세 (OveruseResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class OveruseResponse {
        private Long billingId;
        private String planName;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private long baseAmount;
        private long totalOveruseAmount;
        private long totalOveruseCount;
        private List<OveruseItem> overuseList;

        @Getter @Builder
        @NoArgsConstructor @AllArgsConstructor
        public static class OveruseItem {
            private Long billingId;
            private long amount;
            private LocalDateTime billedAt;
            private String billingStatus;
            private String description;

            public static OveruseItem from(Billing billing) {
                return OveruseItem.builder()
                        .billingId(billing.getId())
                        .amount(billing.getAmount())
                        .billedAt(billing.getBilledAt())
                        .billingStatus(billing.getBillingStatus().name())
                        .description(BillingDescriptionMapper.toEnglish(billing.getDescription()))
                        .build();
            }
        }
    }

    // =================================================================
    // 5. 구독별 초과 요금 (SubscriptionOveruse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionOveruse {
        private Long subscriptionId;
        private String planName;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
        private long totalAmount;
        private List<Detail> overuseList;
    }

    // =================================================================
    // 6. 상태 변경 요청 (StatusUpdateRequest)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class StatusUpdateRequest {
        @NotBlank(message = "변경할 상태는 필수입니다.")
        private String billingStatus; // "PAID", "UNPAID", "PENDING"
        private String memo;
    }

    // =================================================================
    // 7. 상태 변경 이력 응답 (StatusHistoryResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class StatusHistoryResponse {
        private Long historyId;
        private String prevStatus;
        private String newStatus;
        private String changedBy;
        private String memo;
        private Date created;

        public static StatusHistoryResponse from(BillingStatusHistory history) {
            return StatusHistoryResponse.builder()
                    .historyId(history.getBillingStatusHistoryId())
                    .prevStatus(history.getOldStatus().name())
                    .newStatus(history.getNewStatus().name())
                    .changedBy(history.getChangedBy())
                    .memo(history.getMemo())
                    .created(history.getCreated())
                    .build();
        }
    }

    // =================================================================
    // 8. 엑셀 데이터 (ExcelData)
    // =================================================================
    @Getter @Builder
    public static class ExcelData {
        private Long organizationId;
        private String organizationName;
        private List<Summary> summaries;
        private Map<Long, OveruseResponse> detailMap;
    }

    // =================================================================
    // [신규] 9. 페이지네이션 응답 (PagedResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class PagedResponse {
        private PagingDTO paging;       // 페이지 정보 (page, size, total 등)
        private List<Summary> content;  // 빌링 목록
    }
}