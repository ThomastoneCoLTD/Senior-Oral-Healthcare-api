package com.kaii.dentix.domain.agreement.application;

import com.kaii.dentix.domain.agreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.agreement.domain.ServiceAgreement;
import com.kaii.dentix.domain.type.YnType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceAgreementInitializer {

    private final ServiceAgreementRepository serviceAgreementRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedServiceAgreements() {
        for (ServiceAgreement agreement : defaultAgreements()) {
            if (serviceAgreementRepository.findByServiceAgreeSort(agreement.getServiceAgreeSort()).isEmpty()) {
                serviceAgreementRepository.save(agreement);
            }
        }
    }

    private List<ServiceAgreement> defaultAgreements() {
        return List.of(
                agreement(
                        1L,
                        "서비스 이용약관",
                        "서비스 이용약관",
                        YnType.Y,
                        "덴티하이 관리자 페이지"
                ),
                agreement(
                        2L,
                        "개인정보 수집 및 이용",
                        "개인정보 수집 및 이용",
                        YnType.Y,
                        "덴티로카 관리자 페이지"
                ),
                agreement(
                        3L,
                        "마케팅 정보 수신",
                        "마케팅 정보 수신",
                        YnType.N,
                        "덴티엑스 API 문서"
                )
        );
    }

    private ServiceAgreement agreement(
            Long sort,
            String name,
            String menuName,
            YnType required,
            String path
    ) {
        return ServiceAgreement.builder()
                .serviceAgreeSort(sort)
                .serviceAgreeName(name)
                .serviceAgreeMenuName(menuName)
                .isServiceAgreeRequired(required)
                .serviceAgreePath(path)
                .build();
    }
}
