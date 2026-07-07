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
    void seedUsesSelectionOneVideoForAllExtraContentsUntilRealVideosAreReady() {
        when(oralExerciseContentRepository.findByContentSort(anyInt())).thenReturn(Optional.empty());

        initializer.seedOralExerciseContents();

        ArgumentCaptor<OralExerciseContent> contentCaptor = ArgumentCaptor.forClass(OralExerciseContent.class);
        verify(oralExerciseContentRepository, times(12)).save(contentCaptor.capture());

        List<OralExerciseContent> savedContents = contentCaptor.getAllValues();
        OralExerciseContent selectionOneContent = findBySort(savedContents, 6);

        assertThat(savedContents)
                .filteredOn(content -> content.getContentSort() >= 6)
                .allSatisfy(content -> {
                    assertThat(content.getVideoUrl()).isEqualTo(selectionOneContent.getVideoUrl());
                    assertThat(content.getDurationSeconds()).isEqualTo(selectionOneContent.getDurationSeconds());
                    assertThat(content.getLevel()).isEqualTo("선택");
                    assertThat(content.isActive()).isTrue();
                });
    }

    private OralExerciseContent findBySort(List<OralExerciseContent> contents, int sort) {
        return contents.stream()
                .filter(content -> content.getContentSort() == sort)
                .findFirst()
                .orElseThrow();
    }
}
