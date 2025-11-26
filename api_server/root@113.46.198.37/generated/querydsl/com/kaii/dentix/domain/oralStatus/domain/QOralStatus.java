package com.kaii.dentix.domain.oralStatus.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QOralStatus is a Querydsl query type for OralStatus
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOralStatus extends EntityPathBase<OralStatus> {

    private static final long serialVersionUID = -938476327L;

    public static final QOralStatus oralStatus = new QOralStatus("oralStatus");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final StringPath oralStatusDescription = createString("oralStatusDescription");

    public final StringPath oralStatusDescriptionEn = createString("oralStatusDescriptionEn");

    public final StringPath oralStatusDescriptionVi = createString("oralStatusDescriptionVi");

    public final NumberPath<Integer> oralStatusPriority = createNumber("oralStatusPriority", Integer.class);

    public final StringPath oralStatusSubDescription = createString("oralStatusSubDescription");

    public final StringPath oralStatusSubDescriptionEn = createString("oralStatusSubDescriptionEn");

    public final StringPath oralStatusSubDescriptionVi = createString("oralStatusSubDescriptionVi");

    public final StringPath oralStatusTitle = createString("oralStatusTitle");

    public final StringPath oralStatusTitleEn = createString("oralStatusTitleEn");

    public final StringPath oralStatusTitleVi = createString("oralStatusTitleVi");

    public final StringPath oralStatusType = createString("oralStatusType");

    public QOralStatus(String variable) {
        super(OralStatus.class, forVariable(variable));
    }

    public QOralStatus(Path<? extends OralStatus> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOralStatus(PathMetadata metadata) {
        super(OralStatus.class, metadata);
    }

}

