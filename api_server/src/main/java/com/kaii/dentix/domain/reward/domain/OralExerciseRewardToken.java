package com.kaii.dentix.domain.reward.domain;

public final class OralExerciseRewardToken {

    private static final int ESSENTIAL_VIDEO_COUNT = 5;
    private static final int OPTIONAL_VIDEO_COUNT = 7;

    private OralExerciseRewardToken() {
    }

    public static String tokenNameForContentSort(int contentSort) {
        if (contentSort >= 2 && contentSort <= ESSENTIAL_VIDEO_COUNT + 1) {
            return "essential_video_" + (contentSort - 1);
        }
        int optionalIndex = contentSort == 1 ? 1 : contentSort - ESSENTIAL_VIDEO_COUNT;
        if (optionalIndex >= 1 && optionalIndex <= OPTIONAL_VIDEO_COUNT) {
            return "optional_video_" + optionalIndex;
        }
        return null;
    }
}
