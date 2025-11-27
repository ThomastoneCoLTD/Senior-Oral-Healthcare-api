package com.kaii.dentix.domain.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -2102436177L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final com.kaii.dentix.global.common.entity.QTimeEntity _super = new com.kaii.dentix.global.common.entity.QTimeEntity(this);

    public final DateTimePath<java.util.Date> birth = createDateTime("birth", java.util.Date.class);

    //inherited
    public final DateTimePath<java.util.Date> created = _super.created;

    public final DateTimePath<java.util.Date> deleted = createDateTime("deleted", java.util.Date.class);

    public final StringPath findPwdAnswer = createString("findPwdAnswer");

    public final NumberPath<Long> findPwdQuestionId = createNumber("findPwdQuestionId", Long.class);

    public final EnumPath<com.kaii.dentix.domain.type.YnType> isVerify = createEnum("isVerify", com.kaii.dentix.domain.type.YnType.class);

    //inherited
    public final DateTimePath<java.util.Date> modified = _super.modified;

    public final com.kaii.dentix.domain.organization.domain.QOrganization organization;

    public final NumberPath<Integer> successCount = createNumber("successCount", Integer.class);

    public final EnumPath<com.kaii.dentix.domain.type.GenderType> userGender = createEnum("userGender", com.kaii.dentix.domain.type.GenderType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final DateTimePath<java.util.Date> userLastLoginDate = createDateTime("userLastLoginDate", java.util.Date.class);

    public final StringPath userLoginIdentifier = createString("userLoginIdentifier");

    public final StringPath userName = createString("userName");

    public final StringPath userPassword = createString("userPassword");

    public final StringPath userPhoneNumber = createString("userPhoneNumber");

    public final StringPath userRefreshToken = createString("userRefreshToken");

    public final ListPath<com.kaii.dentix.domain.userToAppService.domain.UserToAppService, com.kaii.dentix.domain.userToAppService.domain.QUserToAppService> userToAppServices = this.<com.kaii.dentix.domain.userToAppService.domain.UserToAppService, com.kaii.dentix.domain.userToAppService.domain.QUserToAppService>createList("userToAppServices", com.kaii.dentix.domain.userToAppService.domain.UserToAppService.class, com.kaii.dentix.domain.userToAppService.domain.QUserToAppService.class, PathInits.DIRECT2);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.organization = inits.isInitialized("organization") ? new com.kaii.dentix.domain.organization.domain.QOrganization(forProperty("organization"), inits.get("organization")) : null;
    }

}

