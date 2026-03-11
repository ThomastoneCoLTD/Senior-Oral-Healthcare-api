package com.kaii.dentix.domain.contents;

import com.kaii.dentix.domain.contents.application.ContentsService;
import com.kaii.dentix.domain.contents.dao.ContentsCardRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCategoryRepository;
import com.kaii.dentix.domain.contents.dao.ContentsCustomRepository;
import com.kaii.dentix.domain.contents.dao.ContentsRepository;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.type.ContentsType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentsServiceTest {

    @Mock
    private ContentsCategoryRepository contentsCategoryRepository;

    @Mock
    private ContentsRepository contentsRepository;

    @Mock
    private ContentsCardRepository contentsCardRepository;

    @Mock
    private ContentsCustomRepository contentsCustomRepository;

    @Mock
    private QuestionnaireRepository questionnaireRepository;

    @Mock
    private OralCheckRepository oralCheckRepository;

    @InjectMocks
    private ContentsService contentsService;

    @Test
    void hidesPersonalizedCategoryWhenVerifiedUserHasNoQuestionnaireAndNoOralCheck() {
        User user = User.builder()
                .userId(1L)
                .userName("김덴티")
                .isVerify(YnType.Y)
                .build();

        when(contentsCategoryRepository.findAll(any(Sort.class))).thenReturn(List.of(
                com.kaii.dentix.domain.contents.domain.ContentsCategory.builder()
                        .contentsCategoryId(1)
                        .contentsCategoryName("질병")
                        .contentsCategoryColor("#98B4ED")
                        .contentsCategorySort(1)
                        .build()
        ));
        when(contentsCustomRepository.getContents()).thenReturn(List.of(
                ContentsDto.Summary.builder()
                        .id(1L)
                        .title("콘텐츠")
                        .sort(1)
                        .type(ContentsType.CARD)
                        .typeColor("#FF9F06")
                        .thumbnail("thumb")
                        .videoURL(null)
                        .categoryIds(List.of(1))
                        .build()
        ));
        when(questionnaireRepository.findTopByUserIdOrderByCreatedDesc(1L)).thenReturn(Optional.empty());
        when(oralCheckRepository.findTopByUser_UserIdOrderByCreatedDesc(1L)).thenReturn(Optional.empty());

        ContentsDto.ListResponse response = contentsService.getContentsList(user);

        assertThat(response.getCategories()).extracting(ContentsDto.Category::getId)
                .doesNotContain(0);
        assertThat(response.getContents()).allSatisfy(content ->
                assertThat(content.getCategoryIds()).doesNotContain(0));
    }
}
