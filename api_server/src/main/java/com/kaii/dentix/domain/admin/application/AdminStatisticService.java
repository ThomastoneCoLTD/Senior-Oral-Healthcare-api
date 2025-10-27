package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.*;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.admin.dto.statistic.*;
import com.kaii.dentix.domain.admin.dto.superAdmin.SuperAdminUserStatisticResponse;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckCustomRepository;
import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionResponse;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireCustomRepository;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatisticService {

    private final AdminUserCustomRepository adminUserCustomRepository;

    private final OralCheckCustomRepository oralCheckCustomRepository;

    private final OralCheckService oralCheckService;

    private final QuestionnaireCustomRepository questionnaireCustomRepository;

    private final JwtTokenUtil jwtTokenUtil;

    private final AdminRepository adminRepository;
private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OralCheckRepository oralCheckRepository;
private final AdminService adminService;
//    private final Admin admin;
    /**
     *  사용자 통계
     */
    @Transactional(readOnly = true)
    public AdminUserStatisticResponse userStatistic(AdminStatisticRequest request){

        // 통계 1. 전체 남녀 가입률
        AdminUserSignUpCountDto userSignUpCount = adminUserCustomRepository.userSignUpCount(request);

        // 통계 2. 평균 구강검진
        OralCheckResultTypeCount userOralCheckList = oralCheckCustomRepository.userOralCheckList(request); // 구강검진 결과 타입별 횟수

        int allUserOralCheckCount = oralCheckCustomRepository.allUserOralCheckCount(request);  // 구강검진을 한 총 사용자 수

        int allOralCheckCount = userOralCheckList.getCountHealthy() + userOralCheckList.getCountGood() + userOralCheckList.getCountAttention() + userOralCheckList.getCountDanger(); // 전체 구강검진 횟수
        int oralCheckAverage = 0; // 사용자 당 평균 구강검진 횟수

        if (allUserOralCheckCount > 0) {
            oralCheckAverage = Math.round((float) allOralCheckCount / allUserOralCheckCount);
        }

        OralCheckResultType averageState = oralCheckService.getState(userOralCheckList); // 전체 평균 구강 상태

        // 통계 3. 평균 문진표 유형
        List<QuestionnaireStatisticDto> questionnaireList = questionnaireCustomRepository.questionnaireList(request); // 모든 문진표 리스트

        int allQuestionnaireCount = questionnaireCustomRepository.allQuestionnaireCount(request); // 전체 문진표 작성 횟수

        AllQuestionnaireResultTypeCount allQuestionnaireResultTypeCount = new AllQuestionnaireResultTypeCount(); // 모든 문진표 결과 유형

        if (allQuestionnaireCount > 0){
            int countA = 0;
            int countB = 0;
            int countC = 0;
            int countD = 0;
            int countE = 0;
            int countF = 0;
            int countG = 0;
            int countH = 0;
            int countI = 0;
            int countJ = 0;
            int countK = 0;

            for (QuestionnaireStatisticDto questionnaireStatisticDto : questionnaireList){
                switch (questionnaireStatisticDto.getQuestionnaireType()) { // 문진표 결과 타입별 횟수 count
                    case "A" -> countA ++;
                    case "B" -> countB ++;
                    case "C" -> countC ++;
                    case "D" -> countD ++;
                    case "E" -> countE ++;
                    case "F" -> countF ++;
                    case "G" -> countG ++;
                    case "H" -> countH ++;
                    case "I" -> countI ++;
                    case "J" -> countJ ++;
                    case "K" -> countK ++;
                }
            }

            allQuestionnaireResultTypeCount = AllQuestionnaireResultTypeCount.builder()
                    .countA(countA)
                    .countB(countB)
                    .countC(countC)
                    .countD(countD)
                    .countE(countE)
                    .countF(countF)
                    .countG(countG)
                    .countH(countH)
                    .countI(countI)
                    .countJ(countJ)
                    .countK(countK)
                    .build();
        }

        return AdminUserStatisticResponse.builder()
                .userSignUpCount(userSignUpCount)
                .averageState(averageState)
                .oralCheckCount(allOralCheckCount)
                .oralCheckAverage(oralCheckAverage)
                .oralCheckResultTypeCount(userOralCheckList)
                .questionnaireAllCount(allQuestionnaireCount)
                .allQuestionnaireResultTypeCount(allQuestionnaireResultTypeCount)
                .build();
    }

    /**
     *  관리자 통계 조회
     */
    @Transactional(readOnly = true)
    public AdminUserStatisticResponse getOrgStatistics(AdminStatisticRequest request, HttpServletRequest httpRequest) {

        // ✅ 1️⃣ 현재 로그인 관리자 식별
        Long adminId = jwtTokenUtil.getUserId(
                jwtTokenUtil.getAccessToken(httpRequest),
                TokenType.AccessToken
        );

        // ✅ 2️⃣ 관리자 → 소속 기관 ID 가져오기
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() == null) {
            throw new IllegalStateException("현재 관리자는 아직 기관에 소속되어 있지 않습니다.");
        }

        Long organizationId = admin.getOrganization().getOrganizationId();

        // ✅ 3️⃣ request 에 기관ID 자동 주입
        request.setOrganizationId(organizationId);

//        log.info("📊 통계 요청 관리자={}, 기관ID={}", admin.getAdminName(), organizationId);

        // ✅ 4️⃣ 통계 1. 전체 남녀 가입률
        AdminUserSignUpCountDto userSignUpCount = adminUserCustomRepository.userSignUpCount(request);

        // ✅ 5️⃣ 통계 2. 구강검진 통계
        OralCheckResultTypeCount userOralCheckList = oralCheckCustomRepository.userOralCheckList(request);
        int allUserOralCheckCount = oralCheckCustomRepository.allUserOralCheckCount(request);
        int allOralCheckCount = userOralCheckList.getCountHealthy()
                + userOralCheckList.getCountGood()
                + userOralCheckList.getCountAttention()
                + userOralCheckList.getCountDanger();

        int oralCheckAverage = allUserOralCheckCount > 0
                ? Math.round((float) allOralCheckCount / allUserOralCheckCount)
                : 0;

        OralCheckResultType averageState = oralCheckService.getState(userOralCheckList);

        // ✅ 6️⃣ 통계 3. 문진표 통계
        List<QuestionnaireStatisticDto> questionnaireList = questionnaireCustomRepository.questionnaireList(request);
        int allQuestionnaireCount = questionnaireCustomRepository.allQuestionnaireCount(request);

        AllQuestionnaireResultTypeCount allQuestionnaireResultTypeCount = calcQuestionnaireCount(questionnaireList, allQuestionnaireCount);

        // ✅ 7️⃣ 응답 반환
        return AdminUserStatisticResponse.builder()
                .userSignUpCount(userSignUpCount)
                .averageState(averageState)
                .oralCheckCount(allOralCheckCount)
                .oralCheckAverage(oralCheckAverage)
                .oralCheckResultTypeCount(userOralCheckList)
                .questionnaireAllCount(allQuestionnaireCount)
                .allQuestionnaireResultTypeCount(allQuestionnaireResultTypeCount)
                .build();
    }

    /**
     * 문진표 유형별 count 계산
     */
    private AllQuestionnaireResultTypeCount calcQuestionnaireCount(List<QuestionnaireStatisticDto> list, int totalCount) {
        if (totalCount == 0) return new AllQuestionnaireResultTypeCount();

        int[] counts = new int[11]; // A~K (11개)
        for (QuestionnaireStatisticDto dto : list) {
            switch (dto.getQuestionnaireType()) {
                case "A" -> counts[0]++;
                case "B" -> counts[1]++;
                case "C" -> counts[2]++;
                case "D" -> counts[3]++;
                case "E" -> counts[4]++;
                case "F" -> counts[5]++;
                case "G" -> counts[6]++;
                case "H" -> counts[7]++;
                case "I" -> counts[8]++;
                case "J" -> counts[9]++;
                case "K" -> counts[10]++;
            }
        }

        return AllQuestionnaireResultTypeCount.builder()
                .countA(counts[0]).countB(counts[1]).countC(counts[2]).countD(counts[3])
                .countE(counts[4]).countF(counts[5]).countG(counts[6]).countH(counts[7])
                .countI(counts[8]).countJ(counts[9]).countK(counts[10])
                .build();
    }

    @Transactional(readOnly = true)
    public AdminStatisticsOrgUserResponse getOrganizationUserStatistics(HttpServletRequest httpRequest) {

        // 1️⃣ 로그인 관리자 식별
        Long adminId = jwtTokenUtil.getUserId(
                jwtTokenUtil.getAccessToken(httpRequest),
                TokenType.AccessToken
        );

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() == null)
            throw new IllegalStateException("관리자가 소속된 기관이 없습니다.");

        Long orgId = admin.getOrganization().getOrganizationId();

        // 2️⃣ 기관 내 사용자 통계 조회
        long totalUsers = userRepository.countByOrganization_OrganizationId(orgId);
        long maleUsers = userRepository.countByOrganization_OrganizationIdAndUserGender(orgId, GenderType.M);
        long femaleUsers = userRepository.countByOrganization_OrganizationIdAndUserGender(orgId, GenderType.W);

        // 3️⃣ 최근 가입자 (7일)
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        Date oneWeekAgoDate = java.sql.Timestamp.valueOf(oneWeekAgo);

        long newUsers = userRepository.countByOrganization_OrganizationIdAndCreatedAfter(orgId, oneWeekAgoDate);
