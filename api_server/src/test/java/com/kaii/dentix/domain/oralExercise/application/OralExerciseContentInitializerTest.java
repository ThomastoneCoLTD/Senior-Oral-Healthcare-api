package com.kaii.dentix.domain.oralExercise.application;

import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OralExerciseContentInitializerTest {

    private OralExerciseContentRepository oralExerciseContentRepository;
    private OralExerciseContentInitializer initializer;

    @BeforeEach
    void setUp() {
        oralExerciseContentRepository = mock(OralExerciseContentRepository.class);
        initializer = new OralExerciseContentInitializer(oralExerciseContentRepository);
    }

    @Test
    void seedUsesUploadedVideosThumbnailsAndDurationsForEveryChapter() {
        when(oralExerciseContentRepository.findByContentSort(anyInt())).thenReturn(Optional.empty());

        initializer.seedOralExerciseContents();

        ArgumentCaptor<OralExerciseContent> contentCaptor = ArgumentCaptor.forClass(OralExerciseContent.class);
        verify(oralExerciseContentRepository, times(12)).save(contentCaptor.capture());

        List<OralExerciseContent> savedContents = contentCaptor.getAllValues();
        OralExerciseContent introContent = findBySort(savedContents, 1);

        assertThat(introContent.getLevel()).isEqualTo("INTRO");
        assertThat(introContent.getThumbnailUrl()).endsWith("optional_video_1.png");
        assertThat(findBySort(savedContents, 2).getThumbnailUrl()).endsWith("essential_video_1.png");
        assertThat(findBySort(savedContents, 6).getThumbnailUrl()).endsWith("essential_video_5.png");
        assertThat(findBySort(savedContents, 7).getThumbnailUrl()).endsWith("optional_video_2.png");
        assertThat(findBySort(savedContents, 12).getThumbnailUrl()).endsWith("optional_video_7.png");

        assertThat(savedContents)
                .allSatisfy(content -> {
                    assertThat(content.getVideoUrl()).contains(content.getContentSort() + "%ED%99%94");
                    assertThat(content.getVideoUrl()).contains("tms-static-hosting.s3.ap-northeast-2.amazonaws.com/oral-exercise/video/");
                    assertThat(content.getThumbnailUrl()).contains("tms-static-hosting.s3.ap-northeast-2.amazonaws.com/oral-exercise/video-thumbnails/");
                    assertThat(content.getThumbnailUrl()).endsWith(".png");
                    assertThat(content.isActive()).isTrue();
                });

        assertThat(findBySort(savedContents, 7).getVideoUrl())
                .contains("%EA%B5%AC%EA%B0%95%EA%B1%B4%EC%A1%B0%EC%A6%9D");
        assertThat(findBySort(savedContents, 8).getVideoUrl())
                .contains("%EC%9D%98%EC%B9%98%EA%B4%80%EB%A6%AC%EB%B2%95");
        assertThat(findBySort(savedContents, 11).getVideoUrl())
                .contains("%EC%82%BC%ED%82%B4%20%EA%B1%B4%EA%B0%95");
        assertThat(findBySort(savedContents, 12).getVideoUrl())
                .contains("%EC%A0%95%EA%B8%B0%EA%B2%80%EC%A7%84%EA%B3%BC");

        assertThat(findBySort(savedContents, 1).getDurationSeconds()).isEqualTo(114);
        assertThat(findBySort(savedContents, 2).getDurationSeconds()).isEqualTo(212);
        assertThat(findBySort(savedContents, 3).getDurationSeconds()).isEqualTo(176);
        assertThat(findBySort(savedContents, 4).getDurationSeconds()).isEqualTo(172);
        assertThat(findBySort(savedContents, 5).getDurationSeconds()).isEqualTo(428);
        assertThat(findBySort(savedContents, 6).getDurationSeconds()).isEqualTo(232);
        assertThat(findBySort(savedContents, 7).getDurationSeconds()).isEqualTo(176);
        assertThat(findBySort(savedContents, 8).getDurationSeconds()).isEqualTo(171);
        assertThat(findBySort(savedContents, 9).getDurationSeconds()).isEqualTo(163);
        assertThat(findBySort(savedContents, 10).getDurationSeconds()).isEqualTo(133);
        assertThat(findBySort(savedContents, 11).getDurationSeconds()).isEqualTo(172);
        assertThat(findBySort(savedContents, 12).getDurationSeconds()).isEqualTo(167);
    }

    private OralExerciseContent findBySort(List<OralExerciseContent> contents, int sort) {
        return contents.stream()
                .filter(content -> content.getContentSort() == sort)
                .findFirst()
                .orElseThrow();
    }
}
