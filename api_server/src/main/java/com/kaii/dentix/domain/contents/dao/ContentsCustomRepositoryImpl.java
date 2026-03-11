package com.kaii.dentix.domain.contents.dao;

import com.kaii.dentix.domain.contents.domain.QContents;
import com.kaii.dentix.domain.contents.domain.QContentsToCategory;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.oralStatusAssignment.domain.QOralStatusAssignment;
import com.kaii.dentix.domain.oralStatusToContents.domain.QOralStatusToContents;
import com.kaii.dentix.domain.type.OralSectionType;
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
    private final QOralStatusAssignment oralStatusAssignment = QOralStatusAssignment.oralStatusAssignment;
    private final QOralStatusToContents oralStatusToContents = QOralStatusToContents.oralStatusToContents;
    private final QContents contents = QContents.contents;
    private final QContentsToCategory contentsToCategory = QContentsToCategory.contentsToCategory;

    /**
     * 구강교육 조회
     * (Return Type 변경: ContentsDto -> ContentsDto.Summary)
     */
    @Override
    public List<ContentsDto.Summary> getContents() {
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
                .from(contents)
                .leftJoin(contentsToCategory).on(contentsToCategory.contents.eq(contents))
                .orderBy(contents.contentsSort.asc())
                .distinct()
                .transform(groupBy(contents.contentsId).list(Projections.constructor(ContentsDto.Summary.class, //
                        contents.contentsId,
                        contents.contentsTitle,
                        contents.contentsSort,
                        contents.contentsType,
                        contents.contentsTypeColor,
                        contents.contentsThumbnail,
                        contents.contentsPath, // DTO의 videoURL 매핑
                        list(contentsToCategory.contentsCategoryId)
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
                .join(oralStatusAssignment).on(oralStatusAssignment.oralStatus.oralStatusType.eq(oralStatusToContents.oralStatus.oralStatusType))
                .where(oralStatusAssignment.questionnaire.questionnaireId.eq(questionnaireId))
                .fetch();
    }

    /**
     * 맞춤형 구강교육 조회
     */
    @Override
    public List<ContentsDto.Summary> getCustomizedContents(Long questionnaireId) {
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
                .from(contents)
                .leftJoin(contentsToCategory).on(contentsToCategory.contents.eq(contents))
                .join(oralStatusToContents).on(oralStatusToContents.contents.contentsId.eq(contents.contentsId))
                .join(oralStatusAssignment).on(oralStatusAssignment.oralStatus.oralStatusType.eq(oralStatusToContents.oralStatus.oralStatusType))
                .where(oralStatusAssignment.questionnaire.questionnaireId.eq(questionnaireId))
                .orderBy(contents.contentsSort.asc())
                .distinct()
                .transform(groupBy(contents.contentsId).list(Projections.constructor(ContentsDto.Summary.class,
                        contents.contentsId,
                        contents.contentsTitle,
                        contents.contentsSort,
                        contents.contentsType,
                        contents.contentsTypeColor,
                        contents.contentsThumbnail,
                        contents.contentsPath,
                        list(contentsToCategory.contentsCategoryId)
                )));
    }

    @Override
    public List<Long> getCustomizedContentsIdListByOralCheck(Long oralCheckId) {
        return queryFactory
                .selectDistinct(contents.contentsId)
                .from(contents)
                .join(oralStatusToContents).on(oralStatusToContents.contents.contentsId.eq(contents.contentsId))
                .join(oralStatusAssignment).on(oralStatusAssignment.oralStatus.oralStatusType.eq(oralStatusToContents.oralStatus.oralStatusType))
                .where(oralStatusAssignment.oralCheck.oralCheckId.eq(oralCheckId))
                .fetch();
    }

    @Override
    public List<ContentsDto.Summary> getCustomizedContents(OralSectionType sectionType, Long sectionId) {
        if (sectionType == null || sectionId == null) {
            return List.of();
        }

        return switch (sectionType) {
            case QUESTIONNAIRE -> getCustomizedContents(sectionId);
            case ORAL_CHECK -> getCustomizedContentsByOralCheck(sectionId);
            default -> List.of();
        };
    }

    private List<ContentsDto.Summary> getCustomizedContentsByOralCheck(Long oralCheckId) {
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager)
                .from(contents)
                .leftJoin(contentsToCategory).on(contentsToCategory.contents.eq(contents))
                .join(oralStatusToContents).on(oralStatusToContents.contents.contentsId.eq(contents.contentsId))
                .join(oralStatusAssignment).on(oralStatusAssignment.oralStatus.oralStatusType.eq(oralStatusToContents.oralStatus.oralStatusType))
                .where(oralStatusAssignment.oralCheck.oralCheckId.eq(oralCheckId))
                .orderBy(contents.contentsSort.asc())
                .distinct()
                .transform(groupBy(contents.contentsId).list(Projections.constructor(ContentsDto.Summary.class,
                        contents.contentsId,
                        contents.contentsTitle,
                        contents.contentsSort,
                        contents.contentsType,
                        contents.contentsTypeColor,
                        contents.contentsThumbnail,
                        contents.contentsPath,
                        list(contentsToCategory.contentsCategoryId)
                )));
    }
}
