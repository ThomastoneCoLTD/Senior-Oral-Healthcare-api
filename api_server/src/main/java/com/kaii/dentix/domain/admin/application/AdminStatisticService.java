package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.statistic.QuestionnaireStatisticDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckCustomRepository;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireCustomRepository;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatisticService {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final OralCheckService oralCheckService;
    private final OralCheckRepository oralCheckRepository;
    private final AdminUserCustomRepository adminUserCustomRepository;
    private final OralCheckCustomRepository oralCheckCustomRepository;
    private final QuestionnaireCustomRepository questionnaireCustomRepository;

    /**
     * 관리자 통계 조회 (기관용)
     */
    @Transactional(readOnly = true)
    public AdminStatisticDto.MainResponse getOrgStatistics(AdminStatisticDto.SearchRequest request, HttpServletRequest httpRequest) {

        // 1. 관리자 및 기관 ID 식별
        Long adminId = jwtTokenUtil.getUserId(jwtTokenUtil.getAccessToken(httpRequest), TokenType.AccessToken);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() == null) {
            throw new IllegalStateException("소속된 기관이 없습니다.");
        }

        // 기관 ID 자동 주입
        request.setOrganizationId(admin.getOrganization().getOrganizationId());

        return calculateStatistics(request);
    }

    /**
     * 공통 통계 계산 로직 (UserStatistic 메서드 통합)
     */
    private AdminStatisticDto.MainResponse calculateStatistics(AdminStatisticDto.SearchRequest request) {

        // 1. 가입률 통계
        //Repository가 이미 AdminStatisticDto.SignUpCount를 반환하므로 바로 할당
        AdminStatisticDto.SignUpCount signUpCount = adminUserCustomRepository.userSignUpCount(request);

        // 2. 구강검진 통계
        // (OralCheckCustomRepository는 아직 구형 DTO(OralCheckResultTypeCount)를 반환한다고 가정)
        var rawOralCheck = oralCheckCustomRepository.userOralCheckList(request);

        // 구형 DTO -> 신규 통합 DTO 변환
        AdminStatisticDto.OralCheckStats oralCheckStats = AdminStatisticDto.OralCheckStats.builder()
                .countHealthy(rawOralCheck.getCountHealthy())
                .countGood(rawOralCheck.getCountGood())
                .countAttention(rawOralCheck.getCountAttention())
                .countDanger(rawOralCheck.getCountDanger())
                .build();

        // 전체 검진 수 조회
        int allUserOralCheckCount = oralCheckCustomRepository.allUserOralCheckCount(request);

        // 평균 검진 횟수 계산
        int allOralCheckCount = oralCheckStats.getTotal();
        int oralCheckAverage = (allUserOralCheckCount > 0)
                ? Math.round((float) allOralCheckCount / allUserOralCheckCount)
                : 0;

        // 평균 구강 상태 계산 (기존 Service 활용)
        OralCheckResultType averageState = oralCheckService.getState(rawOralCheck);

        // 3. 문진표 통계
        // (QuestionnaireStatisticDto는 리스트로 받아와서 내부 메서드로 집계)
        List<QuestionnaireStatisticDto> qList = questionnaireCustomRepository.questionnaireList(request);
        int allQuestionnaireCount = questionnaireCustomRepository.allQuestionnaireCount(request);

        AdminStatisticDto.QuestionnaireStats qStats = calcQuestionnaireCount(qList);

        // 4. 최종 응답 생성
        return AdminStatisticDto.MainResponse.builder()
                .userSignUpCount(signUpCount)
                .averageState(averageState)
                .oralCheckCount(allOralCheckCount)
                .oralCheckAverage(oralCheckAverage)
                .oralCheckResultTypeCount(oralCheckStats)
                .questionnaireAllCount(allQuestionnaireCount)
                .allQuestionnaireResultTypeCount(qStats)
                .build();
    }
    /**
     * 문진표 유형별 Count 계산 (개선된 버전)
     */
    private AdminStatisticDto.QuestionnaireStats calcQuestionnaireCount(List<QuestionnaireStatisticDto> list) {
        if (list == null || list.isEmpty()) return new AdminStatisticDto.QuestionnaireStats();

        int[] counts = new int[11]; // A~K (11개)

        for (QuestionnaireStatisticDto dto : list) {
            if (dto.getQuestionnaireType() == null) continue;
            // 'A'의 아스키코드 값(65)을 빼서 인덱스로 사용 (A=0, B=1 ...)
            int index = dto.getQuestionnaireType().charAt(0) - 'A';
            if (index >= 0 && index < 11) {
                counts[index]++;
            }
        }

        return AdminStatisticDto.QuestionnaireStats.builder()
                .countA(counts[0]).countB(counts[1]).countC(counts[2]).countD(counts[3])
                .countE(counts[4]).countF(counts[5]).countG(counts[6]).countH(counts[7])
                .countI(counts[8]).countJ(counts[9]).countK(counts[10])
                .build();
    }

    /**
     * 기관 사용자 간략 통계 (대시보드 등)
     */
    @Transactional(readOnly = true)
    public AdminStatisticDto.OrgUserStatsResponse getOrganizationUserStatistics(HttpServletRequest httpRequest) {
        Long adminId = jwtTokenUtil.getUserId(jwtTokenUtil.getAccessToken(httpRequest), TokenType.AccessToken);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() == null) throw new IllegalStateException("소속 기관 없음");
        Long orgId = admin.getOrganization().getOrganizationId();

        long total = userRepository.countByOrganization_OrganizationId(orgId);
        long male = userRepository.countByOrganization_OrganizationIdAndUserGender(orgId, GenderType.M);
        long female = userRepository.countByOrganization_OrganizationIdAndUserGender(orgId, GenderType.W);

        // 최근 7일 가입자
        Date oneWeekAgo = java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(7));
        long newUsers = userRepository.countByOrganization_OrganizationIdAndCreatedAfter(orgId, oneWeekAgo);

        // 구강 상태 (기존 로직 유지하되 DTO 변환 필요할 수 있음)
        // List<OralCheckResultTypeCount> -> List<AdminStatisticDto.OralCheckStats> 변환 로직 필요 시 추가

        return AdminStatisticDto.OrgUserStatsResponse.builder()
                .organizationName(admin.getOrganization().getOrganizationName())
                .totalUsers(total)
                .maleUsers(male)
                .femaleUsers(female)
                .newUsers(newUsers)
                // .oralCheckStats(...) // 필요한 경우 변환하여 세팅
                .build();
    }

    /**
     * 슈퍼관리자용 전체 통계
     */
    @Transactional(readOnly = true)
    public AdminStatisticDto.SuperAdminTotalResponse getSuperAdminTotalStats(Admin admin) {
        if (admin.getAdminIsSuper() != YnType.Y) {
            throw new BadRequestApiException("슈퍼관리자 권한 필요");
        }

        // 전체 유저 카운트
        long total = userRepository.count();
        long male = userRepository.countByUserGender(GenderType.M);
        long female = userRepository.countByUserGender(GenderType.W);
        long newUsers = userRepository.countByCreatedAfter(java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(7)));

        return AdminStatisticDto.SuperAdminTotalResponse.builder()
                .totalUsers(total)
                .maleUsers(male)
                .femaleUsers(female)
                .newUsers7Days(newUsers)
                // .organizationStats(orgStats) // 필요 시 추가
                .build();
    }
}