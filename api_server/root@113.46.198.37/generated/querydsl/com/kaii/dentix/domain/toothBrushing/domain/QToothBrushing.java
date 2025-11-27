package com.kaii.dentix.domain.toothBrushing.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QToothBrushing is a Querydsl query type for ToothBrushing
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QToothBrushing extends EntityPathBase<ToothBrushing> {

    private static final long serialVersionUID = -1149921375L;

    public static final QToothBrushing toothBrushing = new QToothBrushing("toothBrushing");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final NumberPath<Long> toothBrushingId = createNumber("toothBrushingId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QToothBrushing(String variable) {
        super(ToothBrushing.class, forVariable(variable));
    }

    public QToothBrushing(Path<? extends ToothBrushing> path) {
        super(path.getType(), path.getMetadata());
    }

    public QToothBrushing(PathMetadata metadata) {
        super(ToothBrushing.class, metadata);
    }

}

