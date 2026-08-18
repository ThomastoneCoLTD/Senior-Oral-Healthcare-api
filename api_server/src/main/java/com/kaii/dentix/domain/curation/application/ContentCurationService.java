package com.kaii.dentix.domain.curation.application;

import com.kaii.dentix.domain.contents.dao.ContentsRepository;
import com.kaii.dentix.domain.contents.domain.Contents;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.curation.dao.ContentCurationRuleRepository;
import com.kaii.dentix.domain.curation.domain.ContentCurationRule;
import com.kaii.dentix.domain.type.AnalysisType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentCurationService {
    private final ContentCurationRuleRepository ruleRepository;
    private final ContentsRepository contentsRepository;

    @Transactional(readOnly = true)
    public List<ContentsDto.Summary> questionnaireContents(List<String> resultKeys) {
        if (resultKeys == null || resultKeys.isEmpty()) return List.of();
        return distinct(ruleRepository.findQuestionnaireRules(AnalysisType.QUESTIONNAIRE, resultKeys), "QUESTIONNAIRE");
    }

    @Transactional(readOnly = true)
    public List<ContentsDto.Summary> gingivitisContents(String resultKey) {
        return distinct(ruleRepository.findRankedRules(AnalysisType.GINGIVITIS, resultKey), "GINGIVITIS");
    }

    @Transactional(readOnly = true)
    public List<ContentsDto.Summary> plaqueContents(String resultKey) {
        return distinct(ruleRepository.findRankedRules(AnalysisType.PLAQUE, resultKey), "PLAQUE");
    }

    @Transactional(readOnly = true)
    public List<ContentsDto.Summary> plaqueContentsByIds(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return List.of();
        Map<Long, Contents> contentsById = new LinkedHashMap<>();
        contentsRepository.findAllById(contentIds)
                .forEach(content -> contentsById.put(content.getContentsId(), content));
        return contentIds.stream()
                .distinct()
                .map(contentsById::get)
                .filter(java.util.Objects::nonNull)
                .map(content -> toSummary(content, "PLAQUE", null))
                .toList();
    }

    public List<Long> questionnaireContentIds(List<String> resultKeys) {
        return questionnaireContents(resultKeys).stream().map(ContentsDto.Summary::getId).toList();
    }

    public List<Long> gingivitisContentIds(String resultKey) {
        return gingivitisContents(resultKey).stream().map(ContentsDto.Summary::getId).toList();
    }

    public List<Long> plaqueContentIds(String resultKey) {
        return plaqueContents(resultKey).stream().map(ContentsDto.Summary::getId).toList();
    }

    private List<ContentsDto.Summary> distinct(List<ContentCurationRule> rules, String source) {
        LinkedHashMap<Long, ContentsDto.Summary> byContentId = new LinkedHashMap<>();
        for (ContentCurationRule rule : rules) {
            Contents content = rule.getContents();
            byContentId.computeIfAbsent(content.getContentsId(), ignored -> toSummary(content, source, rule.getRank()));
        }
        return List.copyOf(byContentId.values());
    }

    private ContentsDto.Summary toSummary(Contents content, String source, Integer rank) {
        return ContentsDto.Summary.builder()
                .id(content.getContentsId())
                .title(content.getContentsTitle())
                .sort(content.getContentsSort())
                .type(content.getContentsType())
                .typeColor(content.getContentsTypeColor())
                .thumbnail(content.getContentsThumbnail())
                .videoURL(content.getContentsPath())
                .categoryIds(content.getContentsToCategories() == null ? List.of() : content.getContentsToCategories().stream()
                        .map(category -> category.getContentsCategoryId())
                        .toList())
                .personalized(true)
                .personalizationSource(source)
                .rank(rank)
                .build();
    }
}
