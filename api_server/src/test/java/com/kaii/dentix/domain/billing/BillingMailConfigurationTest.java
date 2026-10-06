package com.kaii.dentix.domain.billing;

import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.billing.dao.BillingHistoryRepository;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.event.OveruseBillingCreatedEvent;
import com.kaii.dentix.domain.billing.event.OveruseBillingEventListener;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.global.common.mail.EmailService;
import com.kaii.dentix.global.security.AdminAccessGuard;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BillingMailConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class,
                    MailHealthContributorAutoConfiguration.class))
            .withUserConfiguration(BillingConfiguration.class);

    private ApplicationContextRunner prodRunner() {
        return runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod", "spring.mail.password=");
    }

    @Test
    void productionStartsWithBlankMailKeyAndNoMailHealthCheck() {
        prodRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(BillingService.class)
                    .hasSingleBean(EmailService.class).doesNotHaveBean(OveruseBillingEventListener.class)
                    .doesNotHaveBean("mailHealthContributor");
            assertThat(context.getEnvironment().getProperty("soh.mail.enabled", Boolean.class)).isFalse();
            assertThat(context.getBean(JavaMailSenderImpl.class).getPassword()).isEmpty();
        });
    }

    @Test
    void disabledMailNeedsNoSenderAndDoesNotReadEventEntities() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(BillingService.class)
                    .doesNotHaveBean(JavaMailSender.class).doesNotHaveBean(OveruseBillingEventListener.class);
            context.getBean(EmailService.class).sendBillingNotice(null, null, 0);
            Billing untouchedBilling = mock(Billing.class);
            context.publishEvent(new OveruseBillingCreatedEvent(untouchedBilling));
            verifyNoInteractions(untouchedBilling);
        });
    }

    @Test
    void disabledMailStillPersistsOveruseBillingWithExistingPriceAndStatus() {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        prodRunner().withBean(JavaMailSenderImpl.class, () -> sender).run(context -> {
            Organization organization = Organization.builder().organizationId(7L)
                    .organizationName("테스트 기관").organizationEmail("test@example.invalid").build();
            SubscriptionPlan plan = SubscriptionPlan.builder().overuseUnitPrice(150).build();
            OrganizationSubscriptionHistory history = OrganizationSubscriptionHistory.builder()
                    .organization(organization).subscriptionPlan(plan).build();
            when(context.getBean(OrganizationSubscriptionHistoryRepository.class)
                    .findByOrganization_OrganizationIdAndEndDateIsNull(7L)).thenReturn(Optional.of(history));
            BillingRepository billings = context.getBean(BillingRepository.class);

            Billing billing = context.getBean(BillingService.class).createOveruseBatchBilling(organization);

            verify(billings).save(billing);
            assertThat(billing.getAmount()).isEqualTo(150L);
            assertThat(billing.getBillingStatus()).isEqualTo(BillingStatus.PENDING);
            assertThat(billing.getBillingType()).isEqualTo(BillingType.OVERUSE);
            assertThat(billing.getOrganization()).isSameAs(organization);
            assertThat(billing.getSubscriptionPlan()).isSameAs(plan);
            context.getBean(EmailService.class).sendBillingNotice("test@example.invalid", "테스트 기관", 150);
            verifyNoInteractions(sender);
        });
    }

    @Test
    void duplicatePendingOveruseBillingIsStillSkipped() {
        runner.run(context -> {
            Organization organization = Organization.builder().organizationId(7L).build();
            OrganizationSubscriptionHistory history = OrganizationSubscriptionHistory.builder()
                    .subscriptionPlan(SubscriptionPlan.builder().overuseUnitPrice(150).build()).build();
            when(context.getBean(OrganizationSubscriptionHistoryRepository.class)
                    .findByOrganization_OrganizationIdAndEndDateIsNull(7L)).thenReturn(Optional.of(history));
            BillingRepository billings = context.getBean(BillingRepository.class);
            when(billings.existsByOrganizationAndBillingTypeAndBillingStatus(
                    organization, BillingType.OVERUSE, BillingStatus.PENDING)).thenReturn(true);

            assertThat(context.getBean(BillingService.class).createOveruseBatchBilling(organization)).isNull();
            verify(billings, never()).save(any());
        });
    }

    @Test
    void explicitlyEnabledMailRetainsBillingNoticeAndMailHealthCheck() {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        prodRunner().withPropertyValues("SOH_MAIL_ENABLED=true")
                .withBean(JavaMailSenderImpl.class, () -> sender).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(OveruseBillingEventListener.class)
                    .hasBean("mailHealthContributor");
            Billing billing = Billing.builder().amount(150L).organization(Organization.builder()
                    .organizationName("테스트 기관").organizationEmail("test@example.invalid").build()).build();
            context.publishEvent(new OveruseBillingCreatedEvent(billing));

            ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(sender).send(message.capture());
            assertThat(message.getValue().getTo()).containsExactly("test@example.invalid");
            assertThat(message.getValue().getSubject()).contains("테스트 기관", "추가 과금 안내");
            assertThat(message.getValue().getText()).contains("150원");
        });
    }

    @Test
    void enablingMailWithoutSenderReportsConfigurationErrorOnlyWhenCalled() {
        runner.withPropertyValues("soh.mail.enabled=true").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(BillingService.class);
            assertThatThrownBy(() -> context.getBean(EmailService.class)
                    .sendBillingNotice("test@example.invalid", "테스트 기관", 150))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("SMTP");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({BillingService.class, EmailService.class, OveruseBillingEventListener.class})
    static class BillingConfiguration {
        @Bean AdminAccessGuard accessGuard() { return mock(AdminAccessGuard.class); }
        @Bean BillingRepository billings() { return mock(BillingRepository.class); }
        @Bean OrganizationRepository organizations() { return mock(OrganizationRepository.class); }
        @Bean BillingHistoryRepository billingHistory() { return mock(BillingHistoryRepository.class); }
        @Bean OrganizationSubscriptionHistoryRepository subscriptionHistory() {
            return mock(OrganizationSubscriptionHistoryRepository.class);
        }
    }
}
