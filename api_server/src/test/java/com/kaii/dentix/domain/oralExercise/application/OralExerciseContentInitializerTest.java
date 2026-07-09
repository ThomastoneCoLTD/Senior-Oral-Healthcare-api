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
    void seedUsesUploadedVideoForEveryChapter() {
        when(oralExerciseContentRepository.findByContentSort(anyInt())).thenReturn(Optional.empty());

        initializer.seedOralExerciseContents();

        ArgumentCaptor<OralExerciseContent> contentCaptor = ArgumentCaptor.forClass(OralExerciseContent.class);
        verify(oralExerciseContentRepository, times(12)).save(contentCaptor.capture());

        List<OralExerciseContent> savedContents = contentCaptor.getAllValues();
        OralExerciseContent introContent = findBySort(savedContents, 1);

        assertThat(introContent.getLevel()).isEqualTo("INTRO");
        assertThat(introContent.getTitle()).isEqualTo("Chapter 1. 인트로");

        assertThat(savedContents)
                .filteredOn(content -> content.getContentSort() >= 7)
                .allSatisfy(content -> {
                    assertThat(content.getVideoUrl()).contains(content.getContentSort() + "%ED%99%94");
                    assertThat(content.getLevel()).isEqualTo("선택");
                    assertThat(content.isActive()).isTrue();
                });
        assertThat(findBySort(savedContents, 7).getVideoUrl()).contains("%EA%B5%AC%EA%B0%95%EA%B1%B4%EC%A1%B0%EC%A6%9D");
        assertThat(findBySort(savedContents, 12).getVideoUrl()).contains("%EC%8A%B5%EA%B4%80");
    }

    private OralExerciseContent findBySort(List<OralExerciseContent> contents, int sort) {
        return contents.stream()
                .filter(content -> content.getContentSort() == sort)
                .findFirst()
                .orElseThrow();
    }
}
