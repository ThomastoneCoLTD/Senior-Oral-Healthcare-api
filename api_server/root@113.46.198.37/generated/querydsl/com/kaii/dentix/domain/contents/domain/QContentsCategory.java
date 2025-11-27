package com.kaii.dentix.domain.contents.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QContentsCategory is a Querydsl query type for ContentsCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContentsCategory extends EntityPathBase<ContentsCategory> {

    private static final long serialVersionUID = 343665387L;

    public static final QContentsCategory contentsCategory = new QContentsCategory("contentsCategory");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final StringPath contentsCategoryColor = createString("contentsCategoryColor");

    public final NumberPath<Integer> contentsCategoryId = createNumber("contentsCategoryId", Integer.class);

    public final StringPath contentsCategoryName = createString("contentsCategoryName");

    public final NumberPath<Integer> contentsCategorySort = createNumber("contentsCategorySort", Integer.class);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public QContentsCategory(String variable) {
        super(ContentsCategory.class, forVariable(variable));
    }

    public QContentsCategory(Path<? extends ContentsCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QContentsCategory(PathMetadata metadata) {
        super(ContentsCategory.class, metadata);
    }

}

