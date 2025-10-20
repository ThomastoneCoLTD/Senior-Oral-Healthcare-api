package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminStatisticService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.statistic.AdminUserStatisticResponse;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.subscriptionInfo.application.SubscriptionInfoService;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/statistic")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminStatisticController {

    private final AdminStatisticService adminStatisticService;
    private final AdminService adminService;
    private final AdminRepository adminRepository;
    /**
     *  사용자 통계
     */
    @GetMapping(name = "사용자 통계")
    public DataResponse<AdminUserStatisticResponse> userStatistic(AdminStatisticRequest request){
        DataResponse<AdminUserStatisticResponse> response = new DataResponse<>(adminStatisticService.userStatistic(request));
        return response;
    }

    private final SubscriptionInfoService subscriptionInfoService;
    private final JwtTokenUtil jwtTokenUtil;

    @GetMapping("/me")
    public ResponseEntity<SubscriptionInfoResponse> getMySubscriptionInfo(HttpServletRequest request) {
        // ✅ 1️⃣ Access Token 추출
        String token = jwtTokenUtil.getAccessToken(request);
        if (token == null) {
            throw new UnauthorizedException("Access Token이 없습니다.");
        }

        // ✅ 2️⃣ 토큰 정보 파싱
        Long adminId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
        UserRole role = jwtTokenUtil.getRoles(token, TokenType.AccessToken);

        if (role != UserRole.ROLE_ADMIN) {
            throw new UnauthorizedException("어드민 계정만 접근할 수 있습니다.");
        }

        // ✅ 3️⃣ 어드민 엔티티 조회 후 기관 ID 추출
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundDataException("어드민 정보를 찾을 수 없습니다."));

        Long orgId = admin.getOrganization().getOrganizationId();

        // ✅ 4️⃣ 기관 구독 정보 조회
        SubscriptionInfoResponse response = subscriptionInfoService.getSubscriptionInfo(orgId);

        return ResponseEntity.ok(response);
    }
}
