package com.kaii.dentix.domain.gingivitis;

import com.kaii.dentix.domain.gingivitis.domain.GingivitisResultType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GingivitisResultTypeTest {

    @ParameterizedTest
    @CsvSource({
            "0,S", "9.9,S",
            "10,G", "39.9,G",
            "40,A", "69.9,A",
            "70,D", "100,D"
    })
    void classifiesRequiredBoundaries(double percent, GingivitisResultType expected) {
        assertThat(GingivitisResultType.fromPercent(percent)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 100.1})
    void rejectsOutOfRangePercent(double percent) {
        assertThatThrownBy(() -> GingivitisResultType.fromPercent(percent))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
