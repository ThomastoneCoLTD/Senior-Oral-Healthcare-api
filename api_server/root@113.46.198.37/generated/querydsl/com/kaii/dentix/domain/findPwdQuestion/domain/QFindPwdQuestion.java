package com.kaii.dentix.domain.findPwdQuestion.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QFindPwdQuestion is a Querydsl query type for FindPwdQuestion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFindPwdQuestion extends EntityPathBase<FindPwdQuestion> {

    private static final long serialVersionUID = -632589599L;

    public static final QFindPwdQuestion findPwdQuestion = new QFindPwdQuestion("findPwdQuestion");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final NumberPath<Long> findPwdQuestionId = createNumber("findPwdQuestionId", Long.class);

    public final NumberPath<Long> findPwdQuestionSort = createNumber("findPwdQuestionSort", Long.class);

    public final StringPath findPwdQuestionTitle = createString("findPwdQuestionTitle");

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public QFindPwdQuestion(String variable) {
        super(FindPwdQuestion.class, forVariable(variable));
    }

    public QFindPwdQuestion(Path<? extends FindPwdQuestion> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFindPwdQuestion(PathMetadata metadata) {
        super(FindPwdQuestion.class, metadata);
    }

}

