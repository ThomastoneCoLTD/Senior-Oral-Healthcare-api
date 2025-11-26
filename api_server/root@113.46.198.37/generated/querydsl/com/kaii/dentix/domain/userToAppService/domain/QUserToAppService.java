package com.kaii.dentix.domain.userToAppService.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserToAppService is a Querydsl query type for UserToAppService
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserToAppService extends EntityPathBase<UserToAppService> {

    private static final long serialVersionUID = -2077703219L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserToAppService userToAppService = new QUserToAppService("userToAppService");

    public final com.kaii.dentix.domain.appService.domain.QAppService appService;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.kaii.dentix.domain.user.domain.QUser user;

    public QUserToAppService(String variable) {
        this(UserToAppService.class, forVariable(variable), INITS);
    }

    public QUserToAppService(Path<? extends UserToAppService> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserToAppService(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserToAppService(PathMetadata metadata, PathInits inits) {
        this(UserToAppService.class, metadata, inits);
    }

    public QUserToAppService(Class<? extends UserToAppService> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.appService = inits.isInitialized("appService") ? new com.kaii.dentix.domain.appService.domain.QAppService(forProperty("appService")) : null;
        this.user = inits.isInitialized("user") ? new com.kaii.dentix.domain.user.domain.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

