package com.kaii.dentix.domain.subscription.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSubscriptionPlan is a Querydsl query type for SubscriptionPlan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSubscriptionPlan extends EntityPathBase<SubscriptionPlan> {

    private static final long serialVersionUID = -1943959268L;

    public static final QSubscriptionPlan subscriptionPlan = new QSubscriptionPlan("subscriptionPlan");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final BooleanPath active = createBoolean("active");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final BooleanPath customSurveyEnabled = createBoolean("customSurveyEnabled");

    public final DateTimePath<java.time.LocalDateTime> deleted = createDateTime("deleted", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxSuccessResponses = createNumber("maxSuccessResponses", Integer.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final NumberPath<Integer> overuseUnitPrice = createNumber("overuseUnitPrice", Integer.class);

    public final StringPath planCycle = createString("planCycle");

    public final EnumPath<com.kaii.dentix.domain.type.PlanName> planName = createEnum("planName", com.kaii.dentix.domain.type.PlanName.class);

    public final NumberPath<Integer> planSort = createNumber("planSort", Integer.class);

    public final NumberPath<Double> price = createNumber("price", Double.class);

    public final BooleanPath reportExportEnabled = createBoolean("reportExportEnabled");

    public QSubscriptionPlan(String variable) {
        super(SubscriptionPlan.class, forVariable(variable));
    }

    public QSubscriptionPlan(Path<? extends SubscriptionPlan> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSubscriptionPlan(PathMetadata metadata) {
        super(SubscriptionPlan.class, metadata);
    }

}

