package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.admin.dto.superAdmin.SuperAdminUserStatisticResponse;
import com.kaii.dentix.domain.appService.domain.QAppService;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserSignUpCountDto;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.oralCheck.domain.QOralCheck;
import com.kaii.dentix.domain.oralStatus.domain.QOralStatus;
import com.kaii.dentix.domain.organization.domain.QOrganization;
import com.kaii.dentix.domain.questionnaire.domain.QQuestionnaire;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.domain.QUser;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus;
import com.kaii.dentix.domain.userToAppService.domain.QUserToAppService;
import com.kaii.dentix.global.common.dto.PagingRequest;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
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
    private final QUserOralStatus userOralStatus = QUserOralStatus.userOralStatus;
    private final QOralStatus oralStatus = QOralStatus.oralStatus;
    private final QAppService appService = QAppService.appService;
    private final QUserToAppService userToAppService = QUserToAppService.userToAppService;
    private final QOrganization organization = QOrganization.organization;


    /**
     * 사용자 목록 조회
     */
    @Override
    public Page<AdminUserInfoDto> findAll(AdminUserListRequest request) {

        Pageable paging = new PagingRequest(request.getPage(), request.getSize()).of();

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(whereSearch(request));

        if (request.getOrganizationId() != null) {
            builder.and(user.organization.organizationId.eq(request.getOrganizationId()));
        }

        //1) 사용자 목록만 조회 (가장 빠름)
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

        //2) 최신 설문
        Map<Long, Questionnaire> latestQuestionnaires = getLatestQuestionnaires(userIds);

        //3) 최신 검사 결과
        Map<Long, OralCheck> latestOralChecks = getLatestOralChecks(userIds);

        //4) 유저별 oralStatusTitle
        Map<Long, String> oralStatusTitles = getOralStatusTitles(userIds);

        //5) 유저별 사용 서비스명
        Map<Long, List<String>> serviceNames = getUserServices(userIds);

        //6) DTO 리스트 조립
        List<AdminUserInfoDto> result = users.stream()
                .map(u -> new AdminUserInfoDto(
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

                        // serviceNames는 콤마 연결만 넘기면 DTO가 알아서 split 처리함
                        serviceNames.get(u.getUserId()) != null
                                ? String.join(",", serviceNames.get(u.getUserId()))
                                : ""
                ))
                .toList();

        Long totalCount = queryFactory
                .select(user.count())
                .from(user)
                .where(builder)
                .fetchOne();
        if (totalCount == null) totalCount = 0L;
        return new PageImpl<>(result, paging, totalCount);
    }


    @Override
    public Page<AdminUserInfoDto> findAllByOrganization(AdminUserListRequest request) {
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

        QUserOralStatus uos = QUserOralStatus.userOralStatus;
        QOralStatus os = QOralStatus.oralStatus;

        List<Tuple> list = queryFactory
                .select(uos.questionnaire.userId, os.oralStatusTitle)
                .from(uos)
                .leftJoin(uos.oralStatus, os)
                .where(uos.questionnaire.userId.in(userIds))
                .fetch();

        return list.stream()
                .filter(t -> t.get(uos.questionnaire.userId) != null)  // key null 제거
                .collect(Collectors.toMap(
                        t -> t.get(uos.questionnaire.userId),
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
                // ❗ key null 제거
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
    private Predicate whereSearch(AdminUserListRequest request) {

        BooleanBuilder builder = new BooleanBuilder();

        // 검색어
        if (StringUtils.isNotBlank(request.getUserIdentifierOrName())) {
            String keyword = request.getUserIdentifierOrName();
            builder.and(
                    user.userLoginIdentifier.contains(keyword)
                            .or(user.userName.contains(keyword))
            );
        }

        // 구강검사 결과
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

        // 문진표 유형 (oralStatusTitle)
        if (StringUtils.isNotBlank(request.getOralStatus())) {
            builder.and(
                    user.userId.in(
                            JPAExpressions
                                    .select(questionnaire.userId)
                                    .from(userOralStatus)
                                    .join(userOralStatus.questionnaire, questionnaire)
                                    .join(userOralStatus.oralStatus, oralStatus)
                                    .where(oralStatus.oralStatusTitle.eq(request.getOralStatus()))
                    )
            );
        }

        // 성별
        if (request.getUserGender() != null) {
            builder.and(user.userGender.eq(request.getUserGender()));
        }

        // 인증 여부
        if (request.getIsVerify() != null) {
            builder.and(user.isVerify.eq(request.getIsVerify()));
        }

        // 자동 기간
        if (request.getAllDatePeriod() != null) {
            builder.and(whereAllDatePeriodAuto(request.getAllDatePeriod()));
        }

        // 날짜 범위
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
     * 가입 통계
     */
    @Override
    public AdminUserSignUpCountDto userSignUpCount(AdminStatisticRequest request) {
        return queryFactory
                .select(Projections.constructor(
                        AdminUserSignUpCountDto.class,
                        Wildcard.count.longValue(),
                        new CaseBuilder().when(user.userGender.eq(GenderType.M)).then(1L).otherwise(0L).sum(),
                        new CaseBuilder().when(user.userGender.eq(GenderType.W)).then(1L).otherwise(0L).sum()
                ))
                .from(user)
                .where(whereUserEndDate(request.getEndDate()))
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
    public List<SuperAdminUserStatisticResponse> getAllOrganizationUserStats() {

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        Date oneMonthAgoDate = Date.from(oneMonthAgo.atZone(ZoneId.systemDefault()).toInstant());

        return queryFactory
                .select(Projections.constructor(
                        SuperAdminUserStatisticResponse.class,
                        organization.organizationId,
                        organization.organizationName,
                        user.countDistinct(),
                        user.userGender.when(GenderType.M).then(1L).otherwise(0L).sum(),
                        user.userGender.when(GenderType.W).then(1L).otherwise(0L).sum(),
                        new CaseBuilder().when(user.created.gt(oneMonthAgoDate)).then(1L).otherwise(0L).sum()
                ))
                .from(user)
                .join(user.organization, organization)
                .groupBy(organization.organizationId, organization.organizationName)
                .orderBy(organization.organizationId.asc())
                .fetch();
    }
}
