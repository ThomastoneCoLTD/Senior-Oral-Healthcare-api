package com.kaii.dentix.domain.curation;

import com.kaii.dentix.domain.contents.dao.ContentsRepository;
import com.kaii.dentix.domain.contents.domain.Contents;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.curation.application.ContentCurationService;
import com.kaii.dentix.domain.curation.dao.ContentCurationRuleRepository;
import com.kaii.dentix.domain.curation.domain.ContentCurationRule;
import com.kaii.dentix.domain.type.AnalysisType;
import com.kaii.dentix.domain.type.ContentsType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentCurationServiceTest {
    @Mock private ContentCurationRuleRepository repository;
    @Mock private ContentsRepository contentsRepository;
    @InjectMocks private ContentCurationService service;

    @Test
    void questionnaireReturnsOrderedUnionWithoutDuplicates() {
        Contents first = content(1L, 1);
        Contents second = content(2L, 2);
        when(repository.findQuestionnaireRules(AnalysisType.QUESTIONNAIRE, List.of("A", "B")))
                .thenReturn(List.of(
                        rule(AnalysisType.QUESTIONNAIRE, "A", first, null),
                        rule(AnalysisType.QUESTIONNAIRE, "B", first, null),
                        rule(AnalysisType.QUESTIONNAIRE, "B", second, null)
                ));

        List<ContentsDto.Summary> result = service.questionnaireContents(List.of("A", "B"));

        assertThat(result).extracting(ContentsDto.Summary::getId).containsExactly(1L, 2L);
        assertThat(result).extracting(ContentsDto.Summary::getPersonalizationSource)
                .containsOnly("QUESTIONNAIRE");
    }

    @Test
    void gingivitisKeepsConfiguredRankOrder() {
        when(repository.findRankedRules(AnalysisType.GINGIVITIS, "D"))
                .thenReturn(List.of(
                        rule(AnalysisType.GINGIVITIS, "D", content(9L, 8), 1),
                        rule(AnalysisType.GINGIVITIS, "D", content(3L, 2), 2)
                ));

        List<ContentsDto.Summary> result = service.gingivitisContents("D");

        assertThat(result).extracting(ContentsDto.Summary::getId).containsExactly(9L, 3L);
        assertThat(result).extracting(ContentsDto.Summary::getRank).containsExactly(1, 2);
    }

    @Test
    void plaqueContentIdsKeepAiResponseOrder() {
        Contents content15 = content(15L, 2);
        Contents content16 = content(16L, 1);
        when(contentsRepository.findAllById(List.of(15L, 16L))).thenReturn(List.of(content16, content15));

        assertThat(service.plaqueContentsByIds(List.of(15L, 16L)))
                .extracting(ContentsDto.Summary::getId)
                .containsExactly(15L, 16L);
    }

    private ContentCurationRule rule(AnalysisType type, String key, Contents contents, Integer rank) {
        return ContentCurationRule.builder()
                .analysisType(type)
                .resultKey(key)
                .contents(contents)
                .rank(rank)
                .active(true)
                .build();
    }

    private Contents content(Long id, int sort) {
        return Contents.builder()
                .contentsId(id)
                .contentsSort(sort)
                .contentsType(ContentsType.CARD)
                .contentsTitle("콘텐츠 " + id)
                .contentsTypeColor("#123456")
                .contentsThumbnail("thumbnail")
                .contentsToCategories(List.of())
                .build();
    }
}
