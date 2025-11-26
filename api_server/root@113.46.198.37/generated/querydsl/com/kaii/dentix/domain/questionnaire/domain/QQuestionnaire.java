package com.kaii.dentix.domain.questionnaire.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQuestionnaire is a Querydsl query type for Questionnaire
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQuestionnaire extends EntityPathBase<Questionnaire> {

    private static final long serialVersionUID = -1374021247L;

    public static final QQuestionnaire questionnaire = new QQuestionnaire("questionnaire");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final StringPath form = createString("form");

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final NumberPath<Long> questionnaireId = createNumber("questionnaireId", Long.class);

    public final StringPath questionnaireVersion = createString("questionnaireVersion");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final ListPath<com.kaii.dentix.domain.userOralStatus.domain.UserOralStatus, com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus> userOralStatusList = this.<com.kaii.dentix.domain.userOralStatus.domain.UserOralStatus, com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus>createList("userOralStatusList", com.kaii.dentix.domain.userOralStatus.domain.UserOralStatus.class, com.kaii.dentix.domain.userOralStatus.domain.QUserOralStatus.class, PathInits.DIRECT2);

    public QQuestionnaire(String variable) {
        super(Questionnaire.class, forVariable(variable));
    }

    public QQuestionnaire(Path<? extends Questionnaire> path) {
        super(path.getType(), path.getMetadata());
    }

    public QQuestionnaire(PathMetadata metadata) {
        super(Questionnaire.class, metadata);
    }

}

