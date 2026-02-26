//package com.kaii.dentix.domain.serviceAgreement.dao;
//
//import com.kaii.dentix.domain.agreement.dto.ServiceAgreementConsentDto;
//import com.kaii.dentix.domain.agreement.domain.QServiceAgreement;
//import com.kaii.dentix.domain.type.YnType;
//import com.kaii.dentix.domain.agreement.domain.QServiceAgreementConsent;
//import com.querydsl.core.types.Projections;
//import com.querydsl.core.types.dsl.CaseBuilder;
//import com.querydsl.jpa.impl.JPAQueryFactory;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//@RequiredArgsConstructor
//public class ServiceAgreementRepositoryImpl implements ServiceAgreementCustomRepository{
//
//    private final JPAQueryFactory queryFactory;
//
//    private final QServiceAgreement serviceAgreement = QServiceAgreement.serviceAgreement;
//
//    private final QServiceAgreementConsent serviceAgreementConsent = QServiceAgreementConsent.serviceAgreementConsent;
//
//    /**
//     *  사용자 서비스 이용 동의 '선택' 항목 리스트 조회
//     */
//    @Override
//    public List<ServiceAgreementConsentDto.Response> findAllByNotRequiredServiceAgreement(Long userId) {
//        return queryFactory
//                .select(Projections.constructor(ServiceAgreementConsentDto.Response.class,
//                        serviceAgreement.serviceAgreeId,
//                        new CaseBuilder()
//                                .when(serviceAgreementConsent.isUserServiceAgree.isNull())
//                                .then(YnType.N)
//                                .otherwise(serviceAgreementConsent.isUserServiceAgree),
//                        serviceAgreementConsent.userServiceAgreeDate
//                ))
//                .from(serviceAgreement)
//                .leftJoin(serviceAgreementConsent).on(
//                        serviceAgreementConsent.serviceAgreeId.eq(serviceAgreement.serviceAgreeId)
//                                .and(serviceAgreementConsent.userId.eq(userId))
//                )
//                .where(serviceAgreement.isServiceAgreeRequired.eq(YnType.N))
//                .orderBy(serviceAgreement.serviceAgreeSort.asc())
//                .fetch();
//    }
//
//}
