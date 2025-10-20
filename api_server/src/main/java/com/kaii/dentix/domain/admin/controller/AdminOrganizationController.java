package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/organization")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminOrganizationController {
    private final AdminOrganizationService adminOrganizationService;
    @GetMapping("/my")
    public ResponseEntity<OrganizationResponse> getMyOrganization(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminOrganizationService.getMyOrganization(httpRequest));
    }
}
