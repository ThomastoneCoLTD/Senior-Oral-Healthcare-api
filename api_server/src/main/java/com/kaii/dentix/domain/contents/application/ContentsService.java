package com.kaii.dentix.domain.contents.application;

import com.kaii.dentix.domain.contents.dao.ContentsCardRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCategoryRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCustomRepository;
import com.kaii.dentix.domain.contents.dao.ContentsRepository;
import com.kaii.dentix.domain.contents.domain.Contents;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.curation.application.ContentCurationService;
import com.kaii.dentix.domain.gingivitis.domain.GingivitisResultType;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.oralStatusAssignment.dao.OralStatusAssignmentRepository;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentsService {

    private final ContentsCategoryRepository contentsCategoryRepository;
    private final ContentsRepository contentsRepository;
    private final ContentsCardRepository contentsCardRepository;
    private final ContentsCustomRepository contentsCustomRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final OralCheckRepository oralCheckRepository;
    private final OralStatusAssignmentRepository oralStatusAssignmentRepository;
    private final ContentCurationService contentCurationService;
    /**
     * 콘텐츠 카테고리 목록 생성 (내부 헬퍼 메서드)
     */
    private List<ContentsDto.Category> getCategoryList(String userName, boolean includePersonalizedCategory) {
        // 1. 기본 카테고리 조회
        List<ContentsDto.Category> categoryList = contentsCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "contentsCategorySort"))
                .stream()
                .map(c -> ContentsDto.Category.builder()
                        .id(c.getContentsCategoryId())
                        .sort(c.getContentsCategorySort())
                        .name(c.getContentsCategoryName())
                        .color(c.getContentsCategoryColor())
                        .build()
                ).toList();

        List<ContentsDto.Category> resultList = new ArrayList<>(categoryList);

        // 2. 사용자 맞춤 카테고리 추가 (인증된 사용자일 경우)
        if (includePersonalizedCategory && StringUtils.isNotBlank(userName)) {
            String displayName = (userName.length() > 6 ? userName.substring(0, 6) + "・・・" : userName) + "님 맞춤";

            ContentsDto.Category userCategory = ContentsDto.Category.builder()
                    .id(0) // 맞춤 카테고리 ID는 0으로 고정
                    .sort(1)
                    .name(displayName)
                    .color(null)
                    .build();

            resultList.add(0, userCategory);

            // 순서 재정렬
            for (int i = 0; i < resultList.size(); i++) {
                resultList.get(i).setSort(i + 1);
            }
        }

        return resultList;
    }

    /**
     * 콘텐츠 목록 조회
     */
    @Transactional(readOnly = true)
    public ContentsDto.ListResponse getContentsList(User user) {

        boolean isAuthenticatedUser = user != null;
        String userName = isAuthenticatedUser ? user.getUserName() : null;

        // 2. 전체 콘텐츠 리스트 조회
        List<ContentsDto.Summary> allContents = contentsCustomRepository.getContents();
        allContents.forEach(content -> content.setCategoryIds(
                new ArrayList<>(content.getCategoryIds() == null ? List.of() : content.getCategoryIds())
        ));
        Map<Long, ContentsDto.Summary> contentsById = allContents.stream()
                .collect(Collectors.toMap(
                        ContentsDto.Summary::getId,
                        content -> content,
                        (left, ignored) -> left,
                        LinkedHashMap::new
                ));
        List<ContentsDto.PersonalizedSection> sections = isAuthenticatedUser
                ? buildPersonalizedSections(user, contentsById)
                : List.of();
        LinkedHashSet<Long> customizedIds = new LinkedHashSet<>();

        // 3. 맞춤 콘텐츠 태깅 (SOH는 구독 플랜과 관계없이 모든 로그인 사용자에게 제공)
        if (isAuthenticatedUser) {
            sections.forEach(section -> section.getContents().forEach(content -> {
                customizedIds.add(content.getId());
                if (!content.getCategoryIds().contains(0)) {
                    content.getCategoryIds().add(0, 0);
                }
                content.setPersonalized(true);
                String existingSource = content.getPersonalizationSource();
                if (StringUtils.isBlank(existingSource)) {
                    content.setPersonalizationSource(section.getSource());
                } else if (!List.of(existingSource.split(",")).contains(section.getSource())) {
                    content.setPersonalizationSource(existingSource + "," + section.getSource());
                }
            }));
        }

        // 1. 카테고리 리스트 준비
        List<ContentsDto.Category> categoryList = this.getCategoryList(userName, isAuthenticatedUser);
        List<ContentsDto.MenuTab> menuTabs = getMenuTabs(isAuthenticatedUser);

        return ContentsDto.ListResponse.builder()
                .menuTabs(menuTabs)
                .categories(categoryList)
                .contents(allContents)
                .sections(sections)
                .build();
    }

    private List<ContentsDto.PersonalizedSection> buildPersonalizedSections(
            User user,
            Map<Long, ContentsDto.Summary> contentsById
    ) {
        List<OralCheck> successfulAnalyses = oralCheckRepository
                .findAllByUser_UserIdOrderByCreatedDesc(user.getUserId())
                .stream()
                .filter(item -> item.getOralCheckAnalysisState() == OralCheckAnalysisState.SUCCESS)
                .toList();
        return List.of(
                buildOralAnalysisSection(successfulAnalyses, false, contentsById),
                buildOralAnalysisSection(successfulAnalyses, true, contentsById),
                buildQuestionnaireSection(user, contentsById)
        );
    }

    private ContentsDto.PersonalizedSection buildOralAnalysisSection(
            List<OralCheck> analyses,
            boolean gingivitis,
            Map<Long, ContentsDto.Summary> contentsById
    ) {
        String source = gingivitis ? "GINGIVITIS" : "PLAQUE";
        String actionPath = gingivitis ? "/user/oralCheck/gingivitis" : "/user/oralCheck/plaque";
        try {
            Optional<OralCheck> latest = analyses.stream()
                    .filter(item -> isGingivitis(item) == gingivitis)
                    .findFirst();
            if (latest.isEmpty()) {
                return emptySection(source, actionPath);
            }

            OralCheck oralCheck = latest.get();
            String resultKey = gingivitis
                    ? GingivitisResultType.fromPercent(oralCheck.getOralCheckTotalRange() == null
                            ? 0 : oralCheck.getOralCheckTotalRange()).name()
                    : plaqueResultKey(oralCheck);
            List<Long> contentIds;
            if (gingivitis) {
                contentIds = contentCurationService.gingivitisContentIds(resultKey);
            } else {
                contentIds = plaqueContentIds(oralCheck);
                if (contentIds.isEmpty()) {
                    List<String> resultKeys = oralStatusAssignmentRepository
                            .findOralStatusTypesByOralCheckId(oralCheck.getOralCheckId());
                    contentIds = contentCurationService.questionnaireContentIds(resultKeys);
                }
                if (contentIds.isEmpty() && resultKey != null) {
                    contentIds = contentCurationService.plaqueContentIds(resultKey);
                }
            }
            return completedSection(
                    source,
                    actionPath,
                    oralCheck.getOralCheckId(),
                    resultKey,
                    oralCheck.getCreated(),
                    orderedVisibleContents(contentIds, contentsById)
            );
        } catch (Exception exception) {
            log.warn("{} 기반 맞춤 콘텐츠 조회에 실패했습니다.", source, exception);
            return emptySection(source, actionPath);
        }
    }

    private ContentsDto.PersonalizedSection buildQuestionnaireSection(
            User user,
            Map<Long, ContentsDto.Summary> contentsById
    ) {
        try {
            Optional<Questionnaire> latest = questionnaireRepository.findTopByUserIdOrderByCreatedDesc(user.getUserId());
            if (latest.isEmpty()) {
                return emptySection("QUESTIONNAIRE", "/user/questionnaire");
            }
            Questionnaire questionnaire = latest.get();
            List<String> resultKeys = oralStatusAssignmentRepository
                    .findOralStatusTypesByQuestionnaireId(questionnaire.getQuestionnaireId());
            List<Long> contentIds = contentCurationService.questionnaireContentIds(resultKeys);
            if (contentIds.isEmpty()) {
                contentIds = contentsCustomRepository.getCustomizedContentsIdList(questionnaire.getQuestionnaireId());
            }
            return completedSection(
                    "QUESTIONNAIRE",
                    "/user/questionnaire",
                    questionnaire.getQuestionnaireId(),
                    String.join(",", resultKeys),
                    questionnaire.getCreated(),
                    orderedVisibleContents(contentIds, contentsById)
            );
        } catch (Exception exception) {
            log.warn("QUESTIONNAIRE 기반 맞춤 콘텐츠 조회에 실패했습니다.", exception);
            return emptySection("QUESTIONNAIRE", "/user/questionnaire");
        }
    }

    private boolean isGingivitis(OralCheck oralCheck) {
        String json = oralCheck.getOralCheckResultJsonData();
        return json != null && (json.contains("\"ging_check\"") || json.contains("\"gingCheck\""));
    }

    private List<Long> plaqueContentIds(OralCheck oralCheck) {
        String json = oralCheck.getOralCheckResultJsonData();
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        int keyIndex = json.indexOf("\"plaque_contents\"");
        if (keyIndex < 0) {
            keyIndex = json.indexOf("\"plaqueContents\"");
        }
        if (keyIndex < 0) {
            return List.of();
        }
        int arrayStart = json.indexOf('[', keyIndex);
        int arrayEnd = arrayStart < 0 ? -1 : json.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String value : json.substring(arrayStart + 1, arrayEnd).split(",")) {
            try {
                long id = Long.parseLong(value.trim());
                if (id > 0) result.add(id);
            } catch (NumberFormatException ignored) {
                // AI 응답의 숫자 콘텐츠 ID만 사용한다.
            }
        }
        return result.stream().distinct().toList();
    }

    private String plaqueResultKey(OralCheck oralCheck) {
        return oralCheck.getOralCheckResultTotalType() == null
                ? null
                : oralCheck.getOralCheckResultTotalType().name();
    }

    private List<ContentsDto.Summary> orderedVisibleContents(
            List<Long> contentIds,
            Map<Long, ContentsDto.Summary> contentsById
    ) {
        List<ContentsDto.Summary> result = contentIds.stream()
                .distinct()
                .map(contentsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        for (int index = 0; index < result.size(); index++) {
            result.get(index).setRank(index + 1);
        }
        return result;
    }

    private ContentsDto.PersonalizedSection emptySection(String source, String actionPath) {
        return ContentsDto.PersonalizedSection.builder()
                .source(source)
                .enabled(true)
                .completed(false)
                .actionPath(actionPath)
                .contents(List.of())
                .build();
    }

    private ContentsDto.PersonalizedSection completedSection(
            String source,
            String actionPath,
            Long resultId,
            String resultKey,
            java.util.Date created,
            List<ContentsDto.Summary> contents
    ) {
        return ContentsDto.PersonalizedSection.builder()
                .source(source)
                .enabled(true)
                .completed(true)
                .resultId(resultId)
                .resultKey(resultKey)
                .created(created)
                .actionPath(actionPath)
                .contents(contents)
                .build();
    }

    private List<ContentsDto.MenuTab> getMenuTabs(boolean includePersonalizedTab) {
        List<ContentsDto.MenuTab> menuTabs = new ArrayList<>();

        if (includePersonalizedTab) {
            menuTabs.add(ContentsDto.MenuTab.builder()
                    .id("PERSONALIZED")
                    .name("맞춤 콘텐츠")
                    .build());
        }

        menuTabs.add(ContentsDto.MenuTab.builder()
                .id("ALL")
                .name("모든 콘텐츠")
                .build());

        return menuTabs;
    }

    /**
     * 콘텐츠 카드뉴스 상세 조회
     */
    @Transactional(readOnly = true)
    public ContentsDto.CardListResponse getContentsCard(Long contentsId) {
        Contents contents = contentsRepository.findById(contentsId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 콘텐츠입니다."));

        List<ContentsDto.Card> cards = contentsCardRepository.findAllByContents_ContentsId(contents.getContentsId())
                .stream()
                .map(c -> ContentsDto.Card.builder()
                        .number(c.getContentsCardNumber())
                        .path(c.getContentsCardPath())
                        .build())
                .toList();

        return ContentsDto.CardListResponse.builder()
                .title(contents.getContentsTitle())
                .cardList(cards)
                .build();
    }
}
