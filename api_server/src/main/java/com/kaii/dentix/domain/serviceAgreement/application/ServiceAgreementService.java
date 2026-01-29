package com.kaii.dentix.domain.serviceAgreement.application;

import com.kaii.dentix.domain.serviceAgreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.serviceAgreement.dto.ServiceAgreementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAgreementService {
    private final ServiceAgreementRepository serviceAgreementRepository;

    /**
     * 서비스 동의 목록 조회
     */
    public ServiceAgreementDto.ListResponse serviceAgreementList() {

        List<ServiceAgreementDto.Response> list = serviceAgreementRepository.findAll(Sort.by(Sort.Direction.ASC, "serviceAgreeSort"))
                .stream()
                .map(ServiceAgreementDto.Response::from) // from 메서드 사용으로 간결화
                .toList();

        return new ServiceAgreementDto.ListResponse(list);
    }

}