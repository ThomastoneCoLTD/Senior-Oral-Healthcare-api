//package com.kaii.dentix.domain.subscription.domain;
//
//import com.kaii.dentix.domain.organization.domain.Organization;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.YearMonth;
//
//@Entity
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@Table(name = "subscription_usage_history")
//public class SubscriptionUsageHistory {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "organization_id")
//    private Organization organization;
//
//    private YearMonth usageMonth;   // 예: 2025-11
//    private int usedCount;          // 사용된 횟수
//    private int overusedCount;      // 초과 사용 횟수
//
//    public void increaseUsage() {
//        usedCount++;
//        if (organization.getRemainingResponses() > 0) {
//            organization.setRemainingResponses(organization.getRemainingResponses() - 1);
//        } else {
//            overusedCount++;
//        }
//    }
//}