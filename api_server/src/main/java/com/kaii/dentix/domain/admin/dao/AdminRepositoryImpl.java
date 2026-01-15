package com.kaii.dentix.domain.admin.dao;

import com.kaii.dentix.domain.admin.domain.QAdmin;
import com.kaii.dentix.domain.admin.dto.AdminAccountDto;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.dto.PagingRequest;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdminRepositoryImpl implements AdminCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final QAdmin admin = QAdmin.admin;

    /**
     *  관리자 페이징 목록
     */
    @Override
    public Page<AdminAccountDto> findAllByNotSuper(PageAndSizeRequest request){

        Pageable paging = new PagingRequest(request.getPage(), request.getSize()).of();

        // fetchCount Deprecated 로 인해 count 쿼리 구현
        long total = Optional.ofNullable(queryFactory.select(admin.count()).from(admin).where(admin.adminIsSuper.eq(YnType.N)).fetchOne())
                .orElse(0L);

        // total 이 0보다 크면 조건에 맞게 페이징 처리 , 0 이면 빈 리스트 반환
        List<AdminAccountDto> result = total > 0 ? queryFactory
                .select(Projections.constructor(AdminAccountDto.class,
                        admin.adminId, admin.adminLoginIdentifier, admin.adminName, admin.adminPhoneNumber,
                        Expressions.stringTemplate("DATE_FORMAT({0}, {1})", admin.created, "%Y-%m-%d")
                ))
                .from(admin)
                .where(admin.adminIsSuper.eq(YnType.N))
                .orderBy(admin.created.desc())
                .offset(paging.getOffset())
                .limit(paging.getPageSize())
                .fetch() : new ArrayList<>();

        return new PageImpl<>(result, paging, total);
    }
}
