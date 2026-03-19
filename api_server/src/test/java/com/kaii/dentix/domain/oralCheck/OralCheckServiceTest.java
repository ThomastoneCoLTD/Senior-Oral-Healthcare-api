package com.kaii.dentix.domain.oralCheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.billing.application.BillingService;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.oralStatus.jpa.OralStatusRepository;
import com.kaii.dentix.domain.oralStatusAssignment.dao.OralStatusAssignmentRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.application.OrganizationSubscriptionHistoryService;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireCustomRepository;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingCustomRepository;
import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingRepository;
import com.kaii.dentix.domain.toothBrushing.domain.ToothBrushing;
import com.kaii.dentix.domain.type.OralDateStatusType;
import com.kaii.dentix.domain.type.OralSectionType;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.aws.AWSS3Service;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.kaii.dentix.global.common.util.Utils;
import com.kaii.dentix.global.common.util.AiModelService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OralCheckServiceTest {

    @Mock private UserService userService;
    @Mock private AWSS3Service awss3Service;
    @Mock private ObjectMapper objectMapper;
    @Mock private AiModelService aiModelService;
    @Mock private BillingService billingService;
    @Mock private UserRepository userRepository;
    @Mock private OralCheckRepository oralCheckRepository;
    @Mock private OralStatusRepository oralStatusRepository;
    @Mock private ToothBrushingRepository toothBrushingRepository;
    @Mock private QuestionnaireRepository questionnaireRepository;
    @Mock private OralStatusAssignmentRepository oralStatusAssignmentRepository;
    @Mock private ToothBrushingCustomRepository toothBrushingCustomRepository;
    @Mock private QuestionnaireCustomRepository questionnaireCustomRepository;
    @Mock private OrganizationSubscriptionHistoryService organizationSubscriptionHistoryService;

    @InjectMocks
    private OralCheckService oralCheckService;

    @Test
    void oralCheck_includesOlderOralCheckHistoryAndExplicitOralCheckId() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = User.builder()
                .userId(1L)
                .organization(Organization.builder().organizationId(10L).organizationName("테스트기관").build())
                .build();

        Calendar calendar = Calendar.getInstance();
        Date recentDate = calendar.getTime();
        calendar.add(Calendar.DATE, -45);
        Date oldDate = calendar.getTime();

        OralCheck recentOralCheck = OralCheck.builder()
                .oralCheckId(200L)
                .user(user)
                .oralCheckAnalysisState(OralCheckAnalysisState.SUCCESS)
                .oralCheckResultTotalType(OralCheckResultType.GOOD)
                .build();
        recentOralCheck.setCreated(recentDate);

        OralCheck oldOralCheck = OralCheck.builder()
                .oralCheckId(100L)
                .user(user)
                .oralCheckAnalysisState(OralCheckAnalysisState.SUCCESS)
                .oralCheckResultTotalType(OralCheckResultType.ATTENTION)
                .build();
        oldOralCheck.setCreated(oldDate);

        given(userService.getTokenUser(request)).willReturn(user);
        given(oralCheckRepository.findAllByUser_UserIdOrderByCreatedDesc(1L)).willReturn(List.of(recentOralCheck, oldOralCheck));
        given(toothBrushingRepository.findAllByUserIdOrderByCreatedDesc(1L)).willReturn(Collections.<ToothBrushing>emptyList());
        given(questionnaireRepository.findAllByUserIdOrderByCreatedDesc(1L)).willReturn(Collections.<Questionnaire>emptyList());
        OralCheckDto.TimelineResponse response = oralCheckService.oralCheck(request);

        String oldDateString = DateFormatUtil.dateToString("yyyy-MM-dd", oldDate);
        OralCheckDto.Detail oldDetail = response.getDailyList().stream()
                .filter(daily -> DateFormatUtil.dateToString("yyyy-MM-dd", daily.getDate()).equals(oldDateString))
                .flatMap(daily -> daily.getDetailList().stream())
                .filter(detail -> detail.getOralCheckId() != null && detail.getOralCheckId().equals(100L))
                .findFirst()
                .orElse(null);

        assertThat(oldDetail).isNotNull();
        assertThat(oldDetail.getIdentifier()).isEqualTo(100L);
        assertThat(oldDetail.getOralCheckId()).isEqualTo(100L);
    }

    @Test
    void oralCheck_skipsFailedOralCheckWithoutResultTypeWhenBuildingDailyStatus() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = User.builder()
                .userId(1L)
                .organization(Organization.builder().organizationId(10L).organizationName("테스트기관").build())
                .build();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        Date targetDate = calendar.getTime();

        OralCheck failedOralCheck = OralCheck.builder()
                .oralCheckId(300L)
                .user(user)
                .oralCheckAnalysisState(OralCheckAnalysisState.FAIL)
                .build();
        failedOralCheck.setCreated(targetDate);

        Questionnaire questionnaire = Questionnaire.builder()
                .questionnaireId(400L)
                .userId(1L)
                .questionnaireVersion("v1")
                .form("{}")
                .build();
        questionnaire.setCreated(targetDate);

        given(userService.getTokenUser(request)).willReturn(user);
        given(oralCheckRepository.findAllByUser_UserIdOrderByCreatedDesc(1L)).willReturn(List.of(failedOralCheck));
        given(toothBrushingRepository.findAllByUserIdOrderByCreatedDesc(1L)).willReturn(Collections.<ToothBrushing>emptyList());
        given(questionnaireRepository.findAllByUserIdOrderByCreatedDesc(1L)).willReturn(List.of(questionnaire));
        given(oralStatusAssignmentRepository.findQuestionnaireOralStatuses(List.of(questionnaire.getQuestionnaireId())))
                .willReturn(List.of(new OralStatusAssignmentProjection(questionnaire.getQuestionnaireId(), "A")));
        given(oralStatusRepository.findAllByOralStatusTypeInOrderByOralStatusPriority(List.of("A")))
                .willReturn(List.of(OralStatus.builder()
                        .oralStatusType("A")
                        .oralStatusPriority(1)
                        .oralStatusTitle("건강형")
                        .oralStatusDescription("설명")
                        .oralStatusSubDescription("부연 설명")
                        .build()));

        OralCheckDto.TimelineResponse response = oralCheckService.oralCheck(request);

        String targetDateString = DateFormatUtil.dateToString("yyyy-MM-dd", targetDate);
        OralCheckDto.Daily targetDaily = response.getDailyList().stream()
                .filter(daily -> DateFormatUtil.dateToString("yyyy-MM-dd", daily.getDate()).equals(targetDateString))
                .findFirst()
                .orElse(null);

        assertThat(targetDaily).isNotNull();
        assertThat(targetDaily.getStatus()).isEqualTo(OralDateStatusType.QUESTIONNAIRE);
        assertThat(targetDaily.getDetailList()).extracting(OralCheckDto.Detail::getSectionType)
                .containsExactly(OralSectionType.QUESTIONNAIRE);
    }

    @Test
    void oralCheckResult_returnsFailedResponseWhenAnalysisWasNotSuccessful() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = User.builder()
                .userId(1L)
                .organization(Organization.builder().organizationId(10L).organizationName("테스트기관").build())
                .build();

        OralCheck failedOralCheck = OralCheck.builder()
                .oralCheckId(500L)
                .user(user)
                .oralCheckAnalysisState(OralCheckAnalysisState.FAIL)
                .build();
        failedOralCheck.setCreated(new Date());

        given(userService.getTokenUser(request)).willReturn(user);
        given(oralCheckRepository.findById(500L)).willReturn(java.util.Optional.of(failedOralCheck));

        OralCheckDto.ResultResponse response = oralCheckService.oralCheckResult(request, 500L);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCreated()).isEqualTo(failedOralCheck.getCreated());
        assertThat(response.getOralCheckCommentList()).isEmpty();
    }

    private record OralStatusAssignmentProjection(Long questionnaireId, String oralStatusType)
            implements OralStatusAssignmentRepository.QuestionnaireOralStatusProjection {
        @Override
        public Long getQuestionnaireId() {
            return questionnaireId;
        }

        @Override
        public String getOralStatusType() {
            return oralStatusType;
        }
    }
}
