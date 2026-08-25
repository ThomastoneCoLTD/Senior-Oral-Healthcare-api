package com.kaii.dentix.global.common.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetiredFeatureCleanup implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS tooth_brushing");
        log.info("[RETIRED_FEATURE_CLEANUP] tooth_brushing table removed if it existed");
    }
}
