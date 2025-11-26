package com.kaii.dentix.domain.serviceAgreement.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QServiceAgreement is a Querydsl query type for ServiceAgreement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QServiceAgreement extends EntityPathBase<ServiceAgreement> {

    private static final long serialVersionUID = 1989479747L;

    public static final QServiceAgreement serviceAgreement = new QServiceAgreement("serviceAgreement");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final EnumPath<com.kaii.dentix.domain.type.YnType> isServiceAgreeRequired = createEnum("isServiceAgreeRequired", com.kaii.dentix.domain.type.YnType.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final NumberPath<Long> serviceAgreeId = createNumber("serviceAgreeId", Long.class);

    public final StringPath serviceAgreeMenuName = createString("serviceAgreeMenuName");

    public final StringPath serviceAgreeName = createString("serviceAgreeName");

    public final StringPath serviceAgreePath = createString("serviceAgreePath");

    public final NumberPath<Long> serviceAgreeSort = createNumber("serviceAgreeSort", Long.class);

    public QServiceAgreement(String variable) {
        super(ServiceAgreement.class, forVariable(variable));
    }

    public QServiceAgreement(Path<? extends ServiceAgreement> path) {
        super(path.getType(), path.getMetadata());
    }

    public QServiceAgreement(PathMetadata metadata) {
        super(ServiceAgreement.class, metadata);
    }

}

