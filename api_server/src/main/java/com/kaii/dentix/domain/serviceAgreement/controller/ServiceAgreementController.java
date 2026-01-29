package com.kaii.dentix.domain.serviceAgreement.controller;

import com.kaii.dentix.domain.serviceAgreement.application.ServiceAgreementService;
import com.kaii.dentix.domain.serviceAgreement.dto.ServiceAgreementDto;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/service-agreement")
public class ServiceAgreementController {

    private final ServiceAgreementService serviceAgreementService;

    /**s
     * 약관 전체 조회
     */
    @GetMapping(name = "약관 전체 조회")
    public DataResponse<ServiceAgreementDto.ListResponse> serviceAgreementPath() {
        return new DataResponse<>(serviceAgreementService.serviceAgreementList());
    }
}