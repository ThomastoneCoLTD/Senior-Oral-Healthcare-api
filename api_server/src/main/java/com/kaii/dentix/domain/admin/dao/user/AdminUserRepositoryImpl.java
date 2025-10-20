package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.appService.domain.QAppService;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserSignUpCountDto;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.oralCheck.domain.QOralCheck;
import com.kaii.dentix.domain.oralStatus.domain.QOralStatus;
import com.kaii.dentix.domain.questionnaire.domain.QQuestionnaire;
import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.domain.QUser;
import com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus;
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
//import static com.kaii.dentix.domain.user.domain.Q
import static com.kaii.dentix.domain.appService.domain.QAppService.appService;
import static com.kaii.dentix.domain.userToAppService.domain.QUserToAppService.userToAppService;
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

    private final QAppService service = QAppService.appService;
    /**
     *  사용자 목록 조회
     */
    @Override
    public Page<AdminUserInfoDto> findAll(AdminUserListRequest request){

        Pageable paging = new PagingRequest(request.getPage(), request.getSize()).of();

        List<AdminUserInfoDto> result = queryFactory
                .select(Projections.constructor(AdminUserInfoDto.class,
                        user.userId,
                        user.userLoginIdentifier,
                        user.userName,
                        user.userGender,
                        // ✅ oralStatusTitle로 group_concat
                        Expressions.stringTemplate("group_concat(DISTINCT {0})", oralStatus.oralStatusTitle),
                        questionnaire.created.as("questionnaireDate"),
                        oralCheck.oralCheckResultTotalType,
                        oralCheck.created.as("oralCheckDate"),
                        user.isVerify,
                        // ✅ 서비스명도 group_concat으로
                        Expressions.stringTemplate("group_concat(DISTINCT {0})", service.name)
                ))
                .from(user)
                .leftJoin(userToAppService).on(userToAppService.user.eq(user))
                .leftJoin(userToAppService.appService, appService)
                .leftJoin(questionnaire).on(questionnaire.userId.eq(user.userId)
                        .and(questionnaire.created.eq(JPAExpressions.select(questionnaire.created.max())
                                .from(questionnaire)
                                .where(questionnaire.userId.eq(user.userId))
                        )))
                .leftJoin(userOralStatus).on(questionnaire.questionnaireId.eq(userOralStatus.questionnaire.questionnaireId))
                // ✅ OralStatus 조인 추가
                .leftJoin(userOralStatus.oralStatus, oralStatus)
                .leftJoin(oralCheck).on(user.userId.eq(oralCheck.userId)
                        .and(oralCheck.created.eq(JPAExpressions.select(oralCheck.created.max())
                                .from(oralCheck)
                                .where(oralCheck.userId.eq(user.userId))
                        )))
                .where(whereSearch(request))
                .groupBy(user.userId, questionnaire.questionnaireId, oralCheck.oralCheckId)
                .orderBy(user.created.desc())
                .offset(paging.getOffset())
                .limit(paging.getPageSize())
                .fetch();

        Long totalCount = Optional.ofNullable(queryFactory
                .select(user.countDistinct())
                .from(user)
                .fetchOne()).orElse(0L);

        return new PageImpl<>(result, paging, totalCount);

    }

    /**
     *  검색 필터링
     */
    private Predicate whereSearch(AdminUserListRequest request) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        // 검색어 (아이디 혹은 이름)
        if (StringUtils.isNotBlank(request.getUserIdentifierOrName())) {
            booleanBuilder.or(user.userLoginIdentifier.contains(request.getUserIdentifierOrName())
                    .or(user.userName.contains(request.getUserIdentifierOrName())));
        }

        // 구강 상태
        booleanBuilder.and(whereOralCheckResult(request.getOralCheckResultTotalType()));
// 문진표 유형 (oralStatusType)
        if (request.getOralStatus() != null) {
            booleanBuilder.and(userOralStatus.oralStatus.oralStatusType.eq(request.getOralStatus()));
        }

        // 성별
        if (request.getUserGender() != null) {
            booleanBuilder.and(user.userGender.eq(request.getUserGender()));
        }

        // 사용자 인증 여부
        if (request.getIsVerify() != null) {
            booleanBuilder.and(user.isVerify.eq(request.getIsVerify()));
        }

        // 기간 설정
        booleanBuilder.and(whereAllDatePeriod(request.getAllDatePeriod()));
        booleanBuilder.and(whereStartDate(request.getStartDate()));
        booleanBuilder.and(whereEndDate(request.getEndDate()));

        return booleanBuilder;
    }

    /**
     *  구강 상태 필터링
     */
    private BooleanExpression whereOralCheckResult(OralCheckResultType oralCheckResultTotalType){
        return oralCheckResultTotalType == null ? null : oralCheck.oralCheckResultTotalType.eq(oralCheckResultTotalType);
    }

    /**
     *  기간 설정 타입 필터링 (구강 촬영일 or 문진표 검사일)
     */
    private BooleanExpression whereAllDatePeriod(DatePeriodType type){

        if (type != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 1);

            // 내일 00시 00분 기준으로 시작
            switch (type) {
                case TODAY: cal.add(Calendar.DATE, -1); break;
                case WEEK1: cal.add(Calendar.DATE, -7); break;
                case MONTH1: cal.add(Calendar.MONTH, -1); break;
                case MONTH3: cal.add(Calendar.MONTH, -3); break;
                case YEAR1: cal.add(Calendar.YEAR, -1); break;
                case ALL: return null;
            }

            Date startDate = cal.getTime();

            return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").goe(DateFormatUtil.dateToString("yyyy-MM-dd", startDate)).or
                    (Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").goe(DateFormatUtil.dateToString("yyyy-MM-dd", startDate)));
        }
        return null;
    }

    /**
     *  기간 설정 '시작일' 필터링
     */
    private BooleanExpression whereStartDate(String date){
        return StringUtils.isNotBlank(date) ?
                Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").goe(date).or
                        (Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").goe(date))
                : null;
    }

    /**
     *  기간 설정 '종료일' 필터링
     */
    private BooleanExpression whereEndDate(String date){
        return StringUtils.isNotBlank(date) ?
                Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").loe(date).or
                        (Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").loe(date))
                : null;
    }

    /**
     *  통계 - 전체 남녀 가입률
     */
    @Override
    public AdminUserSignUpCountDto userSignUpCount(AdminStatisticRequest request){
        return queryFactory.select(Projections.constructor(AdminUserSignUpCountDto.class,
                Wildcard.count.longValue(), // 전체 가입자 수
                new CaseBuilder() // 남성 가입자 수
                        .when(user.userGender.eq(GenderType.M))
                        .then(1L).otherwise(0L).sum(),
                new CaseBuilder() // 여성 가입자 수
                        .when(user.userGender.eq(GenderType.W))
                        .then(1L).otherwise(0L).sum()))
                .from(user)
                .where(whereUserEndDate(request.getEndDate()))
                .fetchOne();
    }

    /**
     *  사용자 가입일 '종료일' 필터링
     */
    private BooleanExpression whereUserEndDate(String endDate){
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

}
