package com.kaii.dentix.domain.contents.application;

import com.kaii.dentix.domain.contents.dao.ContentsCardRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCategoryRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCustomRepository;
import com.kaii.dentix.domain.contents.dao.ContentsRepository;
import com.kaii.dentix.domain.contents.domain.Contents;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentsService {

    private final ContentsCategoryRepository contentsCategoryRepository;
    private final ContentsRepository contentsRepository;
    private final ContentsCardRepository contentsCardRepository;
    private final ContentsCustomRepository contentsCustomRepository;
    private final QuestionnaireRepository questionnaireRepository;
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

        boolean isVerifiedUser = (user != null && user.getIsVerify() == YnType.Y);
        String userName = isVerifiedUser ? user.getUserName() : null;

        // 2. 전체 콘텐츠 리스트 조회
        List<ContentsDto.Summary> allContents = contentsCustomRepository.getContents();
        List<Long> customizedIds = new ArrayList<>();

        // 3. 맞춤 콘텐츠 태깅 (인증된 사용자)
        if (isVerifiedUser) {
            Optional<Questionnaire> questionnaireOpt =
                    questionnaireRepository.findTopByUserIdOrderByCreatedDesc(user.getUserId());

            if (questionnaireOpt.isPresent()) {
                customizedIds = contentsCustomRepository.getCustomizedContentsIdList(
                        questionnaireOpt.get().getQuestionnaireId()
                );
            }

            customizedIds.forEach(targetId ->
                    allContents.stream()
                            .filter(c -> c.getId().equals(targetId))
                            .findFirst()
                            .ifPresent(c -> {
                                if (!c.getCategoryIds().contains(0)) {
                                    c.getCategoryIds().add(0, 0);
                                }
                            })
            );
        }

        // 1. 카테고리 리스트 준비
        List<ContentsDto.Category> categoryList = this.getCategoryList(userName, !customizedIds.isEmpty());

        return ContentsDto.ListResponse.builder()
                .categories(categoryList)
                .contents(allContents)
                .build();
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
