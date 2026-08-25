package com.kaii.dentix.global.common.migration;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RetiredFeatureCleanupTest {

    @Test
    void removesOnlyRetiredToothBrushingTable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        new RetiredFeatureCleanup(jdbcTemplate).run(null);

        verify(jdbcTemplate).execute("DROP TABLE IF EXISTS tooth_brushing");
    }
}
