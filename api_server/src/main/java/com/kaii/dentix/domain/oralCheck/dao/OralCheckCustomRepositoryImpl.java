package com.kaii.dentix.domain.oralCheck.dao;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
import com.kaii.dentix.domain.oralCheck.domain.QOralCheck;
import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.domain.QUser;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;

@Repository
@RequiredArgsConstructor
public class OralCheckCustomRepositoryImpl implements OralCheckCustomRepository {

    private final JPAQueryFactory queryFactory;

    private final QUser user = QUser.user;

    private final QOralCheck oralCheck = QOralCheck.oralCheck;

    /**
     *  구강검진 결과 타입별 횟수
     */
    @Override
    public OralCheckResultTypeCount userOralCheckList(AdminStatisticDto.SearchRequest request){
        return queryFactory.select(Projections.constructor(OralCheckResultTypeCount.class,
                new CaseBuilder()
                        .when(oralCheck.oralCheckResultTotalType.eq(OralCheckResultType.HEALTHY))
                        .then(1)
                        .otherwise(0)
                        .sum(),
                new CaseBuilder()
                        .when(oralCheck.oralCheckResultTotalType.eq(OralCheckResultType.GOOD))
                        .then(1)
                        .otherwise(0)
                        .sum(),
                new CaseBuilder()
                        .when(oralCheck.oralCheckResultTotalType.eq(OralCheckResultType.ATTENTION))
                        .then(1)
                        .otherwise(0)
                        .sum(),
                new CaseBuilder()
                        .when(oralCheck.oralCheckResultTotalType.eq(OralCheckResultType.DANGER))
                        .then(1)
                        .otherwise(0)
                        .sum()
                ))
                .from(oralCheck)
                .join(user).on(oralCheck.user.userId.eq(user.userId))
                .where(
                        user.deleted.isNull(),
                        whereAllDatePeriod(request.getDatePeriodType()),
                        whereStartDate(request.getStartDate()),
                        whereEndDate(request.getEndDate())
                )
                .fetchOne();
    }

    /**
     *  구강검진을 한 총 사용자 수
     */
    @Override
    public Integer allUserOralCheckCount(AdminStatisticDto.SearchRequest request) { // 타입 변경

        return queryFactory.select(
                        user.userId.countDistinct().intValue()
                )
                .from(oralCheck)
                .join(user).on(oralCheck.user.userId.eq(user.userId))
                .where(
                        user.deleted.isNull(),
                        // 기관 ID 필터링이 필요하다면 추가 (AdminStatisticService 로직에 따라 필수일 수 있음)
                        // request.getOrganizationId() != null ? user.organization.organizationId.eq(request.getOrganizationId()) : null,

                        // 필드명 변경 반영 (getAllDatePeriod -> getDatePeriodType)
                        whereAllDatePeriod(request.getDatePeriodType()),
                        whereStartDate(request.getStartDate()),
                        whereEndDate(request.getEndDate())
                )
                .fetchOne();
    }

    /**
     *  기간 설정 타입 필터링
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

            return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").goe(DateFormatUtil.dateToString("yyyy-MM-dd", startDate));
        }
        return null;
    }

    /**
     *  기간 설정 '시작일' 필터링
     */
    private BooleanExpression whereStartDate(String date){
        return StringUtils.isNotBlank(date) ?
                Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").goe(date)
                : null;
    }

    /**
     *  기간 설정 '종료일' 필터링
     */
    private BooleanExpression whereEndDate(String date){
        return StringUtils.isNotBlank(date) ?
                Expressions.stringTemplate("DATE_FORMAT({0}, {1})", oralCheck.created, "%Y-%m-%d").loe(date) : null;
    }

}
