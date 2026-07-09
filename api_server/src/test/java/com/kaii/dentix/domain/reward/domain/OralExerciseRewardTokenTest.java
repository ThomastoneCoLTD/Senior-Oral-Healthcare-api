package com.kaii.dentix.domain.reward.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OralExerciseRewardTokenTest {

    @Test
    void tokenNameForContentSortMapsIntroCoreAndOptionalVideos() {
        assertThat(OralExerciseRewardToken.tokenNameForContentSort(1)).isEqualTo("optional_video_1");

        assertThat(OralExerciseRewardToken.tokenNameForContentSort(2)).isEqualTo("essential_video_1");
        assertThat(OralExerciseRewardToken.tokenNameForContentSort(6)).isEqualTo("essential_video_5");

        assertThat(OralExerciseRewardToken.tokenNameForContentSort(7)).isEqualTo("optional_video_2");
        assertThat(OralExerciseRewardToken.tokenNameForContentSort(12)).isEqualTo("optional_video_7");
    }
}
