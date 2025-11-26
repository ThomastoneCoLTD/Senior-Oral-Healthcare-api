package com.kaii.dentix.domain.systemLog.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSystemLog is a Querydsl query type for SystemLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSystemLog extends EntityPathBase<SystemLog> {

    private static final long serialVersionUID = -2011012799L;

    public static final QSystemLog systemLog = new QSystemLog("systemLog");

    public final DateTimePath<java.util.Date> created = createDateTime("created", java.util.Date.class);

    public final StringPath header = createString("header");

    public final StringPath requestBody = createString("requestBody");

    public final StringPath requestName = createString("requestName");

    public final StringPath requestUrl = createString("requestUrl");

    public final StringPath responseBody = createString("responseBody");

    public final NumberPath<Long> systemLogId = createNumber("systemLogId", Long.class);

    public final NumberPath<Long> tokenUserId = createNumber("tokenUserId", Long.class);

    public final EnumPath<com.kaii.dentix.domain.type.UserRole> tokenUserRole = createEnum("tokenUserRole", com.kaii.dentix.domain.type.UserRole.class);

    public QSystemLog(String variable) {
        super(SystemLog.class, forVariable(variable));
    }

    public QSystemLog(Path<? extends SystemLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSystemLog(PathMetadata metadata) {
        super(SystemLog.class, metadata);
    }

}

