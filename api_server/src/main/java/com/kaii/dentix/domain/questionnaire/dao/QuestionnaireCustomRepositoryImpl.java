package com.kaii.dentix.domain.questionnaire.dao;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.statistic.AllQuestionnaireCount;
import com.kaii.dentix.domain.admin.dto.statistic.QuestionnaireStatisticDto;
import com.kaii.dentix.domain.oralStatus.domain.QOralStatus;
import com.kaii.dentix.domain.questionnaire.domain.QQuestionnaire;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireAndStatusDto;
import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.user.domain.QUser;
import com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class QuestionnaireCustomRepositoryImpl implements QuestionnaireCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final QQuestionnaire questionnaire = QQuestionnaire.questionnaire;
    private final QOralStatus oralStatus = QOralStatus.oralStatus;
    private final QUserOralStatus userOralStatus = QUserOralStatus.userOralStatus;

    private final QUser user = QUser.user;

    /**
     * 최근 문진표 및 해당 문진표의 높은 우선순위 구강 상태 조회
     */
    @Override
    public QuestionnaireAndStatusDto getLatestQuestionnaireAndHigherStatus(Long userId) {

        return queryFactory
                .select(Projections.constructor(QuestionnaireAndStatusDto.class,
                        questionnaire.questionnaireId, questionnaire.created, oralStatus.oralStatusType, oralStatus.oralStatusTitle
                ))
                .from(questionnaire)
                .join(userOralStatus).on(userOralStatus.questionnaire.questionnaireId.eq(questionnaire.questionnaireId))
                .join(oralStatus).on(oralStatus.oralStatusType.eq(userOralStatus.oralStatus.oralStatusType))
                .where(questionnaire.userId.eq(userId))
                .orderBy(questionnaire.created.desc(), oralStatus.oralStatusPriority.asc())
                .fetchFirst();
    }

    /**
     * 모든 문진표 리스트
     */
    @Override
    public List<QuestionnaireStatisticDto> questionnaireList(AdminStatisticDto.SearchRequest request) {
        return queryFactory.select(Projections.constructor(QuestionnaireStatisticDto.class,
                        user.userId, userOralStatus.oralStatus.oralStatusType
                ))
                .from(questionnaire)
                .join(user).on(questionnaire.userId.eq(user.userId))
                .join(userOralStatus).on(questionnaire.questionnaireId.eq(userOralStatus.questionnaire.questionnaireId))
                .where(
                        user.deleted.isNull(),
                        whereAllDatePeriod(request.getDatePeriodType()),
                        whereStartDate(request.getStartDate()),
                        whereEndDate(request.getEndDate())
                )
                .fetch();
    }

    /**
     * 전체 문진표 검진 횟수
     */
    @Override
    public int allQuestionnaireCount(AdminStatisticDto.SearchRequest  request) {
        return Objects.requireNonNull(queryFactory.select(Projections.constructor(AllQuestionnaireCount.class,
                        Wildcard.count.intValue().as("questionnaireAllCount")
                ))
                .from(questionnaire)
                .join(user).on(questionnaire.userId.eq(user.userId))
                .where(
                        user.deleted.isNull(),
                        whereAllDatePeriod(request.getDatePeriodType()),
                        whereStartDate(request.getStartDate()),
                        whereEndDate(request.getEndDate())
                )
                .fetchOne()).getAllQuestionnaireCount();
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

            return Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").goe(DateFormatUtil.dateToString("yyyy-MM-dd", startDate));
        }
        return null;
    }

    /**
     *  기간 설정 '시작일' 필터링
     */
    private BooleanExpression whereStartDate(String date){
        return StringUtils.isNotBlank(date) ?
                Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").goe(date) : null;
    }

    /**
     *  기간 설정 '종료일' 필터링
     */
    private BooleanExpression whereEndDate(String date){
        return StringUtils.isNotBlank(date) ?
                Expressions.stringTemplate("DATE_FORMAT({0}, {1})", questionnaire.created, "%Y-%m-%d").loe(date) : null;
    }

}

