package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.appService.domain.QAppService;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.oralCheck.domain.QOralCheck;
import com.kaii.dentix.domain.oralStatus.domain.QOralStatus;
import com.kaii.dentix.domain.organization.domain.QOrganization;
import com.kaii.dentix.domain.questionnaire.domain.QQuestionnaire;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminStatisticDto;
import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.domain.QUser;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.oralStatusAssignment.domain.QOralStatusAssignment;
import com.kaii.dentix.domain.appService.domain.QUserToAppService;
import com.kaii.dentix.global.common.dto.PagingRequest;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AdminUserRepositoryImpl implements AdminUserCustomRepository {
    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;
    private final QOralCheck oralCheck = QOralCheck.oralCheck;
    private final QQuestionnaire questionnaire = QQuestionnaire.questionnaire;
    private final QOralStatusAssignment oralStatusAssignment = QOralStatusAssignment.oralStatusAssignment;
    private final QOralStatus oralStatus = QOralStatus.oralStatus;
    private final QOrganization organization = QOrganization.organization;

    /**
     * 사용자 목록 조회
     */
    @Override
    public Page<AdminUserDto.Info> findAll(AdminUserDto.SearchRequest request) { //타입 변경

        Pageable paging = new PagingRequest(request.getPage(), request.getSize()).of();

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(whereSearch(request)); //하단 메서드도 수정 필요

        if (request.getOrganizationId() != null) {
            builder.and(user.organization.organizationId.eq(request.getOrganizationId()));
        }

        // 1) 사용자 목록 조회
        List<User> users = queryFactory
                .selectFrom(user)
                .leftJoin(user.organization, organization).fetchJoin()
                .where(builder)
                .orderBy(user.created.desc())
                .offset(paging.getOffset())
                .limit(paging.getPageSize())
                .fetch();

        if (users.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), paging, 0);
        }

        List<Long> userIds = users.stream().map(User::getUserId).toList();

        // 2~5) 연관 데이터 조회 (기존 로직 유지)
        Map<Long, Questionnaire> latestQuestionnaires = getLatestQuestionnaires(userIds);
        Map<Long, OralCheck> latestOralChecks = getLatestOralChecks(userIds);
        Map<Long, String> oralStatusTitles = getOralStatusTitles(userIds);
        Map<Long, List<String>> serviceNames = getUserServices(userIds);

        // 6) DTO 리스트 조립 (AdminUserDto.Info 생성자 사용)
        List<AdminUserDto.Info> result = users.stream()
                .map(u -> new AdminUserDto.Info( //생성자 호출 변경
                        u.getUserId(),
                        u.getUserLoginIdentifier(),
                        u.getUserName(),
                        u.getUserGender(),

                        // oralStatusTitle
                        oralStatusTitles.getOrDefault(u.getUserId(), null),

                        // questionnaireDate
                        latestQuestionnaires.containsKey(u.getUserId())
                                ? latestQuestionnaires.get(u.getUserId()).getCreated()
                                : null,

                        // oralCheckResultTotalType
                        latestOralChecks.containsKey(u.getUserId())
                                ? latestOralChecks.get(u.getUserId()).getOralCheckResultTotalType()
                                : null,

                        // oralCheckDate
                        latestOralChecks.containsKey(u.getUserId())
                                ? latestOralChecks.get(u.getUserId()).getCreated()
                                : null,

                        u.getIsVerify(),

                        // serviceNames (List -> String 변환해서 넘김, DTO 내부에서 다시 List로 변환)
                        serviceNames.get(u.getUserId()) != null
                                ? String.join(",", serviceNames.get(u.getUserId()))
                                : ""
                ))
                .toList();

        // Count Query
        Long totalCount = queryFactory
                .select(user.count())
                .from(user)
                .where(builder)
                .fetchOne();
        if (totalCount == null) totalCount = 0L;

        return new PageImpl<>(result, paging, totalCount);
    }

    @Override
    public Page<AdminUserDto.Info> findAllByOrganization(AdminUserDto.SearchRequest request) { //타입 변경
        if (request.getOrganizationId() == null)
            throw new IllegalArgumentException("organizationId가 필요합니다.");
        return findAll(request);
    }

    private Map<Long, Questionnaire> getLatestQuestionnaires(List<Long> userIds) {

        QQuestionnaire q = QQuestionnaire.questionnaire;

        List<Questionnaire> list = queryFactory
                .selectFrom(q)
                .where(q.userId.in(userIds)
                        .and(q.created.in(
                                JPAExpressions
                                        .select(q.created.max())
                                        .from(q)
                                        .where(q.userId.in(userIds))
                                        .groupBy(q.userId)
                        )))
                .fetch();

        return list.stream()
                .filter(qn -> qn != null && qn.getUserId() != null)
                .collect(Collectors.toMap(
                        Questionnaire::getUserId,
                        qn -> qn,
                        (a, b) -> a
                ));
    }

    private Map<Long, OralCheck> getLatestOralChecks(List<Long> userIds) {

        QOralCheck oc = QOralCheck.oralCheck;

        List<OralCheck> list = queryFactory
                .selectFrom(oc)
                .where(oc.user.userId.in(userIds)
                        .and(oc.created.in(
                                JPAExpressions
                                        .select(oc.created.max())
                                        .from(oc)
                                        .where(oc.user.userId.in(userIds))
                                        .groupBy(oc.user.userId)
                        )))
                .fetch();

        return list.stream()
                .filter(ocx -> ocx != null &&
                        ocx.getUser() != null &&
                        ocx.getUser().getUserId() != null)
                .collect(Collectors.toMap(
                        ocx -> ocx.getUser().getUserId(),
                        ocx -> ocx,
                        (a, b) -> a
                ));
    }

    private Map<Long, String> getOralStatusTitles(List<Long> userIds) {

        QOralStatusAssignment oralStatusAssignmentAlias = QOralStatusAssignment.oralStatusAssignment;
        QOralStatus os = QOralStatus.oralStatus;

        List<Tuple> list = queryFactory
                .select(oralStatusAssignmentAlias.questionnaire.userId, os.oralStatusTitle)
                .from(oralStatusAssignmentAlias)
                .leftJoin(oralStatusAssignmentAlias.oralStatus, os)
                .where(oralStatusAssignmentAlias.questionnaire.userId.in(userIds))
                .fetch();

        return list.stream()
                .filter(t -> t.get(oralStatusAssignmentAlias.questionnaire.userId) != null)
                .collect(Collectors.toMap(
                        t -> t.get(oralStatusAssignmentAlias.questionnaire.userId),
                        t -> {
                            String value = t.get(os.oralStatusTitle);
                            return value != null ? value : "";
                        },
                        (a, b) -> a
                ));
    }

    private Map<Long, List<String>> getUserServices(List<Long> userIds) {

        QUserToAppService uta = QUserToAppService.userToAppService;
        QAppService as = QAppService.appService;

        List<Tuple> list = queryFactory
                .select(uta.user.userId, as.name)
                .from(uta)
                .leftJoin(uta.appService, as)
                .where(uta.user.userId.in(userIds))
                .fetch();

        return list.stream()
                //key null 제거
                .filter(t -> {
                    Long key = t.get(uta.user.userId);
                    return key != null;
                })
                .collect(Collectors.groupingBy(
                        t -> t.get(uta.user.userId),
                        Collectors.mapping(
                                t -> Optional.ofNullable(t.get(as.name)).orElse(""),
                                Collectors.toList()
                        )
                ));
    }


    /**
     * WHERE 조건 생성
     */
    private Predicate whereSearch(AdminUserDto.SearchRequest request) {

        BooleanBuilder builder = new BooleanBuilder();

        // 1. 검색어
        if (StringUtils.isNotBlank(request.getKeyword())) {
            String keyword = request.getKeyword();
            builder.and(
                    user.userLoginIdentifier.contains(keyword)
                            .or(user.userName.contains(keyword))
            );
        }

        // 2. 구강검사 결과
        if (request.getOralCheckResultTotalType() != null) {
            builder.and(
                    user.userId.in(
                            JPAExpressions
                                    .select(oralCheck.user.userId)
                                    .from(oralCheck)
                                    .where(oralCheck.oralCheckResultTotalType.eq(request.getOralCheckResultTotalType()))
                    )
            );
        }

        // 3. 문진표 유형
        if (StringUtils.isNotBlank(request.getOralStatus())) {
            builder.and(
                    user.userId.in(
                            JPAExpressions
                                    .select(questionnaire.userId)
                .from(oralStatusAssignment)
                .join(oralStatusAssignment.questionnaire, questionnaire)
                .join(oralStatusAssignment.oralStatus, oralStatus)
                                    .where(oralStatus.oralStatusTitle.eq(request.getOralStatus()))
                    )
            );
        }

        // 4. 성별
        if (request.getGender() != null) {
            builder.and(user.userGender.eq(request.getGender()));
        }

        // 5. 인증 여부
        if (request.getIsVerify() != null) {
            builder.and(user.isVerify.eq(request.getIsVerify()));
        }

        // 6. 자동 기간
        if (request.getDatePeriodType() != null) {
            builder.and(whereAllDatePeriodAuto(request.getDatePeriodType()));
        }

        // 7. 날짜 범위
        builder.and(whereDateRange(request.getStartDate(), request.getEndDate()));

        return builder;
    }


    /**
     * 시작일 / 종료일 (Date 타입 기준)
     */
    private BooleanExpression whereDateRange(String start, String end) {

        BooleanExpression oral = null;
        BooleanExpression ques = null;

        try {
            if (StringUtils.isNotBlank(start)) {
                Date startDt = DateFormatUtil.stringToDate("yyyy-MM-dd", start);

                oral = oralCheck.created.goe(startDt);
                ques = questionnaire.created.goe(startDt);
            }

            if (StringUtils.isNotBlank(end)) {
                Date endDt = DateFormatUtil.stringToDate("yyyy-MM-dd", end);

                // 종료일 포함(+1)
                Calendar cal = Calendar.getInstance();
                cal.setTime(endDt);
                cal.add(Calendar.DATE, 1);
                Date endInclusive = cal.getTime();

                BooleanExpression oc = oralCheck.created.loe(endInclusive);
                BooleanExpression qc = questionnaire.created.loe(endInclusive);

                oral = (oral == null) ? oc : oral.and(oc);
                ques = (ques == null) ? qc : ques.and(qc);
            }

        } catch (Exception e) {
            return null;
        }

        if (oral == null && ques == null) return null;

        return oral.or(ques);
    }



    /**
     * 자동 기간 (오늘 / 1주 / 1달 / 3달 / 1년)
     */
    private BooleanExpression whereAllDatePeriodAuto(DatePeriodType type) {

        if (type == null || type == DatePeriodType.ALL) {
            return null;
        }

        Calendar cal = Calendar.getInstance();

        switch (type) {
            case TODAY -> cal.add(Calendar.DATE, -1);
            case WEEK1 -> cal.add(Calendar.DATE, -7);
            case MONTH1 -> cal.add(Calendar.MONTH, -1);
            case MONTH3 -> cal.add(Calendar.MONTH, -3);
            case YEAR1 -> cal.add(Calendar.YEAR, -1);
        }

        Date startDate = cal.getTime();

        BooleanExpression oral = oralCheck.created.goe(startDate);
        BooleanExpression ques = questionnaire.created.goe(startDate);

        return oral.or(ques);
    }



    /**
     * 가입자 수 통계 (수정됨)
     */
    @Override
    public AdminStatisticDto.SignUpCount userSignUpCount(AdminStatisticDto.SearchRequest request) { // 타입 변경

        return queryFactory
                .select(Projections.constructor(AdminStatisticDto.SignUpCount.class, //생성자 변경
                        user.count().as("countAll"),
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.M)).then(1L)
                                .otherwise(0L).sum().as("countMan"),
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.W)).then(1L)
                                .otherwise(0L).sum().as("countWoman")
                ))
                .from(user)
                .where(
                        // request.getDatePeriodType() 등 통계용 검색 조건 사용
                        whereDateRange(request.getStartDate(), request.getEndDate()),
                        request.getOrganizationId() != null ? user.organization.organizationId.eq(request.getOrganizationId()) : null
                )
                .fetchOne();
    }


    private BooleanExpression whereUserEndDate(String endDate) {
        if (endDate == null) return null;

        try {
            Date date = DateFormatUtil.stringToDate("yyyy-MM-dd", endDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, 1);
            Date finalDate = cal.getTime();
            return user.created.lt(finalDate);

        } catch (Exception e) {
            return null;
        }
    }



    /**
     * 기관별 통계
     */
    @Override
    public List<SuperAdminStatisticDto.OrgUserStats> getAllOrganizationUserStats() { //리턴 타입 변경

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        Date oneMonthAgoDate = Date.from(oneMonthAgo.atZone(ZoneId.systemDefault()).toInstant());

        return queryFactory
                .select(Projections.constructor(
                        SuperAdminStatisticDto.OrgUserStats.class, //타겟 클래스 변경
                        organization.organizationId,
                        organization.organizationName,
                        user.countDistinct(),

                        // 남성
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.M)).then(1L)
                                .otherwise(0L).sum(),

                        // 여성
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.W)).then(1L)
                                .otherwise(0L).sum(),

                        // 신규 가입
                        new CaseBuilder()
                                .when(user.created.gt(oneMonthAgoDate)).then(1L)
                                .otherwise(0L).sum()
                ))
                .from(user)
                .join(user.organization, organization)
                .groupBy(organization.organizationId, organization.organizationName)
                .orderBy(organization.organizationId.asc())
                .fetch();
    }
}
