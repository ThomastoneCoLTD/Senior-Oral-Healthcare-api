package com.kaii.dentix.domain.organization.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DaeguDefaultOrganizationInitializer implements ApplicationRunner {

    private final DaeguDefaultOrganizationService daeguDefaultOrganizationService;

    @Override
    public void run(ApplicationArguments args) {
        daeguDefaultOrganizationService.getOrCreate();
    }
}
