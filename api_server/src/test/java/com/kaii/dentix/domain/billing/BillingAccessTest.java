package com.kaii.dentix.domain.billing;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.dao.BillingHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.security.AdminAccessGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingAccessTest {
    private final BillingRepository billings = mock(BillingRepository.class);
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final AdminRepository admins = mock(AdminRepository.class);
    private final BillingService service = new BillingService(new AdminAccessGuard(admins), billings,
            organizations, mock(BillingHistoryRepository.class), mock(ApplicationEventPublisher.class), mock(OrganizationSubscriptionHistoryRepository.class));
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void login() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("7", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(admins.findByIdWithOrganization(7L)).thenReturn(Optional.of(Admin.builder().adminId(7L).adminIsSuper(YnType.N)
                .organization(Organization.builder().organizationId(2L).build()).build()));
    }
    @Test void ownOrganizationListRemainsAvailableButAnotherExportCannotLoadData() {
        login(); when(billings.findAllByOrganization_OrganizationIdOrderByBilledAtDesc(2L)).thenReturn(List.of());
        assertThat(service.getBillingsByOrganization(2L)).isEmpty();
        assertThatThrownBy(() -> service.getBillingExcelBundle(3L)).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(organizations);
        verify(billings, never()).findAllByOrganization_OrganizationIdOrderByBilledAtDesc(3L);
    }
    @Test void ordinaryAdminCannotChangePaymentEvenInOwnOrganization() {
        login();
        assertThatThrownBy(() -> service.markBillingAsPaid(99L, "payment")).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(billings);
    }
}
