package com.kaii.dentix.domain.userOralStatus.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserOralStatus is a Querydsl query type for UserOralStatus
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserOralStatus extends EntityPathBase<UserOralStatus> {

    private static final long serialVersionUID = -1503388689L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserOralStatus userOralStatus = new QUserOralStatus("userOralStatus");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.oralStatus.domain.QOralStatus oralStatus;

    public final com.kaii.dentix.domain.questionnaire.domain.QQuestionnaire questionnaire;

    public final NumberPath<Long> userOralStatusId = createNumber("userOralStatusId", Long.class);

    public QUserOralStatus(String variable) {
        this(UserOralStatus.class, forVariable(variable), INITS);
    }

    public QUserOralStatus(Path<? extends UserOralStatus> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserOralStatus(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserOralStatus(PathMetadata metadata, PathInits inits) {
        this(UserOralStatus.class, metadata, inits);
    }

    public QUserOralStatus(Class<? extends UserOralStatus> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.oralStatus = inits.isInitialized("oralStatus") ? new com.kaii.dentix.domain.oralStatus.domain.QOralStatus(forProperty("oralStatus")) : null;
        this.questionnaire = inits.isInitialized("questionnaire") ? new com.kaii.dentix.domain.questionnaire.domain.QQuestionnaire(forProperty("questionnaire")) : null;
    }

}

