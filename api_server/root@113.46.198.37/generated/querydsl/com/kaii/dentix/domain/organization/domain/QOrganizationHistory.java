package com.kaii.dentix.domain.organization.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrganizationHistory is a Querydsl query type for OrganizationHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrganizationHistory extends EntityPathBase<OrganizationHistory> {

    private static final long serialVersionUID = -1670606059L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrganizationHistory organizationHistory = new QOrganizationHistory("organizationHistory");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final StringPath afterValue = createString("afterValue");

    public final StringPath beforeValue = createString("beforeValue");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final StringPath fieldName = createString("fieldName");

    public final NumberPath<Long> historyId = createNumber("historyId", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> modifiedByAdminId = createNumber("modifiedByAdminId", Long.class);

    public final QOrganization organization;

    public QOrganizationHistory(String variable) {
        this(OrganizationHistory.class, forVariable(variable), INITS);
    }

    public QOrganizationHistory(Path<? extends OrganizationHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrganizationHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrganizationHistory(PathMetadata metadata, PathInits inits) {
        this(OrganizationHistory.class, metadata, inits);
    }

    public QOrganizationHistory(Class<? extends OrganizationHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new QOrganization(forProperty("organization"), inits.get("organization")) : null;
    }

}

