package com.kaii.dentix.domain.contents.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContents is a Querydsl query type for Contents
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContents extends EntityPathBase<Contents> {

    private static final long serialVersionUID = -568858931L;

    public static final QContents contents = new QContents("contents");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final ListPath<ContentsCard, QContentsCard> contentsCards = this.<ContentsCard, QContentsCard>createList("contentsCards", ContentsCard.class, QContentsCard.class, PathInits.DIRECT2);

    public final NumberPath<Long> contentsId = createNumber("contentsId", Long.class);

    public final StringPath contentsPath = createString("contentsPath");

    public final NumberPath<Integer> contentsSort = createNumber("contentsSort", Integer.class);

    public final StringPath contentsThumbnail = createString("contentsThumbnail");

    public final StringPath contentsTitle = createString("contentsTitle");

    public final ListPath<ContentsToCategory, QContentsToCategory> contentsToCategories = this.<ContentsToCategory, QContentsToCategory>createList("contentsToCategories", ContentsToCategory.class, QContentsToCategory.class, PathInits.DIRECT2);

    public final EnumPath<com.kaii.dentix.domain.type.ContentsType> contentsType = createEnum("contentsType", com.kaii.dentix.domain.type.ContentsType.class);

    public final StringPath contentsTypeColor = createString("contentsTypeColor");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final DateTimePath<java.util.Date> deleted = createDateTime("deleted", java.util.Date.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public QContents(String variable) {
        super(Contents.class, forVariable(variable));
    }

    public QContents(Path<? extends Contents> path) {
        super(path.getType(), path.getMetadata());
    }

    public QContents(PathMetadata metadata) {
        super(Contents.class, metadata);
    }

}

