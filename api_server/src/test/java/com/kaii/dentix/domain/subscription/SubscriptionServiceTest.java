package com.kaii.dentix.domain.subscription;

import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.dto.SubscriptionDto;
import com.kaii.dentix.domain.type.PlanName;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock private BillingRepository billingRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationSubscriptionRepository organizationSubscriptionRepository;
    @Mock private OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    @Mock private OralCheckRepository oralCheckRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void getSubscriptionInfo_usesOnlyCurrentUsagePeriodCounts() {
        LocalDateTime startDate = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime usageResetDate = LocalDateTime.of(2026, 3, 1, 0, 0);

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .id(1L)
                .planName(PlanName.SMALL)
                .planCycle("yearly")
                .planSort(1)
                .price(10000L)
                .maxSuccessResponses(10)
                .build();

        OrganizationSubscription subscription = OrganizationSubscription.builder()
                .subscriptionPlan(plan)
                .subscriptionStartDate(startDate)
                .subscriptionEndDate(LocalDateTime.of(2027, 2, 1, 0, 0))
                .usageResetDate(usageResetDate)
                .successCount(99)
                .build();

        Organization organization = Organization.builder()
                .organizationId(1L)
                .organizationName("테스트기관")
                .organizationSubscription(subscription)
                .build();

        User firstUser = User.builder()
                .userId(10L)
                .userName("사용자1")
                .build();
        User secondUser = User.builder()
                .userId(11L)
                .userName("사용자2")
                .build();

        given(organizationRepository.findById(1L)).willReturn(Optional.of(organization));
        given(userRepository.findByOrganization_OrganizationId(1L)).willReturn(List.of(firstUser, secondUser));
        given(oralCheckRepository.findUserUsageByOrganizationAndPeriod(eq(1L), any(), any()))
                .willReturn(List.of(
                        OralCheckDto.Usage.builder()
                                .userId(10L)
                                .userName("사용자1")
                                .successCount(2L)
                                .build()
                ));

        SubscriptionDto.InfoResponse response = subscriptionService.getSubscriptionInfo(1L);

        assertThat(response.getTotalSuccessCount()).isEqualTo(2);
        assertThat(response.getRemainingCount()).isEqualTo(8);
        assertThat(response.getUsageResetDate()).isEqualTo(usageResetDate);
        assertThat(response.getUsers()).extracting(SubscriptionDto.InfoResponse.UserUsage::getUserId)
                .containsExactly(10L, 11L);
        assertThat(response.getUsers()).extracting(SubscriptionDto.InfoResponse.UserUsage::getSuccessCount)
                .containsExactly(2, 0);
    }
}
