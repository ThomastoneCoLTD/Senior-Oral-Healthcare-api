package com.kaii.dentix.domain.superAdmin;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.superAdmin.application.SuperAdminOrganizationService;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminStatisticDto;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminOrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    @Mock private BillingRepository billingRepository;
    @Mock private OralCheckRepository oralCheckRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminUserCustomRepository adminUserCustomRepository;

    @InjectMocks
    private SuperAdminOrganizationService service;

    @Test
    void statistics_groupsUsersByRealOrganization() {
        User daegu1Male = user("대구1", GenderType.M);
        User daegu1Female = user("대구1", GenderType.W);
        User daegu2Female = user("대구2", GenderType.W);
        User unassigned = user(null, GenderType.M);
        when(userRepository.findAll()).thenReturn(List.of(
                daegu2Female,
                unassigned,
                daegu1Male,
                daegu1Female
        ));

        SuperAdminStatisticDto.TotalUserStats response =
                service.getSuperAdminTotalUserStatistics(null);

        assertThat(response.getTotalUsers()).isEqualTo(4);
        assertThat(response.getMaleUsers()).isEqualTo(2);
        assertThat(response.getFemaleUsers()).isEqualTo(2);
        assertThat(response.getOrganizationStats())
                .extracting(SuperAdminStatisticDto.OrgUserStats::getOrganizationName)
                .containsExactly("대구1", "대구2", "기관 미지정");
        assertThat(response.getOrganizationStats().get(0).getTotalUsers()).isEqualTo(2);
        assertThat(response.getOrganizationStats().get(0).getMaleUsers()).isEqualTo(1);
        assertThat(response.getOrganizationStats().get(0).getFemaleUsers()).isEqualTo(1);
    }

    private User user(String realOrganization, GenderType gender) {
        User user = User.builder()
                .realOrganization(realOrganization)
                .userGender(gender)
                .build();
        user.setCreated(new Date());
        return user;
    }
}
