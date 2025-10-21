package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.dto.AdminUserListDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/organization")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminOrganizationController {

    private final AdminOrganizationService adminOrganizationService;

    private final AdminUserService adminUserService;

    private JwtTokenUtil jwtTokenUtil;


    @GetMapping("/my")
    public ResponseEntity<OrganizationResponse> getMyOrganization(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminOrganizationService.getMyOrganization(httpRequest));
    }

//    @GetMapping("/admin/users")
//    public ResponseEntity<?> getUsersByOrganization(HttpServletRequest request) {
//        Long organizationId = jwtTokenUtil.getOrganizationIdFromAccessToken(request);
//        if (organizationId == null) {
//            throw new NotFoundDataException("기관 정보가 없습니다.");
//        }
//        List<AdminUserListDto> users = adminUserService.userList(organizationId);
//
//        return ResponseEntity.ok(users);
//    }
}
