package com.kaii.dentix.domain.agreement.application;

import com.kaii.dentix.domain.agreement.dao.ServiceAgreementConsentRepository;
import com.kaii.dentix.domain.agreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.agreement.domain.ServiceAgreementConsent;
import com.kaii.dentix.domain.agreement.dto.ServiceAgreementDto;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceAgreementConsentServiceTest {

    private ServiceAgreementService serviceAgreementService;
    private ServiceAgreementConsentRepository serviceAgreementConsentRepository;
    private ServiceAgreementConsentService service;

    @BeforeEach
    void setUp() {
        serviceAgreementService = mock(ServiceAgreementService.class);
        serviceAgreementConsentRepository = mock(ServiceAgreementConsentRepository.class);
        service = new ServiceAgreementConsentService(
                mock(UserService.class),
                serviceAgreementService,
                serviceAgreementConsentRepository,
                mock(ServiceAgreementRepository.class)
        );
    }

    @Test
    void saveUserServiceAgreementsIgnoresUnknownAgreementIds() {
        when(serviceAgreementService.serviceAgreementList())
                .thenReturn(new ServiceAgreementDto.ListResponse(List.of(
                        agreement(1L, YnType.Y),
                        agreement(2L, YnType.Y),
                        agreement(3L, YnType.N)
                )));

        service.saveUserServiceAgreements(10L, List.of(1L, 2L, 4L, 5L));

        ArgumentCaptor<ServiceAgreementConsent> captor =
                ArgumentCaptor.forClass(ServiceAgreementConsent.class);
        verify(serviceAgreementConsentRepository, times(3)).save(captor.capture());

        List<ServiceAgreementConsent> saved = captor.getAllValues();
        assertThat(saved).extracting(ServiceAgreementConsent::getServiceAgreeId)
                .containsExactly(1L, 2L, 3L);
        assertThat(saved).extracting(ServiceAgreementConsent::getIsUserServiceAgree)
                .containsExactly(YnType.Y, YnType.Y, YnType.N);
    }

    private ServiceAgreementDto.Response agreement(Long id, YnType required) {
        return ServiceAgreementDto.Response.builder()
                .id(id)
                .name("agreement-" + id)
                .menuName("agreement-" + id)
                .isServiceAgreeRequired(required)
                .path("path")
                .build();
    }
}
