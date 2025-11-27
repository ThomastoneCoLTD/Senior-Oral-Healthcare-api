package com.kaii.dentix.domain.oralCheck.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOralCheck is a Querydsl query type for OralCheck
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOralCheck extends EntityPathBase<OralCheck> {

    private static final long serialVersionUID = -634813471L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOralCheck oralCheck = new QOralCheck("oralCheck");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final EnumPath<com.kaii.dentix.domain.type.oral.OralCheckAnalysisState> oralCheckAnalysisState = createEnum("oralCheckAnalysisState", com.kaii.dentix.domain.type.oral.OralCheckAnalysisState.class);

    public final NumberPath<Float> oralCheckDownLeftRange = createNumber("oralCheckDownLeftRange", Float.class);

    public final EnumPath<com.kaii.dentix.domain.type.oral.OralCheckResultType> oralCheckDownLeftScoreType = createEnum("oralCheckDownLeftScoreType", com.kaii.dentix.domain.type.oral.OralCheckResultType.class);

    public final NumberPath<Float> oralCheckDownRightRange = createNumber("oralCheckDownRightRange", Float.class);

    public final EnumPath<com.kaii.dentix.domain.type.oral.OralCheckResultType> oralCheckDownRightScoreType = createEnum("oralCheckDownRightScoreType", com.kaii.dentix.domain.type.oral.OralCheckResultType.class);

    public final NumberPath<Long> oralCheckId = createNumber("oralCheckId", Long.class);

    public final StringPath oralCheckPicturePath = createString("oralCheckPicturePath");

    public final StringPath oralCheckResultJsonData = createString("oralCheckResultJsonData");

    public final EnumPath<com.kaii.dentix.domain.type.oral.OralCheckResultType> oralCheckResultTotalType = createEnum("oralCheckResultTotalType", com.kaii.dentix.domain.type.oral.OralCheckResultType.class);

    public final NumberPath<Float> oralCheckTotalRange = createNumber("oralCheckTotalRange", Float.class);

    public final NumberPath<Float> oralCheckUpLeftRange = createNumber("oralCheckUpLeftRange", Float.class);

    public final EnumPath<com.kaii.dentix.domain.type.oral.OralCheckResultType> oralCheckUpLeftScoreType = createEnum("oralCheckUpLeftScoreType", com.kaii.dentix.domain.type.oral.OralCheckResultType.class);

    public final NumberPath<Float> oralCheckUpRightRange = createNumber("oralCheckUpRightRange", Float.class);

    public final EnumPath<com.kaii.dentix.domain.type.oral.OralCheckResultType> oralCheckUpRightScoreType = createEnum("oralCheckUpRightScoreType", com.kaii.dentix.domain.type.oral.OralCheckResultType.class);

    public final com.kaii.dentix.domain.user.domain.QUser user;

    public QOralCheck(String variable) {
        this(OralCheck.class, forVariable(variable), INITS);
    }

    public QOralCheck(Path<? extends OralCheck> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOralCheck(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOralCheck(PathMetadata metadata, PathInits inits) {
        this(OralCheck.class, metadata, inits);
    }

    public QOralCheck(Class<? extends OralCheck> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.kaii.dentix.domain.user.domain.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

