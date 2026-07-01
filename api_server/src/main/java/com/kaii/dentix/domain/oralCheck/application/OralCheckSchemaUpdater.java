package com.kaii.dentix.domain.oralCheck.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OralCheckSchemaUpdater implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer notNullableCount = jdbcTemplate.queryForObject(
                    """
                            select count(*)
                            from information_schema.columns
                            where table_schema = database()
                              and table_name = 'oral_check'
                              and column_name = 'subscription_history_id'
                              and is_nullable = 'NO'
                            """,
                    Integer.class
            );

            if (notNullableCount != null && notNullableCount > 0) {
                jdbcTemplate.execute("alter table oral_check modify column subscription_history_id bigint null");
                log.info("[ORAL_CHECK_SCHEMA_UPDATE] subscription_history_id nullable migration applied");
            }
        } catch (DataAccessException e) {
            log.warn("[ORAL_CHECK_SCHEMA_UPDATE] subscription_history_id nullable migration skipped", e);
        }
    }
}
