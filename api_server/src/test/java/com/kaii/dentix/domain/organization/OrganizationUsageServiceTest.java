package com.kaii.dentix.domain.organization;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.application.OrganizationUsageService;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.PlanName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrganizationUsageServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private OralCheckRepository oralCheckRepository;
    @Mock private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @InjectMocks
    private OrganizationUsageService organizationUsageService;

    @Test
    void getMyOrganizationUsage_usesUsageResetDateAsPeriodEnd() {
        LocalDateTime subscriptionStartDate = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime usageResetDate = LocalDateTime.of(2026, 3, 1, 0, 0);
        LocalDateTime subscriptionEndDate = LocalDateTime.of(2027, 2, 1, 0, 0);

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .planName(PlanName.SMALL)
                .maxSuccessResponses(10)
                .build();

        Organization organization = Organization.builder()
                .organizationId(1L)
                .organizationName("테스트기관")
                .build();

        Admin admin = Admin.builder()
                .adminId(7L)
                .organization(organization)
                .build();

        OrganizationSubscription subscription = OrganizationSubscription.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .subscriptionStartDate(subscriptionStartDate)
                .subscriptionEndDate(subscriptionEndDate)
                .usageResetDate(usageResetDate)
                .build();

        given(adminRepository.findById(7L)).willReturn(Optional.of(admin));
        given(organizationSubscriptionRepository.findByOrganization_OrganizationId(1L)).willReturn(Optional.of(subscription));
        given(oralCheckRepository.countSubscriptionPeriodUsage(eq(1L), any(Date.class), any(Date.class))).willReturn(3L);
        given(oralCheckRepository.countTodayUsage(1L)).willReturn(0L);
        given(oralCheckRepository.countThisWeekUsage(1L)).willReturn(0L);
        given(oralCheckRepository.countThisMonthUsage(1L)).willReturn(0L);
        given(oralCheckRepository.findTopUsers(1L)).willReturn(Collections.emptyList());
        given(oralCheckRepository.findRecentUsages(1L)).willReturn(Collections.emptyList());

        OrganizationDto.UsageResponse response = organizationUsageService.getMyOrganizationUsage(7L);

        ArgumentCaptor<Date> endDateCaptor = ArgumentCaptor.forClass(Date.class);
        org.mockito.Mockito.verify(oralCheckRepository).countSubscriptionPeriodUsage(eq(1L), any(Date.class), endDateCaptor.capture());

        Date expectedEndDate = Date.from(usageResetDate.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        assertThat(endDateCaptor.getValue()).isEqualTo(expectedEndDate);
        assertThat(response.getSuccessCount()).isEqualTo(3L);
        assertThat(response.getRemainingResponses()).isEqualTo(7L);
    }
}
