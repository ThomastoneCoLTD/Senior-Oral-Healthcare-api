package com.kaii.dentix.domain.serviceAgreement.controller;

import com.kaii.dentix.domain.serviceAgreement.application.ServiceAgreementService;
import com.kaii.dentix.domain.serviceAgreement.dto.ServiceAgreementListDto;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreementResponse;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/service-agreement")
public class ServiceAgreementController {

    private final ServiceAgreementService serviceAgreementService;
    private final UserService userService;

    /**
     * 약관 전체 조회
     */
    @GetMapping(name = "약관 전체 조회")
    public DataResponse<ServiceAgreementListDto> serviceAgreementPath() {
        DataResponse<ServiceAgreementListDto> response = new DataResponse<>(serviceAgreementService.serviceAgreementList());
        return response;
    }
}