//        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
//        long newUsers = userRepository.countByOrganization_OrganizationIdAndCreatedAfter(orgId, oneWeekAgo.atStartOfDay());

        // 4️⃣ 전체 구강 상태별 사용자 (최근 검진 기준)
        List<OralCheckResultTypeCount> oralStateCounts = oralCheckRepository.countByOrganization(orgId);

        return AdminStatisticsOrgUserResponse.builder()
                .organizationName(admin.getOrganization().getOrganizationName())
                .totalUsers(totalUsers)
                .maleUsers(maleUsers)
                .femaleUsers(femaleUsers)
                .newUsers(newUsers)
                .oralCheckStats(oralStateCounts)
                .build();
    }

    @Transactional(readOnly = true)
    public SubscriptionInfoResponse getMySubscriptionInfo(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관 정보를 찾을 수 없습니다."));

        SubscriptionPlan plan = org.getSubscriptionPlan();
        if (plan == null) {
            throw new NotFoundDataException("기관의 구독 플랜 정보를 찾을 수 없습니다.");
        }

        // ✅ 실시간 계산 대신 Organization 엔티티 값 사용
        int totalSuccessCount = org.getSuccessCount() != null ? org.getSuccessCount() : 0;
        int max = plan.getMaxSuccessResponses();
        int remaining = Math.max(0, max - totalSuccessCount);
        double usageRate = org.getUsageRate() != null
                ? org.getUsageRate()
                : (max == 0 ? 0 : (double) totalSuccessCount / max * 100.0);

        // ✅ 사용자별 successCount 필드 사용
        List<User> users = userRepository.findByOrganization_OrganizationId(organizationId);
        List<SubscriptionInfoResponse.UserUsage> userUsages = users.stream()
                .map(u -> SubscriptionInfoResponse.UserUsage.builder()
                        .userId(u.getUserId())
                        .userName(u.getUserName())
                        .successCount(u.getSuccessCount() != null ? u.getSuccessCount() : 0)
                        .build())
                .toList();

        // ✅ 응답 DTO 구성
        return SubscriptionInfoResponse.builder()
                .organizationName(org.getOrganizationName())
                .planName(plan.getPlanName())
                .planCycle(plan.getPlanCycle())
                .price(plan.getPrice())
                .maxSuccessResponses(max)
                .totalSuccessCount(totalSuccessCount)  // ✅ DB count 대신 Organization 값
                .remainingCount(remaining)
                .usageRate(Math.round(usageRate * 10) / 10.0)
                .subscriptionStartDate(org.getSubscriptionStartDate())
                .usageResetDate(org.getUsageResetDate())
                .users(userUsages)
                .build();
    }
//    @Transactional(readOnly = true)
//    public List<OrganizationSubscriptionResponse> getSubscriptionUsage(Admin admin) {
//        if (admin.getRoles() == UserRole.ROLE_SUPER_ADMIN) {
//            // ✅ 모든 기관의 구독 정보 조회
//            return organizationRepository.findAllWithSubscription()
//                    .stream()
//                    .map(OrganizationSubscriptionResponse::from)
//                    .toList();
//        } else {
//            // ✅ 본인 기관만 조회
//            Organization org = admin.getOrganization();
//            return List.of(OrganizationSubscriptionResponse.from(org));
//        }
//    }
@Transactional(readOnly = true)
public List<SuperAdminUserStatisticResponse> getAllOrganizationUserStats(Admin admin) {
    // ✅ 슈퍼관리자 권한 검증
    if (admin.getAdminIsSuper() != YnType.Y) {
        throw new BadRequestApiException("슈퍼관리자만 접근할 수 있습니다.");
    }

    return adminUserCustomRepository.getAllOrganizationUserStats();
}

}
