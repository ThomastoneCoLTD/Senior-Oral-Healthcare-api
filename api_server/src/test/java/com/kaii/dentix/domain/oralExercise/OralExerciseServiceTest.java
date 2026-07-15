package com.kaii.dentix.domain.oralExercise;

import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseService;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseInteractionLogRepository;
import com.kaii.dentix.domain.oralExercise.dao.UserOralExerciseProgressRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.oralExercise.dto.OralExerciseDto;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.reward.application.UserRewardService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.aws.AWSS3Service;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OralExerciseServiceTest {

    private OralExerciseContentRepository contentRepository;
    private OralExerciseInteractionLogRepository interactionLogRepository;
    private UserOralExerciseProgressRepository progressRepository;
    private UserRewardTransactionRepository rewardTransactionRepository;
    private UserRewardService userRewardService;
    private UserRepository userRepository;
    private JwtTokenUtil jwtTokenUtil;
    private AWSS3Service awss3Service;
    private OralExerciseService service;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        contentRepository = mock(OralExerciseContentRepository.class);
        interactionLogRepository = mock(OralExerciseInteractionLogRepository.class);
        progressRepository = mock(UserOralExerciseProgressRepository.class);
        rewardTransactionRepository = mock(UserRewardTransactionRepository.class);
        userRewardService = mock(UserRewardService.class);
        userRepository = mock(UserRepository.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        awss3Service = mock(AWSS3Service.class);
        request = mock(HttpServletRequest.class);

        service = new OralExerciseService(
                contentRepository,
                interactionLogRepository,
                progressRepository,
                rewardTransactionRepository,
                userRewardService,
                userRepository,
                jwtTokenUtil,
                awss3Service
        );

        when(jwtTokenUtil.getAccessToken(request)).thenReturn("access-token");
        when(jwtTokenUtil.isExpired("access-token", TokenType.AccessToken)).thenReturn(false);
        when(jwtTokenUtil.getUserId("access-token", TokenType.AccessToken)).thenReturn(7L);
        when(progressRepository.findByUserId(7L)).thenReturn(List.of());
    }

    @Test
    void getContentsTemporarilyUnlocksAllCoreContentsForTestingAndKeepsIntroOpen() {
        User user = userCreatedDaysAgo(21);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(rewardTransactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of());
        when(contentRepository.findByActiveTrueOrderByContentSortAsc()).thenReturn(List.of(
                content(1),
                content(2),
                content(3),
                content(4),
                content(5),
                content(6)
        ));

        OralExerciseDto.ListResponse response = service.getContents(request);

        assertThat(response.getCurrentWeek()).isEqualTo(4);
        assertThat(response.getCurrentContent().getSort()).isEqualTo(5);
        assertThat(response.getPreviousContents()).extracting(OralExerciseDto.ContentResponse::getSort)
                .containsExactly(2, 3, 4);
        assertThat(response.getContents()).filteredOn(content -> content.getSort() == 6)
                .singleElement()
                .satisfies(content -> assertThat(content.isAvailable()).isTrue());
        assertThat(response.getExtraContents()).extracting(OralExerciseDto.ContentResponse::getSort)
                .containsExactly(1);
        assertThat(response.getExtraContents()).allSatisfy(content -> {
            assertThat(content.getWeek()).isEqualTo(0);
            assertThat(content.isAvailable()).isTrue();
            assertThat(content.isCurrentWeekContent()).isTrue();
        });
    }

    @Test
    void getContentsKeepsAllExtraContentsUnlockedFromFirstWeek() {
        User user = userCreatedDaysAgo(0);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(rewardTransactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of());
        when(contentRepository.findByActiveTrueOrderByContentSortAsc()).thenReturn(List.of(
                content(1),
                content(2),
                content(6),
                content(7),
                content(8)
        ));

        OralExerciseDto.ListResponse response = service.getContents(request);

        assertThat(response.getCurrentWeek()).isEqualTo(1);
        assertThat(response.getCurrentContent().getSort()).isEqualTo(2);
        assertThat(response.getPreviousContents()).isEmpty();
        assertThat(response.getExtraContents()).extracting(OralExerciseDto.ContentResponse::getSort)
                .containsExactly(1, 7, 8);
        assertThat(response.getExtraContents()).allSatisfy(content -> {
            assertThat(content.getWeek()).isEqualTo(0);
            assertThat(content.isAvailable()).isTrue();
            assertThat(content.isCurrentWeekContent()).isTrue();
        });
    }

    @Test
    void getContentsPresignsVideoAndThumbnailAssets() {
        User user = userCreatedDaysAgo(0);
        OralExerciseContent content = content(
                7,
                "https://denti-backends.s3.ap-northeast-2.amazonaws.com/soh/video/sample.mp4",
                "https://denti-backends.s3.ap-northeast-2.amazonaws.com/soh/video-thumbnails/optional_video_2.png"
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(rewardTransactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of());
        when(contentRepository.findByActiveTrueOrderByContentSortAsc()).thenReturn(List.of(content));
        when(awss3Service.getPresignedUrl("soh/video/sample.mp4", 180)).thenReturn("signed-video-url");
        when(awss3Service.getPresignedUrl("soh/video-thumbnails/optional_video_2.png", 180))
                .thenReturn("signed-thumbnail-url");

        OralExerciseDto.ListResponse response = service.getContents(request);

        OralExerciseDto.ContentResponse contentResponse = response.getContents().get(0);
        assertThat(contentResponse.getVideoUrl()).isEqualTo("signed-video-url");
        assertThat(contentResponse.getThumbnailUrl()).isEqualTo("signed-thumbnail-url");
    }

    @Test
    void getContentsConvertsTmsStaticHostingS3UrisToHttpsUrls() {
        User user = userCreatedDaysAgo(0);
        OralExerciseContent content = content(
                7,
                "s3://tms-static-hosting/oral-exercise/sample.mp4",
                "s3://tms-static-hosting/oral-exercise/optional_video_2.png"
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(rewardTransactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of());
        when(contentRepository.findByActiveTrueOrderByContentSortAsc()).thenReturn(List.of(content));

        OralExerciseDto.ListResponse response = service.getContents(request);

        OralExerciseDto.ContentResponse contentResponse = response.getContents().get(0);
        assertThat(contentResponse.getVideoUrl())
                .isEqualTo("https://tms-static-hosting.s3.ap-northeast-2.amazonaws.com/oral-exercise/sample.mp4");
        assertThat(contentResponse.getThumbnailUrl())
                .isEqualTo("https://tms-static-hosting.s3.ap-northeast-2.amazonaws.com/oral-exercise/optional_video_2.png");
    }

    @Test
    void getContentsMarksButtonRewardAsAlreadyReceivedPerRewardTokenName() {
        User user = userCreatedDaysAgo(0);
        OralExerciseContent firstContent = content(2);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(contentRepository.findByActiveTrueOrderByContentSortAsc()).thenReturn(List.of(firstContent));
        when(rewardTransactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of(
                UserRewardTransaction.builder()
                        .userId(7L)
                        .oralExerciseContent(firstContent)
                        .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                        .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                        .amount(3L)
                        .balanceAfter(3L)
                        .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                        .coinId("essential_video_1")
                        .build()
        ));

        OralExerciseDto.ListResponse response = service.getContents(request);

        OralExerciseDto.ContentResponse content = response.getContents().get(0);
        assertThat(content.isRewardReceived()).isTrue();
        assertThat(content.getButtonChallenge().isRewardAvailable()).isFalse();
    }

    @Test
    void getContentsKeepsRewardReceivedAfterTokenReclaim() {
        User user = userCreatedDaysAgo(0);
        OralExerciseContent firstContent = content(2);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(contentRepository.findByActiveTrueOrderByContentSortAsc()).thenReturn(List.of(firstContent));
        when(rewardTransactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of(
                UserRewardTransaction.builder()
                        .userId(7L)
                        .oralExerciseContent(firstContent)
                        .type(UserRewardTransactionType.ORAL_EXERCISE_RECLAIM)
                        .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                        .amount(3L)
                        .balanceAfter(3L)
                        .idempotencyKey("ORAL_EXERCISE_RECLAIM:7:1")
                        .coinId("essential_video_1")
                        .build(),
                UserRewardTransaction.builder()
                        .userId(7L)
                        .oralExerciseContent(firstContent)
                        .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                        .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                        .amount(3L)
                        .balanceAfter(3L)
                        .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                        .coinId("essential_video_1")
                        .build()
        ));

        OralExerciseDto.ListResponse response = service.getContents(request);

        OralExerciseDto.ContentResponse content = response.getContents().get(0);
        assertThat(content.isRewardReceived()).isTrue();
        assertThat(content.getButtonChallenge().isRewardAvailable()).isFalse();
    }

    @Test
    void getContentsKeepsButtonRewardAvailableWhenTokenTransferFailed() {
        User user = userCreatedDaysAgo(0);
        OralExerciseContent firstContent = content(2);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(contentRepository.findByActiveTrueOrderByContentSortAsc()).thenReturn(List.of(firstContent));
        when(rewardTransactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of(
                UserRewardTransaction.builder()
                        .userId(7L)
                        .oralExerciseContent(firstContent)
                        .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                        .status(UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED)
                        .amount(3L)
                        .balanceAfter(3L)
                        .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                        .coinId("essential_video_1")
                        .build()
        ));

        OralExerciseDto.ListResponse response = service.getContents(request);

        OralExerciseDto.ContentResponse content = response.getContents().get(0);
        assertThat(content.isRewardReceived()).isFalse();
        assertThat(content.getButtonChallenge().isRewardAvailable()).isTrue();
    }

    private User userCreatedDaysAgo(int daysAgo) {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("tester")
                .userPassword("password")
                .userName("테스터")
                .userPhoneNumber("01000000000")
                .findPwdQuestionId(1L)
                .findPwdAnswer("answer")
                .build();
        Date created = Date.from(LocalDate.now(ZoneId.systemDefault())
                .minusDays(daysAgo)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
        user.setCreated(created);
        return user;
    }

    private OralExerciseContent content(int sort) {
        return content(sort, null, null);
    }

    private OralExerciseContent content(int sort, String videoUrl, String thumbnailUrl) {
        OralExerciseContent content = OralExerciseContent.builder()
                .contentSort(sort)
                .title(sort + "주차")
                .description("description")
                .learningPoint("learning point")
                .thumbnailUrl(thumbnailUrl)
                .videoUrl(videoUrl)
                .durationSeconds(300)
                .level(sort >= 2 && sort <= 6 ? "실습" : "엑스트라")
                .active(true)
                .build();
        ReflectionTestUtils.setField(content, "oralExerciseContentId", (long) sort);
        return content;
    }
}
