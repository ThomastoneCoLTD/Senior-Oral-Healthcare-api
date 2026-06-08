package com.kaii.dentix.domain.oralExercise.dto;

import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionEventType;
import com.kaii.dentix.domain.oralExercise.domain.UserOralExerciseProgress;
import lombok.*;

import java.util.List;

public class OralExerciseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentResponse {
        private Long id;
        private int sort;
        private String title;
        private String description;
        private String learningPoint;
        private String thumbnailUrl;
        private String videoUrl;
        private int durationSeconds;
        private String duration;
        private String level;
        private int week;
        private boolean coreContent;
        private boolean available;
        private boolean currentWeekContent;
        private boolean rewardReceived;
        private ButtonChallengeResponse buttonChallenge;
        private ProgressResponse progress;

        public static ContentResponse from(
                OralExerciseContent content,
                UserOralExerciseProgress progress,
                int currentWeek,
                boolean rewardReceived
        ) {
            boolean available = currentWeek <= 0 || content.getContentSort() <= currentWeek;
            return ContentResponse.builder()
                    .id(content.getOralExerciseContentId())
                    .sort(content.getContentSort())
                    .title(content.getTitle())
                    .description(content.getDescription())
                    .learningPoint(content.getLearningPoint())
                    .thumbnailUrl(content.getThumbnailUrl())
                    .videoUrl(content.getVideoUrl())
                    .durationSeconds(content.getDurationSeconds())
                    .duration(formatDuration(content.getDurationSeconds()))
                    .level(content.getLevel())
                    .week(content.getContentSort())
                    .coreContent(content.getContentSort() <= 5)
                    .available(available)
                    .currentWeekContent(currentWeek == content.getContentSort())
                    .rewardReceived(rewardReceived)
                    .buttonChallenge(ButtonChallengeResponse.forContent(rewardReceived))
                    .progress(ProgressResponse.from(progress))
                    .build();
        }

        public static ContentResponse from(OralExerciseContent content, UserOralExerciseProgress progress) {
            return from(content, progress, 0, false);
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressResponse {
        private int totalWatchedSeconds;
        private int maxWatchedSeconds;
        private int lastPositionSeconds;
        private int completionRate;
        private boolean completed;
        private int viewCount;

        public static ProgressResponse from(UserOralExerciseProgress progress) {
            if (progress == null) {
                return ProgressResponse.builder()
                        .totalWatchedSeconds(0)
                        .maxWatchedSeconds(0)
                        .lastPositionSeconds(0)
                        .completionRate(0)
                        .completed(false)
                        .viewCount(0)
                        .build();
            }

            return ProgressResponse.builder()
                    .totalWatchedSeconds(progress.getTotalWatchedSeconds())
                    .maxWatchedSeconds(progress.getMaxWatchedSeconds())
                    .lastPositionSeconds(progress.getLastPositionSeconds())
                    .completionRate(progress.getCompletionRate())
                    .completed(progress.isCompleted())
                    .viewCount(progress.getViewCount())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private int currentWeek;
        private ContentResponse currentContent;
        private List<ContentResponse> previousContents;
        private List<ContentResponse> extraContents;
        private List<ContentResponse> contents;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ButtonChallengeResponse {
        private List<Integer> buttons;
        private int timeoutSeconds;
        private boolean rewardAvailable;
        private String promptMessage;

        public static ButtonChallengeResponse forContent(boolean rewardReceived) {
            return ButtonChallengeResponse.builder()
                    .buttons(List.of(1, 2, 3, 4, 5))
                    .timeoutSeconds(30)
                    .rewardAvailable(!rewardReceived)
                    .promptMessage("1번부터 5번 중 안내된 번호를 클릭하세요.")
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteractionRequest {
        private Long contentId;
        private OralExerciseInteractionEventType eventType;
        private Integer watchedSeconds;
        private Integer currentPositionSeconds;
        private Integer durationSeconds;
        private Integer completionRate;
        private Boolean completed;
        private String sessionId;
    }

    private static String formatDuration(int durationSeconds) {
        int safeSeconds = Math.max(durationSeconds, 0);
        int minutes = safeSeconds / 60;
        int seconds = safeSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
