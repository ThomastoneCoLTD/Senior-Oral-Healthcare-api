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

    /**
     * 기관정보조회 - 기관전화번호
     */
    @GetMapping("/check/{phoneNumber}")
    public ResponseEntity<OrganizationResponse> checkOrganization(
            @PathVariable String phoneNumber
    ) {
        OrganizationResponse response = organizationService.findByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(response);
    }

}


