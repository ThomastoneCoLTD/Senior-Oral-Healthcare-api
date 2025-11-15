package com.kaii.dentix.domain.organization.controller;

import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationUpdateRequest;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

//    //기관 상세 조회
//    @GetMapping("/{id}")
//    public ResponseEntity<OrganizationResponse> getOne(@PathVariable Long id) {
//        return ResponseEntity.ok(organizationService.getOrganizationById(id));
//    }
//
//    //기관 리스트 조회
//    @GetMapping
//    public ResponseEntity<Page<OrganizationResponse>> getAll(Pageable pageable) {
//        return ResponseEntity.ok(organizationService.getAllOrganizations(pageable));
//    }
//
//    //기관 수정
//    @PutMapping("/{id}")
//    public ResponseEntity<OrganizationResponse> update(
//            @PathVariable Long id,
//            @RequestBody OrganizationUpdateRequest request) {
//        return ResponseEntity.ok(organizationService.update(id, request));
//    }
//
//    //기관 삭제
//    @DeleteMapping("/{id}")
//    public ResponseEntity<String> softDelete(@PathVariable Long id) {
//        organizationService.softDelete(id);
//        return ResponseEntity.ok("삭제되었습니다.");
//    }
//
//    //기관 삭제
//    @DeleteMapping("/hard/{id}")
//    public ResponseEntity<String> hardDelete(@PathVariable Long id) {
//        organizationService.hardDelete(id);
//        return ResponseEntity.ok("삭제되었습니다.");
//    }
//
//    /**
//     * 기관의 구독상품 변경
//     */
//    @PutMapping("/{organizationId}/subscription/{subscriptionPlanId}")
//    public DataResponse<OrganizationResponse> changeSubscriptionPlan(
//            @PathVariable Long organizationId,
//            @PathVariable Long subscriptionPlanId
//    ) {
//        OrganizationResponse response = organizationService.changeSubscriptionPlan(organizationId, subscriptionPlanId);
//        return new DataResponse<>(response);
//    }
//
    /**
     * ✅ 기관 전화번호로 기관 존재 여부 및 구독 정보 조회
     *  (회원가입 전 확인용 - JWT 인증 제외)
     */
//    @GetMapping("/check/{phoneNumber}")
//    public ResponseEntity<DataResponse<OrganizationCheckResponse>> checkOrganization(
//            @PathVariable String phoneNumber
//    ) {
//        OrganizationCheckResponse response = organizationService.checkOrganizationByPhone(phoneNumber);
//        return ResponseEntity.ok(
//                new DataResponse<>(200, "기관 정보 조회 성공", response)
//        );
//    }
//    @GetMapping("/check/{organizationId}")
//    public ResponseEntity<OrganizationResponse> getOrganizationById(@RequestParam String phoneNumber) {
//        OrganizationResponse response = organizationService.checkOrganizationByPhone(phoneNumber);
//        return ResponseEntity.ok(response);
//    }
    // 기관 가입정보 조회
    @GetMapping("/check/{phoneNumber}")
    public ResponseEntity<OrganizationResponse> checkOrganization(
            @PathVariable String phoneNumber
) {
    OrganizationResponse response = organizationService.findByPhoneNumber(phoneNumber);
    return ResponseEntity.ok(response);
}
//    /**
//     * 기관번호로 기관 정보 조회 (회원가입 시 기관 확인용)
//     */
//    @GetMapping("/check/{organizationId}")
//    public ResponseEntity<OrganizationResponse> getOrganizationById(@PathVariable Long organizationId) {
//        OrganizationResponse response = organizationService.getCheckOrganizationById(organizationId);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * ✅ GET /api/organization/{id}/subscription/status
//     * 기관별 구독상품 및 사용률, 남은 응답 수, 초과 여부 포함
//     */
//    @GetMapping("/{id}/subscription/status")
//    public ResponseEntity<SubscriptionStatusResponse> getOrganizationSubscriptionStatus(@PathVariable Long id) {
//        Organization organization = organizationService.getOrganizationWithPlan(id);
//        SubscriptionPlan plan = organization.getSubscriptionPlan();
//
//        // 계산
//        int maxResponses = plan.getMaxSuccessResponses();
//        int successCount = organization.getSuccessCount() != null ? organization.getSuccessCount() : 0;
//        int remaining = organization.getRemainingResponses() != null ? organization.getRemainingResponses() : 0;
//        double usageRate = (maxResponses > 0) ? (double) successCount / maxResponses * 100 : 0.0;
//        boolean overused = remaining <= 0;
//
//        SubscriptionStatusResponse response = new SubscriptionStatusResponse(
//                organization.getOrganizationId(),
//                organization.getOrganizationName(),
//                plan.getPlanName(),
//                plan.getPlanCycle(),
//                plan.getPrice(),
//                plan.getMaxSuccessResponses(),
//                successCount,
//                remaining,
//                usageRate,
//                overused
//        );
//
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * ✅ 응답 DTO (Subscription + Usage 상태 통합)
//     */
//    record SubscriptionStatusResponse(
//            Long organizationId,
//            String organizationName,
//            String planName,
//            String planCycle,
//            Integer planPrice,
//            Integer planQuota,
//            Integer usedCount,
//            Integer remainingCount,
//            Double usageRate,
//            Boolean overused
//    ) {}
}


