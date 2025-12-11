package com.kaii.dentix.domain.organization.controller;

import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dto.OrganizationRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations")
@CrossOrigin(origins = "*", allowedHeaders = "*")

public class OrganizationController {
    private final OrganizationService organizationService;

    //기관 등록
    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@RequestBody OrganizationRequest request) {
        log.info("▶ 기관 등록 요청 body: name={}, phone={}, plan={}",
                request.getOrganizationName(),
                request.getOrganizationPhoneNumber(),
                request.getSubscriptionPlanId());
        return ResponseEntity.ok(organizationService.createOrganization(request));
    }

    // 기관 가입정보 조회
    @GetMapping("/check/{phoneNumber}")
    public ResponseEntity<OrganizationResponse> checkOrganization(
            @PathVariable String phoneNumber
    ) {
        OrganizationResponse response = organizationService.findByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(response);
    }

}


