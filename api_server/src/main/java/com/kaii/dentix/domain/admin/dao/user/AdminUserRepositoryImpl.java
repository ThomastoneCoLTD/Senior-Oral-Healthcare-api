package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
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
    private static final List<String> DEFAULT_SERVICE_NAMES = List.of("PLAQUE", "PERIODONTAL");

    /**
     * ?ъ슜??紐⑸줉 議고쉶
     */
    @Override
    public Page<AdminUserDto.Info> findAll(AdminUserDto.SearchRequest request) { //???蹂寃?

        Pageable paging = new PagingRequest(request.getPage(), request.getSize()).of();

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(whereSearch(request)); //?섎떒 硫붿꽌?쒕룄 ?섏젙 ?꾩슂

        if (request.getOrganizationId() != null) {
            builder.and(user.organization.organizationId.eq(request.getOrganizationId()));
        }

        // 1) ?ъ슜??紐⑸줉 議고쉶
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

        // 2~5) ?곌? ?곗씠??議고쉶 (湲곗〈 濡쒖쭅 ?좎?)
        Map<Long, OralCheck> latestOralChecks = getLatestOralChecks(userIds);
        Map<Long, List<String>> serviceNames = getUserServices(userIds);

        // 6) DTO 由ъ뒪??議곕┰ (AdminUserDto.Info ?앹꽦???ъ슜)
        List<AdminUserDto.Info> result = users.stream()
                .map(u -> new AdminUserDto.Info( //?앹꽦???몄텧 蹂寃?
                        u.getUserId(),
                        u.getUserLoginIdentifier(),
                        u.getUserName(),
                        u.getUserGender(),

                        // oralCheckResultTotalType
                        latestOralChecks.containsKey(u.getUserId())
                                ? latestOralChecks.get(u.getUserId()).getOralCheckResultTotalType()
                                : null,

                        // oralCheckDate
                        latestOralChecks.containsKey(u.getUserId())
                                ? latestOralChecks.get(u.getUserId()).getCreated()
                                : null,

                        u.getIsVerify(),

                        // serviceNames (List -> String 蹂?섑빐???섍?, DTO ?대??먯꽌 ?ㅼ떆 List濡?蹂??
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
    public Page<AdminUserDto.Info> findAllByOrganization(AdminUserDto.SearchRequest request) { //???蹂寃?
        if (request.getOrganizationId() == null)
            throw new IllegalArgumentException("organizationId媛 ?꾩슂?⑸땲??");
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
        return userIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> DEFAULT_SERVICE_NAMES
                ));
    }


    /**
     * WHERE 議곌굔 ?앹꽦
     */
    private Predicate whereSearch(AdminUserDto.SearchRequest request) {

        BooleanBuilder builder = new BooleanBuilder();

        // 1. 寃?됱뼱
        if (StringUtils.isNotBlank(request.getKeyword())) {
            String keyword = request.getKeyword();
            builder.and(
                    user.userLoginIdentifier.contains(keyword)
                            .or(user.userName.contains(keyword))
            );
        }

        // 2. 援ш컯寃??寃곌낵
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

        // 3. 臾몄쭊???좏삎
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

        // 4. ?깅퀎
        if (request.getGender() != null) {
            builder.and(user.userGender.eq(request.getGender()));
        }

        // 5. ?몄쬆 ?щ?
        if (request.getIsVerify() != null) {
            builder.and(user.isVerify.eq(request.getIsVerify()));
        }

        // 6. ?먮룞 湲곌컙
        if (request.getDatePeriodType() != null) {
            builder.and(whereAllDatePeriodAuto(request.getDatePeriodType()));
        }

        // 7. ?좎쭨 踰붿쐞
        builder.and(whereDateRange(request.getStartDate(), request.getEndDate()));

        return builder;
    }


    /**
     * ?쒖옉??/ 醫낅즺??(Date ???湲곗?)
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

                // 醫낅즺???ы븿(+1)
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
     * ?먮룞 湲곌컙 (?ㅻ뒛 / 1二?/ 1??/ 3??/ 1??
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
     * 媛?낆옄 ???듦퀎 (?섏젙??
     */
    @Override
    public AdminStatisticDto.SignUpCount userSignUpCount(AdminStatisticDto.SearchRequest request) { // ???蹂寃?

        return queryFactory
                .select(Projections.constructor(AdminStatisticDto.SignUpCount.class, //?앹꽦??蹂寃?
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
                        // request.getDatePeriodType() ???듦퀎??寃??議곌굔 ?ъ슜
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
     * 湲곌?蹂??듦퀎
     */
    @Override
    public List<SuperAdminStatisticDto.OrgUserStats> getAllOrganizationUserStats() { //由ы꽩 ???蹂寃?

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        Date oneMonthAgoDate = Date.from(oneMonthAgo.atZone(ZoneId.systemDefault()).toInstant());

        return queryFactory
                .select(Projections.constructor(
                        SuperAdminStatisticDto.OrgUserStats.class, //?寃??대옒??蹂寃?
                        organization.organizationId,
                        organization.organizationName,
                        user.countDistinct(),

                        // ?⑥꽦
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.M)).then(1L)
                                .otherwise(0L).sum(),

                        // ?ъ꽦
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.W)).then(1L)
                                .otherwise(0L).sum(),

                        // ?좉퇋 媛??
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
