package com.kaii.dentix.domain.contents.application;

import com.kaii.dentix.domain.contents.dao.ContentsCardRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCategoryRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCustomRepository;
import com.kaii.dentix.domain.contents.dao.ContentsRepository;
import com.kaii.dentix.domain.contents.domain.Contents;
import com.kaii.dentix.domain.contents.dto.*;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentsService {

    private final ContentsCategoryRepository contentsCategoryRepository;

    private final ContentsRepository contentsRepository;

    private final UserService userService;

    private final ContentsCardRepository contentsCardRepository;

    private final ContentsCustomRepository contentsCustomRepository;

    private final QuestionnaireRepository questionnaireRepository;


    /**
     *  콘텐츠 카테고리 조회
     */
    public List<ContentsCategoryDto> getCategoryList(String userName) {
        List<ContentsCategoryDto> categoryList = contentsCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "contentsCategorySort")).stream()
            .map(contentsCategory ->
                ContentsCategoryDto.builder()
                    .id(contentsCategory.getContentsCategoryId())
                    .sort(contentsCategory.getContentsCategorySort())
                    .name(contentsCategory.getContentsCategoryName())
                    .color(contentsCategory.getContentsCategoryColor())
                    .build()
            ).toList();

        List<ContentsCategoryDto> userCategoryList = new ArrayList<>(categoryList);

        // 사용자 맞춤 카테고리 추가
        if (StringUtils.isNotBlank(userName)) {
            ContentsCategoryDto userCategory = ContentsCategoryDto.builder()
                .id(0)
                .sort(1)
                .name((userName.length() > 6 ? userName.substring(0, 6) + "・・・" : userName) + "님 맞춤")
                .color(null)
                .build();
            userCategoryList.add(0, userCategory);

            for (int i = 0; i < userCategoryList.size(); i++) {
                userCategoryList.get(i).setSort(i + 1);
            }
        }

        return userCategoryList;
    }

    /**
     *  콘텐츠 조회
     */
    @Transactional
    public ContentsListDto contentsList(HttpServletRequest httpServletRequest){

        User user = userService.getTokenUserNullable(httpServletRequest);
        log.info("user", user);
        boolean isVerifiedUser = user != null && user.getIsVerify().equals(YnType.Y);

        // 카테고리 리스트 (인증된 사용자의 경우 사용자 맞춤 카테고리 추가)
        List<ContentsCategoryDto> categoryList = this.getCategoryList(isVerifiedUser ? user.getUserName() : null);

        // 콘텐츠 리스트
        List<ContentsDto> userContentsList = contentsCustomRepository.getContents();

        // 사용자 맞춤 카테고리 추가 - 인증된 사용자의 경우
        if (isVerifiedUser) {
            Optional<Questionnaire> questionnaireOpt = questionnaireRepository.findTopByUserIdOrderByCreatedDesc(user.getUserId());
            if (questionnaireOpt.isPresent()) {
                List<Integer> customizedContentsIdList = contentsCustomRepository.getCustomizedContentsIdList(questionnaireOpt.get().getQuestionnaireId());
                customizedContentsIdList.forEach(id -> {
                    userContentsList.stream().filter(o -> o.getId() == id).findAny().ifPresent(contentsDto -> {
                        contentsDto.getCategoryIds().add(0, 0);
                    });
                });
            }
        }

        return new ContentsListDto(categoryList, userContentsList);
    }

    /**
     *  콘텐츠 카드뉴스
     */
    @Transactional
    public ContentsCardListDto contentsCard(Long contentsId){
        Contents contents = contentsRepository.findById(contentsId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 콘텐츠입니다."));

        List<ContentsCardDto> contentsCardList = contentsCardRepository.findAllByContentsId(contents.getContentsId()).stream()
                .map(contentsList -> new ContentsCardDto(contentsList.getContentsCardNumber(), contentsList.getContentsCardPath()))
                .collect(Collectors.toList());

        return new ContentsCardListDto(contents.getContentsTitle(), contentsCardList);
    }

}
