//package com.kaii.dentix.domain.patient.dao;
//
//import com.kaii.dentix.domain.admin.dto.AdminPatientInfoDto;
//import com.kaii.dentix.domain.admin.dto.request.AdminPatientListRequest;
//import com.kaii.dentix.domain.patient.domain.QPatient;
//import com.kaii.dentix.domain.type.YnType;
//import com.kaii.dentix.domain.user.domain.QUser;
//import com.kaii.dentix.global.common.dto.PagingRequest;
//import com.querydsl.core.types.Projections;
//import com.querydsl.core.types.dsl.CaseBuilder;
//import com.querydsl.jpa.impl.JPAQueryFactory;
//import io.micrometer.common.util.StringUtils;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//@RequiredArgsConstructor
//public class AdminPatientCustomRepositoryImpl implements AdminPatientCustomRepository{
//
//    private final JPAQueryFactory queryFactory;
//
//    private final QPatient patient = QPatient.patient;
//
//    private final QUser user = QUser.user;
//
//    /**
//     *  관리자 환자 목록 조회
//     */
//    @Override
//    public Page<AdminPatientInfoDto> findAll(AdminPatientListRequest request) {
//        Pageable paging = new PagingRequest(request.getPage(), request.getSize()).of();
//
//        List<AdminPatientInfoDto> result = queryFactory
//                .select(Projections.constructor(AdminPatientInfoDto.class,
//                        patient.patientId, patient.patientName, patient.patientPhoneNumber, patient.created,
//                        new CaseBuilder() // 연계된 대상자 존재 여부 (isUser)
//                                .when(user.patientId.isNotNull()) // 회원가입을 한 환자의 경우
//                                .then(YnType.Y.toString())
//                                .otherwise(YnType.N.toString()) // 미가입 혹은 탈퇴한 환자의 경우
//                                .as("isUser")
//                ))
//                .from(patient)
//                .leftJoin(user).on(patient.patientId.eq(user.patientId).and(user.deleted.isNull()))
//                .where(
//                        StringUtils.isNotBlank(request.getPatientNameOrPhoneNumber()) ?
//                        patient.patientName.contains(request.getPatientNameOrPhoneNumber()).or(patient.patientPhoneNumber.contains(request.getPatientNameOrPhoneNumber()))
//                        : null)
//                .orderBy(patient.created.desc())
//                .offset(paging.getOffset())
//                .limit(paging.getPageSize())
//                .fetch();
//
//        // fetchCount Deprecated 로 인해 count 쿼리 구현
//        Long total = Optional.ofNullable(queryFactory
//                .select(patient.countDistinct())
//                .from(patient)
//                .leftJoin(user).on(patient.patientId.eq(user.patientId).and(user.deleted.isNull()))
//                .where(
//                        StringUtils.isNotBlank(request.getPatientNameOrPhoneNumber()) ?
//                        patient.patientName.contains(request.getPatientNameOrPhoneNumber()).or(patient.patientPhoneNumber.contains(request.getPatientNameOrPhoneNumber()))
//                        : null)
//                .fetchOne()).orElse(0L);
//
//        return new PageImpl<>(result, paging, total);
//    }
//
//}
