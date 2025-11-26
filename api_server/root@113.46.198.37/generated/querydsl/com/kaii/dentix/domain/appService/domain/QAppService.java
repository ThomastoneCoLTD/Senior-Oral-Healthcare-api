package com.kaii.dentix.domain.appService.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAppService is a Querydsl query type for AppService
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAppService extends EntityPathBase<AppService> {

    private static final long serialVersionUID = -63097663L;

    public static final QAppService appService = new QAppService("appService");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final NumberPath<Long> appServiceId = createNumber("appServiceId", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final StringPath name = createString("name");

    public final EnumPath<com.kaii.dentix.domain.type.ServiceType> serviceType = createEnum("serviceType", com.kaii.dentix.domain.type.ServiceType.class);

    public QAppService(String variable) {
        super(AppService.class, forVariable(variable));
    }

    public QAppService(Path<? extends AppService> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAppService(PathMetadata metadata) {
        super(AppService.class, metadata);
    }

}

