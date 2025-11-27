package com.kaii.dentix.domain.contents.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContentsCard is a Querydsl query type for ContentsCard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContentsCard extends EntityPathBase<ContentsCard> {

    private static final long serialVersionUID = -1357011075L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContentsCard contentsCard = new QContentsCard("contentsCard");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final QContents contents;

    public final NumberPath<Integer> contentsCardId = createNumber("contentsCardId", Integer.class);

    public final NumberPath<Integer> contentsCardNumber = createNumber("contentsCardNumber", Integer.class);

    public final StringPath contentsCardPath = createString("contentsCardPath");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public QContentsCard(String variable) {
        this(ContentsCard.class, forVariable(variable), INITS);
    }

    public QContentsCard(Path<? extends ContentsCard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContentsCard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContentsCard(PathMetadata metadata, PathInits inits) {
        this(ContentsCard.class, metadata, inits);
    }

    public QContentsCard(Class<? extends ContentsCard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contents = inits.isInitialized("contents") ? new QContents(forProperty("contents")) : null;
    }

}

