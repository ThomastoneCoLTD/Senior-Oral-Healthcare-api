//package com.kaii.dentix.domain.admin.controller;
//
//import com.kaii.dentix.domain.jwt.JwtTokenUtil;
//import com.kaii.dentix.domain.organizationSubscriptionHistory.application.OrganizationSubscriptionHistoryService;
//import com.kaii.dentix.domain.organizationSubscriptionHistory.dto.OrganizationSubscriptionHistoryResponse;
//import com.kaii.dentix.domain.subscription.application.SubscriptionHistoryService;
//import com.kaii.dentix.domain.subscription.dto.SubscriptionHistoryResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
///**
// * ✅ 관리자용 구독 이력 조회 컨트롤러
// */
//@RestController
//@RequestMapping("/admin/organization")
//@RequiredArgsConstructor
//public class AdminSubscriptionHistoryController {
//
//    private final SubscriptionHistoryService subscriptionHistoryService;
//    private final JwtTokenUtil jwtTokenUtil;
//    private final OrganizationSubscriptionHistoryService organizationSubscriptionHistoryService;
//
//    /** ✅ 기관별 구독 이력 조회 */
//    @GetMapping("/{organizationId}/subscription-history")
//    public ResponseEntity<List<SubscriptionHistoryResponse>> getSubscriptionHistory(
//            @PathVariable Long organizationId
//    ) {
//        List<SubscriptionHistoryResponse> historyList =
//                organizationSubscriptionHistoryService.getSubscriptionHistoryByOrganization(organizationId);
//        return ResponseEntity.ok(historyList);
//    }
//
//    /** ✅ 기관 관리자 본인 기관의 구독 이력 조회 */
//    @GetMapping("/history")
//    public ResponseEntity<List<OrganizationSubscriptionHistoryResponse>> getMySubscriptionHistory() {
//        Long adminId = jwtTokenUtil.getCurrentAdminId();
//
//        List<OrganizationSubscriptionHistoryResponse> histories =
//                organizationSubscriptionHistoryService.getMySubscriptionHistory(adminId);
//
//        return ResponseEntity.ok(histories);
//    }
//}