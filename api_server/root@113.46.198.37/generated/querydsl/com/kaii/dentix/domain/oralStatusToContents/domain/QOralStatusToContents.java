package com.kaii.dentix.domain.oralStatusToContents.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOralStatusToContents is a Querydsl query type for OralStatusToContents
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOralStatusToContents extends EntityPathBase<OralStatusToContents> {

    private static final long serialVersionUID = 481574275L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOralStatusToContents oralStatusToContents = new QOralStatusToContents("oralStatusToContents");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final com.kaii.dentix.domain.contents.domain.QContents contents;

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.oralStatus.domain.QOralStatus oralStatus;

    public final NumberPath<Long> oralStatusToContentsId = createNumber("oralStatusToContentsId", Long.class);

    public QOralStatusToContents(String variable) {
        this(OralStatusToContents.class, forVariable(variable), INITS);
    }

    public QOralStatusToContents(Path<? extends OralStatusToContents> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOralStatusToContents(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOralStatusToContents(PathMetadata metadata, PathInits inits) {
        this(OralStatusToContents.class, metadata, inits);
    }

    public QOralStatusToContents(Class<? extends OralStatusToContents> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contents = inits.isInitialized("contents") ? new com.kaii.dentix.domain.contents.domain.QContents(forProperty("contents")) : null;
        this.oralStatus = inits.isInitialized("oralStatus") ? new com.kaii.dentix.domain.oralStatus.domain.QOralStatus(forProperty("oralStatus")) : null;
    }

}

