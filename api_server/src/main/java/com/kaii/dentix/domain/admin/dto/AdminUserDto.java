package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.dto.PagingDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.user.domain.UserDaeguCredentialStatus;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AdminUserDto {

    // =================================================================
    // 1. 사용자 목록 조회 (Search / List)
    // =================================================================
    @Getter @Setter @SuperBuilder
    @NoArgsConstructor @AllArgsConstructor
    public static class SearchRequest extends PageAndSizeRequest {
        private Long organizationId;
        private String keyword;       // userIdentifierOrName -> keyword (단축)
        private OralCheckResultType oralCheckResultTotalType;
        private String oralStatus;
        private GenderType gender;    // userGender -> gender
        private YnType isVerify;

        // 날짜 검색
        private DatePeriodType datePeriodType; // allDatePeriod -> datePeriodType
        private String startDate;
        private String endDate;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ListResponse {
        private PagingDTO paging;
        private List<Info> userList;

        public static ListResponse of(PagingDTO paging, List<Info> userList) {
            return ListResponse.builder()
                    .paging(paging)
                    .userList(userList)
                    .build();
        }
    }

    // 목록 내 단건 정보 (기존 AdminUserInfoDto)
    @Getter @Setter
    @NoArgsConstructor
    public static class Info {
        private Long userId;
        private String loginId;     // userLoginIdentifier
        private String name;        // userName
        private GenderType gender;  // userGender
        private OralCheckResultType oralCheckResultTotalType;
        private Date oralCheckDate;
        private YnType isVerify;
        private List<String> serviceNames;

        //QueryDSL Projections.constructor 용 생성자
        public Info(
                Long userId, String loginId, String name, GenderType gender,
                OralCheckResultType oralCheckResultTotalType, Date oralCheckDate,
                YnType isVerify, String serviceNamesStr
        ) {
            this.userId = userId;
            this.loginId = loginId;
            this.name = name;
            this.gender = gender;
            this.oralCheckResultTotalType = oralCheckResultTotalType;
            this.oralCheckDate = oralCheckDate;
            this.isVerify = isVerify;

            // 콤마로 연결된 문자열을 리스트로 변환
            if (serviceNamesStr != null && !serviceNamesStr.isEmpty()) {
                this.serviceNames = Arrays.stream(serviceNamesStr.split(","))
                        .map(String::trim)
                        .toList();
            } else {
                this.serviceNames = List.of();
            }
        }
    }

    // =================================================================
    // 2. 사용자 상세 정보 (Detail / Modify Info)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DetailResponse {
        private String loginId;
        private String name;
        private GenderType gender;

        // 정적 팩토리 메서드
        public static DetailResponse from(String loginId, String name, GenderType gender) {
            return DetailResponse.builder()
                    .loginId(loginId)
                    .name(name)
                    .gender(gender)
                    .build();
        }
    }

    // =================================================================
    // 3. 사용자 정보 수정 (Modify)
    // =================================================================
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifyRequest {
        private Long userId;
        private String loginId;
        private String name;
        private GenderType gender;
    }

    // =================================================================
    // 4. 사용자 일괄 업로드 (Bulk Upload)
    // =================================================================
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUploadResponse {
        private int successCount;
        private int createdCount;
        private int savedCount;
        private int count;
        private int failCount;
        private List<FailInfo> failList;

        public static BulkUploadResponse of(int successCount, List<FailInfo> failList) {
            int failCount = failList == null ? 0 : failList.size();

            return BulkUploadResponse.builder()
                    .successCount(successCount)
                    .createdCount(successCount)
                    .savedCount(successCount)
                    .count(successCount)
                    .failCount(failCount)
                    .failList(failList == null ? List.of() : failList)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailInfo {
        private int row;
        private String reason;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ExerciseProgressResponse {
        private int contentCount;
        private int rewardContentCount;
        private List<ExerciseProgressUser> users;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ExerciseProgressUser {
        private Long userId;
        private String userLoginIdentifier;
        private String userName;
        private int completedCount;
        private int overallCompletionRate;
        private int rewardReceivedCount;
        private List<ExerciseProgressContent> contents;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ExerciseProgressContent {
        private Long contentId;
        private int contentSort;
        private String title;
        private int completionRate;
        private boolean completed;
        private int lastPositionSeconds;
        private int durationSeconds;
        private Date lastViewedAt;
        private boolean rewardEligible;
        private boolean rewardReceived;
        private Date rewardReceivedAt;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DaeguRewardStatusResponse {
        private int essentialRewardContentCount;
        private List<DaeguRewardStatusUser> users;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class DaeguRewardStatusUser {
        private Long userId;
        private String userLoginIdentifier;
        private String userName;
        private String organizationName;
        private Date created;
        private Date userLastLoginDate;
        private String daeguDid;
        private UserDaeguIdentityStatus daeguDidStatus;
        private UserDaeguCredentialStatus daeguCredentialStatus;
        private LocalDate daeguCredentialValidFrom;
        private LocalDate daeguCredentialValidUntil;
        private boolean didIssued;
        private String walletDaeguDid;
        private String walletAddress;
        private long pointBalance;
        private boolean walletCreated;
        private int loginHistoryCount;
        private List<LoginHistory> loginHistories;
        private int essentialRewardReceivedCount;
        private boolean essentialRewardCompleted;
        private List<EssentialReward> essentialRewards;
        private int reclaimCount;
        private long reclaimedAmount;
        private boolean rewardReclaimed;
        private List<RewardTransactionSummary> reclaims;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class LoginHistory {
        private Long historyId;
        private Date loggedInAt;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class EssentialReward {
        private Long contentId;
        private int contentSort;
        private String title;
        private boolean rewardReceived;
        private Date rewardReceivedAt;
        private Long transactionId;
        private String tokenName;
        private UserRewardTransactionStatus status;
        private String txHash;
        private String factHash;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class RewardTransactionSummary {
        private Long transactionId;
        private String tokenName;
        private UserRewardTransactionStatus status;
        private long amount;
        private String txHash;
        private String factHash;
        private Date created;
    }
}
