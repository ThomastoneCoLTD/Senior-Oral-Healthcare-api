package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.admin.dto.superAdmin.SuperAdminUserStatisticResponse;
import com.kaii.dentix.domain.appService.domain.QAppService;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserSignUpCountDto;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.oralCheck.domain.QOralCheck;
import com.kaii.dentix.domain.oralStatus.domain.QOralStatus;
import com.kaii.dentix.domain.organization.domain.QOrganization;
import com.kaii.dentix.domain.questionnaire.domain.QQuestionnaire;
import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.domain.QUser;
import com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus;
import com.kaii.dentix.domain.userToAppService.domain.QUserToAppService;
import com.kaii.dentix.global.common.dto.PagingRequest;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.querydsl.core.BooleanBuilder;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class AdminUserRepositoryImpl implements AdminUserCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final QOralStatus oralStatus = QOralStatus.oralStatus;
    private final QUser user = QUser.user;
    private final QOralCheck oralCheck = QOralCheck.oralCheck;
    private final QUserOralStatus userOralStatus = QUserOralStatus.userOralStatus;
    private final QQuestionnaire questionnaire = QQuestionnaire.questionnaire;
    private final QAppService appService = QAppService.appService;
    private final QUserToAppService userToAppService = QUserToAppService.userToAppService;
    private final QOrganization organization = QOrganization.organization;

    /**
     * ✅ 사용자 목록 조회 (슈퍼관리자 또는 기관관리자 모두 사용 가능)
     */
    @Override
    public Page<AdminUserInfoDto> findAll(AdminUserListRequest request) {

        Pageable paging = new PagingRequest(request.getPage(), request.getSize()).of();

        // ✅ BooleanBuilder로 조건 누적
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(whereSearch(request));

        // ✅ organizationId 필터 추가 (관리자별 기관 제한)
        if (request.getOrganizationId() != null) {
            builder.and(user.organization.organizationId.eq(request.getOrganizationId()));
        }

        List<AdminUserInfoDto> result = queryFactory
                .select(Projections.constructor(AdminUserInfoDto.class,
                        user.userId,
                        user.userLoginIdentifier,
                        user.userName,
                        user.userGender,
                        Expressions.stringTemplate("group_concat(DISTINCT {0})", oralStatus.oralStatusTitle),
                        questionnaire.created.as("questionnaireDate"),
                        oralCheck.oralCheckResultTotalType,
                        oralCheck.created.as("oralCheckDate"),
                        user.isVerify,
                        Expressions.stringTemplate("group_concat(DISTINCT {0})", appService.name)
                ))
                .from(user)
                .leftJoin(user.organization, organization)
                .leftJoin(userToAppService).on(userToAppService.user.eq(user))
                .leftJoin(userToAppService.appService, appService)
                .leftJoin(questionnaire).on(questionnaire.userId.eq(user.userId)
                        .and(questionnaire.created.eq(JPAExpressions.select(questionnaire.created.max())
                                .from(questionnaire)
                                .where(questionnaire.userId.eq(user.userId))
                        )))
                .leftJoin(userOralStatus).on(questionnaire.questionnaireId.eq(userOralStatus.questionnaire.questionnaireId))
                .leftJoin(userOralStatus.oralStatus, oralStatus)
                .leftJoin(oralCheck).on(user.userId.eq(oralCheck.userId)
                        .and(oralCheck.created.eq(JPAExpressions.select(oralCheck.created.max())
                                .from(oralCheck)
                                .where(oralCheck.userId.eq(user.userId))
                        )))
                .where(builder)
                .groupBy(user.userId, questionnaire.questionnaireId, oralCheck.oralCheckId)
                .orderBy(user.created.desc())
                .offset(paging.getOffset())
                .limit(paging.getPageSize())
                .fetch();

        Long totalCount = Optional.ofNullable(queryFactory
                .select(user.countDistinct())
                .from(user)
                .leftJoin(user.organization, organization)
                .where(builder)
                .fetchOne()).orElse(0L);

        return new PageImpl<>(result, paging, totalCount);
    }

    /**
     * ✅ 기관별 사용자 목록 조회 (일반 관리자용)
     */
    @Override
    public Page<AdminUserInfoDto> findAllByOrganization(AdminUserListRequest request) {
        if (request.getOrganizationId() == null) {
            throw new IllegalArgumentException("organizationId가 필요합니다.");
        }
        // 내부적으로 organizationId 조건이 이미 적용되므로 findAll() 재사용
        return findAll(request);
    }

    /**
     * 검색 필터링
     */
    private Predicate whereSearch(AdminUserListRequest request) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (StringUtils.isNotBlank(request.getUserIdentifierOrName())) {
            booleanBuilder.or(user.userLoginIdentifier.contains(request.getUserIdentifierOrName())
                    .or(user.userName.contains(request.getUserIdentifierOrName())));
        }

        booleanBuilder.and(whereOralCheckResult(request.getOralCheckResultTotalType()));

        if (request.getOralStatus() != null) {
            booleanBuilder.and(userOralStatus.oralStatus.oralStatusType.eq(request.getOralStatus()));
        }

        if (request.getUserGender() != null) {
            booleanBuilder.and(user.userGender.eq(request.getUserGender()));
        }

        if (request.getIsVerify() != null) {
            booleanBuilder.and(user.isVerify.eq(request.getIsVerify()));
        }

        booleanBuilder.and(whereAllDatePeriod(request.getAllDatePeriod()));
        booleanBuilder.and(whereStartDate(request.getStartDate()));
        booleanBuilder.and(whereEndDate(request.getEndDate()));

        return booleanBuilder;
    }

    private BooleanExpression whereOralCheckResult(OralCheckResultType oralCheckResultTotalType) {
        return oralCheckResultTotalType == null ? null : oralCheck.oralCheckResultTotalType.eq(oralCheckResultTotalType);
    }

    private BooleanExpression whereAllDatePeriod(DatePeriodType type) {
        if (type != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 1);

            switch (type) {
                case TODAY -> cal.add(Calendar.DATE, -1);
                case WEEK1 -> cal.add(Calendar.DATE, -7);
                case MONTH1 -> cal.add(Calendar.MONTH, -1);
                case MONTH3 -> cal.add(Calendar.MONTH, -3);
                case YEAR1 -> cal.add(Calendar.YEAR, -1);
                case ALL -> { return null; }
            }

            Date startDate = cal.getTime();

            return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").goe(DateFormatUtil.dateToString("yyyy-MM-dd", startDate))
                    .or(Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").goe(DateFormatUtil.dateToString("yyyy-MM-dd", startDate)));
        }
        return null;
    }

    private BooleanExpression whereStartDate(String date) {
        return StringUtils.isNotBlank(date)
                ? Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").goe(date)
                .or(Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").goe(date))
                : null;
    }

    private BooleanExpression whereEndDate(String date) {
        return StringUtils.isNotBlank(date)
                ? Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").loe(date)
                .or(Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").loe(date))
                : null;
    }

    /**
     * 통계 - 전체 남녀 가입률
     */
    @Override
    public AdminUserSignUpCountDto userSignUpCount(AdminStatisticRequest request) {
        return queryFactory.select(Projections.constructor(AdminUserSignUpCountDto.class,
                        Wildcard.count.longValue(),
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.M))
                                .then(1L).otherwise(0L).sum(),
                        new CaseBuilder()
                                .when(user.userGender.eq(GenderType.W))
                                .then(1L).otherwise(0L).sum()))
                .from(user)
                .where(whereUserEndDate(request.getEndDate()))
                .fetchOne();
    }

    private BooleanExpression whereUserEndDate(String endDate) {
        Date date;
        try {
            date = DateFormatUtil.stringToDate("yyyy-MM-dd", endDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, 1);
        } catch (Exception e) {
            return null;
        }
        return user.created.lt(date);
    }
    @Override
    public List<SuperAdminUserStatisticResponse> getAllOrganizationUserStats() {
        QUser user = QUser.user;
        QOrganization org = QOrganization.organization;
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);
        Date oneMonthAgoDate = Date.from(oneMonthAgo.atZone(ZoneId.systemDefault()).toInstant());

        return queryFactory
                .select(
                        Projections.constructor(
                                SuperAdminUserStatisticResponse.class,
                                org.organizationId,
                                org.organizationName,
                                user.countDistinct(), // total users
                                user.userGender
                                        .when(GenderType.M).then(1L)
                                        .otherwise(0L).sum(), // male
                                user.userGender
                                        .when(com.kaii.dentix.domain.type.GenderType.W).then(1L)
                                        .otherwise(0L).sum(), // female
                                new CaseBuilder()
                .when(user.created.gt(oneMonthAgoDate)) // ✅ 비교식은 CaseBuilder 안에서만 사용 가능
                .then(1L)
                .otherwise(0L)
                .sum()  // new users (last 30 days)
                        )
                )
                .from(user)
                .join(user.organization, org)
                .groupBy(org.organizationId, org.organizationName)
                .orderBy(org.organizationId.asc())
                .fetch();
    }
}
