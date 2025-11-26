package com.kaii.dentix.domain.contents.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContentsToCategory is a Querydsl query type for ContentsToCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContentsToCategory extends EntityPathBase<ContentsToCategory> {

    private static final long serialVersionUID = -1716131002L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContentsToCategory contentsToCategory = new QContentsToCategory("contentsToCategory");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final QContents contents;

    public final NumberPath<Integer> contentsCategoryId = createNumber("contentsCategoryId", Integer.class);

    public final NumberPath<Long> contentsToCategoryId = createNumber("contentsToCategoryId", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public QContentsToCategory(String variable) {
        this(ContentsToCategory.class, forVariable(variable), INITS);
    }

    public QContentsToCategory(Path<? extends ContentsToCategory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContentsToCategory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContentsToCategory(PathMetadata metadata, PathInits inits) {
        this(ContentsToCategory.class, metadata, inits);
    }

    public QContentsToCategory(Class<? extends ContentsToCategory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contents = inits.isInitialized("contents") ? new QContents(forProperty("contents")) : null;
    }

}

