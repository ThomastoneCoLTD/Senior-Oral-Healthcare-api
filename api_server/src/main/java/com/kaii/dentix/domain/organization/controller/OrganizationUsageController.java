package com.kaii.dentix.domain.organization.controller;

import com.kaii.dentix.domain.organization.application.OrganizationUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class OrganizationUsageController {
    private final OrganizationUsageService organizationUsageService;

//    @PostMapping("/{id}/success")
//    public ResponseEntity<Map<String, Object>> recordSuccess(@PathVariable Long id){
//        int remaining = organizationUsageService.recordSuccessAndGetRemaining(id);
//        Map<String, Object> response = new HashMap<>();
//        response.put("message","success response record success.");
//        response.put("remainingResponses",remaining);
//
//        return ResponseEntity.ok(response);
//    }
}
