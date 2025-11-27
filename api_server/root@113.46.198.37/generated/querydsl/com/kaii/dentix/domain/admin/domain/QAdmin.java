package com.kaii.dentix.domain.admin.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAdmin is a Querydsl query type for Admin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdmin extends EntityPathBase<Admin> {

    private static final long serialVersionUID = 502266881L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAdmin admin = new QAdmin("admin");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final NumberPath<Long> adminId = createNumber("adminId", Long.class);

    public final EnumPath<com.kaii.dentix.domain.type.YnType> adminIsSuper = createEnum("adminIsSuper", com.kaii.dentix.domain.type.YnType.class);

    public final DateTimePath<java.util.Date> adminLastLoginDate = createDateTime("adminLastLoginDate", java.util.Date.class);

    public final StringPath adminLoginIdentifier = createString("adminLoginIdentifier");

    public final StringPath adminName = createString("adminName");

    public final StringPath adminPassword = createString("adminPassword");

    public final StringPath adminPhoneNumber = createString("adminPhoneNumber");

    public final StringPath adminRefreshToken = createString("adminRefreshToken");

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final DateTimePath<java.util.Date> deleted = createDateTime("deleted", java.util.Date.class);

    public final StringPath findPwdAnswer = createString("findPwdAnswer");

    public final NumberPath<Long> findPwdQuestionId = createNumber("findPwdQuestionId", Long.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.organization.domain.QOrganization organization;

    public QAdmin(String variable) {
        this(Admin.class, forVariable(variable), INITS);
    }

    public QAdmin(Path<? extends Admin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAdmin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAdmin(PathMetadata metadata, PathInits inits) {
        this(Admin.class, metadata, inits);
    }

    public QAdmin(Class<? extends Admin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new com.kaii.dentix.domain.organization.domain.QOrganization(forProperty("organization"), inits.get("organization")) : null;
    }

}

