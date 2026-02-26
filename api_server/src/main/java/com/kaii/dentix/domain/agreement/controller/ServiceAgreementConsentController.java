package com.kaii.dentix.domain.agreement.controller;

import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.agreement.dto.ServiceAgreementConsentDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ServiceAgreementConsentController {

    private final ServiceAgreementConsentService serviceAgreementConsentService;

    /** 서비스 이용 동의 수정 */
    @PutMapping("/service-agreement/consent")
    public DataResponse<ServiceAgreementConsentDto.ModifyResponse> userModifyServiceAgree(
            HttpServletRequest request,
            @Valid @RequestBody ServiceAgreementConsentDto.ModifyRequest dto
    ) {
        return new DataResponse<>(serviceAgreementConsentService.userModifyServiceAgree(request, dto));
    }

    /** 서비스 동의 내역 조회 */
    @GetMapping("/service-agreement/consent")
    public DataResponse<List<ServiceAgreementConsentDto.Response>> getUserServiceAgreements(HttpServletRequest request) {
        return new DataResponse<>(serviceAgreementConsentService.getUserServiceAgreements(request));
    }
}