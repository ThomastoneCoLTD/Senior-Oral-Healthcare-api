package com.kaii.dentix.domain.userServiceAgreement.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserServiceAgreement is a Querydsl query type for UserServiceAgreement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserServiceAgreement extends EntityPathBase<UserServiceAgreement> {

    private static final long serialVersionUID = 1956222233L;

    public static final QUserServiceAgreement userServiceAgreement = new QUserServiceAgreement("userServiceAgreement");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final EnumPath<com.kaii.dentix.domain.type.YnType> isUserServiceAgree = createEnum("isUserServiceAgree", com.kaii.dentix.domain.type.YnType.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final NumberPath<Long> serviceAgreeId = createNumber("serviceAgreeId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final DateTimePath<java.util.Date> userServiceAgreeDate = createDateTime("userServiceAgreeDate", java.util.Date.class);

    public final NumberPath<Long> userServiceAgreeId = createNumber("userServiceAgreeId", Long.class);

    public QUserServiceAgreement(String variable) {
        super(UserServiceAgreement.class, forVariable(variable));
    }

    public QUserServiceAgreement(Path<? extends UserServiceAgreement> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserServiceAgreement(PathMetadata metadata) {
        super(UserServiceAgreement.class, metadata);
    }

}

