package com.kaii.dentix.domain.errorLog.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QErrorLog is a Querydsl query type for ErrorLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QErrorLog extends EntityPathBase<ErrorLog> {

    private static final long serialVersionUID = -661101359L;

    public static final QErrorLog errorLog = new QErrorLog("errorLog");

    public final DateTimePath<java.util.Date> created = createDateTime("created", java.util.Date.class);

    public final NumberPath<Long> errorLogId = createNumber("errorLogId", Long.class);

    public final StringPath errorLogMessage = createString("errorLogMessage");

    public final StringPath header = createString("header");

    public final StringPath requestBody = createString("requestBody");

    public final StringPath requestName = createString("requestName");

    public final StringPath requestUrl = createString("requestUrl");

    public final NumberPath<Long> tokenUserId = createNumber("tokenUserId", Long.class);

    public final EnumPath<com.kaii.dentix.domain.type.UserRole> tokenUserRole = createEnum("tokenUserRole", com.kaii.dentix.domain.type.UserRole.class);

    public QErrorLog(String variable) {
        super(ErrorLog.class, forVariable(variable));
    }

    public QErrorLog(Path<? extends ErrorLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QErrorLog(PathMetadata metadata) {
        super(ErrorLog.class, metadata);
    }

}

