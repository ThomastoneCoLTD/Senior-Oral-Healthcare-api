package com.kaii.dentix.domain.contents.dao;

import com.kaii.dentix.domain.contents.domain.QContents;
import com.kaii.dentix.domain.contents.domain.QContentsToCategory;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.oralStatusToContents.domain.QOralStatusToContents;
import com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

@Repository
@RequiredArgsConstructor
public class ContentsCustomRepositoryImpl implements ContentsCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;
    private final QUserOralStatus userOralStatus = QUserOralStatus.userOralStatus;
    private final QOralStatusToContents oralStatusToContents = QOralStatusToContents.oralStatusToContents;
    private final QContents contents = QContents.contents;
    private final QContentsToCategory contentsToCategory = QContentsToCategory.contentsToCategory;

    /**
     * 구강교육 조회
     */
    @Override
    public List<ContentsDto> getContents() {

        // transform 사용 시 JPQLTemplates.DEFAULT 필요
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
            .from(contents)
            .leftJoin(contentsToCategory).on(contentsToCategory.contents.eq(contents))
            .orderBy(contents.contentsSort.asc())
            .distinct()
            .transform(groupBy(contents.contentsId).list(Projections.constructor(ContentsDto.class,
                contents.contentsId, contents.contentsTitle, contents.contentsSort, contents.contentsType, contents.contentsTypeColor, contents.contentsThumbnail, contents.contentsPath, list(contentsToCategory.contentsCategoryId)
            )));
    }

    /**
     * 맞춤형 구강교육 아이디 목록 조회
     */
    @Override
    public List<Long> getCustomizedContentsIdList(Long questionnaireId) {
        return queryFactory
            .selectDistinct(contents.contentsId)
            .from(contents)
            .join(oralStatusToContents).on(oralStatusToContents.contents.contentsId.eq(contents.contentsId))
            .join(userOralStatus).on(userOralStatus.oralStatus.oralStatusType.eq(oralStatusToContents.oralStatus.oralStatusType))
            .where(userOralStatus.questionnaire.questionnaireId.eq(questionnaireId))
            .fetch();
    }

    /**
     * 맞춤형 구강교육 조회
     */
    @Override
    public List<ContentsDto> getCustomizedContents(Long questionnaireId) {

        // transform 사용 시 JPQLTemplates.DEFAULT 필요
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
            .from(contents)
            .leftJoin(contentsToCategory).on(contentsToCategory.contents.eq(contents))
            .join(oralStatusToContents).on(oralStatusToContents.contents.contentsId.eq(contents.contentsId))
            .join(userOralStatus).on(userOralStatus.oralStatus.oralStatusType.eq(oralStatusToContents.oralStatus.oralStatusType))
            .where(userOralStatus.questionnaire.questionnaireId.eq(questionnaireId))
            .orderBy(contents.contentsSort.asc())
            .distinct()
            .transform(groupBy(contents.contentsId).list(Projections.constructor(ContentsDto.class,
                contents.contentsId, contents.contentsTitle, contents.contentsSort, contents.contentsType, contents.contentsTypeColor, contents.contentsThumbnail, contents.contentsPath, list(contentsToCategory.contentsCategoryId)
            )));
    }
}

