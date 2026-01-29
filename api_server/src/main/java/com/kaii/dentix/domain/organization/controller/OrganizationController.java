package com.kaii.dentix.domain.organization.controller;

import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
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
    public ResponseEntity<OrganizationDto.Response> checkOrganization( // 타입 변경
                                                                       @PathVariable String phoneNumber
    ) {
        OrganizationDto.Response response = organizationService.findByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(response);
    }
}


