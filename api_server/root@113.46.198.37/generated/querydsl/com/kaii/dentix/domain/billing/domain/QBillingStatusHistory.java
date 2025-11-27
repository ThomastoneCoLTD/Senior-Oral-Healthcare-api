package com.kaii.dentix.domain.billing.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBillingStatusHistory is a Querydsl query type for BillingStatusHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBillingStatusHistory extends EntityPathBase<BillingStatusHistory> {

    private static final long serialVersionUID = -945415423L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBillingStatusHistory billingStatusHistory = new QBillingStatusHistory("billingStatusHistory");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final QBilling billing;

    public final NumberPath<Long> billingStatusHistoryId = createNumber("billingStatusHistoryId", Long.class);

    public final StringPath changedBy = createString("changedBy");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final StringPath memo = createString("memo");

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final EnumPath<com.kaii.dentix.domain.type.BillingStatus> newStatus = createEnum("newStatus", com.kaii.dentix.domain.type.BillingStatus.class);

    public final EnumPath<com.kaii.dentix.domain.type.BillingStatus> oldStatus = createEnum("oldStatus", com.kaii.dentix.domain.type.BillingStatus.class);

    public QBillingStatusHistory(String variable) {
        this(BillingStatusHistory.class, forVariable(variable), INITS);
    }

    public QBillingStatusHistory(Path<? extends BillingStatusHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBillingStatusHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBillingStatusHistory(PathMetadata metadata, PathInits inits) {
        this(BillingStatusHistory.class, metadata, inits);
    }

    public QBillingStatusHistory(Class<? extends BillingStatusHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.billing = inits.isInitialized("billing") ? new QBilling(forProperty("billing"), inits.get("billing")) : null;
    }

}

