package com.kaii.dentix.domain.oralExercise.application;

import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OralExerciseContentInitializer {

    private static final String TEST_VIDEO_URL = "/videos/oral-exercise/test.mp4";
    private static final String S3_VIDEO_BASE_URL =
            "https://denti-backends.s3.ap-northeast-2.amazonaws.com/soh/video/";
    private static final String VIDEO_1_URL =
            S3_VIDEO_BASE_URL + "1%ED%99%94%20%EC%9E%85%EC%B2%B4%EC%A1%B0%EC%9D%98%20%ED%9A%A8%EB%8A%A5_fin.mp4";
    private static final String VIDEO_2_URL =
            S3_VIDEO_BASE_URL + "2%ED%99%94%20%EB%AA%A9%EC%8A%A4%ED%8A%B8%EB%A0%88%EC%B9%AD_fin.mp4";
    private static final String VIDEO_3_URL =
            S3_VIDEO_BASE_URL + "3%ED%99%94%20%ED%83%80%EC%95%A1%20%EB%82%98%EC%98%A4%EB%8A%94%20%EC%9E%85%EC%B2%B4%EC%A1%B0_fin.mp4";
    private static final String VIDEO_4_URL =
            S3_VIDEO_BASE_URL + "4%ED%99%94%20%EC%82%BC%ED%82%A4%EB%8A%94%20%ED%9E%98%20%EA%B8%B0%EB%A5%B4%EB%8A%94%20%EC%9E%85%EC%B2%B4%EC%A1%B0_fin.mp4";
    private static final String VIDEO_5_URL =
            S3_VIDEO_BASE_URL + "5%ED%99%94%20%EB%A7%90%ED%95%98%EB%8A%94%20%ED%9E%98%20%EA%B8%B0%EB%A5%B4%EB%8A%94%20%EC%9E%85%EC%B2%B4%EC%A1%B0_fin.mp4";
    private static final String VIDEO_6_URL =
            S3_VIDEO_BASE_URL + "6%ED%99%94%20%EC%94%B9%EB%8A%94%20%ED%9E%98%20%EA%B8%B0%EB%A5%B4%EB%8A%94%20%EC%9E%85%EC%B2%B4%EC%A1%B0_fin.mp4";

    private final OralExerciseContentRepository oralExerciseContentRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOralExerciseContents() {
        for (OralExerciseContent defaultContent : defaultContents()) {
            oralExerciseContentRepository.findByContentSort(defaultContent.getContentSort())
                    .ifPresentOrElse(
                            content -> content.applyDefaultContent(defaultContent, TEST_VIDEO_URL),
                            () -> oralExerciseContentRepository.save(defaultContent)
                    );
        }
    }

    private List<OralExerciseContent> defaultContents() {
        return List.of(
                content(
                        1,
                        "Chapter 1. 입체조의 효능",
                        "구강노쇠(Oral Frailty)를 이해하고 입체조가 필요한 이유를 학습합니다.",
                        "구강노쇠(Oral Frailty) 이해"
                ),
                content(
                        2,
                        "Chapter 2. 목 스트레칭",
                        "경부 근육 이완과 자세 교정 운동을 학습합니다.",
                        "경부 근육 이완과 자세 교정"
                ),
                content(
                        3,
                        "Chapter 3. 타액이 나오는 입체조",
                        "침샘 마사지와 혀 운동을 통해 타액분비 촉진 방법을 학습합니다.",
                        "침샘 마사지와 혀 운동"
                ),
                content(
                        4,
                        "Chapter 4. 삼키는 힘 기르는 입체조",
                        "연하근육 강화 훈련을 학습합니다.",
                        "연하근육 강화 훈련"
                ),
                content(
                        5,
                        "Chapter 5. 말하는 힘 기르는 입체조",
                        "파·타·카·라 발성 훈련을 학습합니다.",
                        "파·타·카·라 발성 훈련"
                ),
                content(
                        6,
                        "Chapter 6. 씹는 힘 기르는 입체조",
                        "저작근 강화와 턱관절 운동을 학습합니다.",
                        "저작근 강화와 턱관절 운동"
                ),
                content(
                        7,
                        "Chapter 7. 구강건조증 관리법",
                        "구강건조증의 원인과 타액분비 촉진 방법을 학습합니다.",
                        "원인과 타액분비 촉진"
                ),
                content(
                        8,
                        "Chapter 8. 의치 사용 및 관리법",
                        "의치의 올바른 착용과 세척·보관 방법을 안내합니다.",
                        "올바른 착용과 세척·보관"
                ),
                content(
                        9,
                        "Chapter 9. 올바른 칫솔질",
                        "칫솔 선택부터 치간관리까지 올바른 칫솔질 방법을 익힙니다.",
                        "칫솔 선택부터 치간관리까지"
                ),
                content(
                        10,
                        "Chapter 10. 구취 예방과 관리",
                        "구취 원인을 파악하고 생활습관 개선법을 학습합니다.",
                        "원인 파악과 생활습관 개선"
                ),
                content(
                        11,
                        "Chapter 11. 삼킴 건강과 식사의 관계",
                        "연하장애 자가점검 방법을 학습합니다.",
                        "연하장애 자가점검"
                ),
                content(
                        12,
                        "Chapter 12. 구강건강, 스스로 지키는 습관",
                        "정기검진과 지속 실천 방법을 학습합니다.",
                        "정기검진과 지속 실천"
                )
        );
    }

    private OralExerciseContent content(
            int sort,
            String title,
            String description,
            String learningPoint
    ) {
        return content(sort, title, description, learningPoint, videoUrlForSort(sort), levelForSort(sort));
    }

    private OralExerciseContent content(
            int sort,
            String title,
            String description,
            String learningPoint,
            String videoUrl,
            String level
    ) {
        return OralExerciseContent.builder()
                .contentSort(sort)
                .title(title)
                .description(description)
                .learningPoint(learningPoint)
                .thumbnailUrl(null)
                .videoUrl(videoUrl)
                .durationSeconds(durationSecondsForSort(sort))
                .level(level)
                .active(true)
                .build();
    }

    private String videoUrlForSort(int sort) {
        if (sort >= 7) {
            return VIDEO_1_URL;
        }
        return switch (sort) {
            case 1 -> VIDEO_1_URL;
            case 2 -> VIDEO_2_URL;
            case 3 -> VIDEO_3_URL;
            case 4 -> VIDEO_4_URL;
            case 5 -> VIDEO_5_URL;
            case 6 -> VIDEO_6_URL;
            default -> null;
        };
    }

    private int durationSecondsForSort(int sort) {
        if (sort == 1 || sort >= 7) {
            return 114;
        }
        return switch (sort) {
            case 2 -> 212;
            case 3 -> 176;
            case 4 -> 172;
            case 5 -> 428;
            case 6 -> 232;
            default -> 300;
        };
    }

    private String levelForSort(int sort) {
        if (sort == 1) {
            return "INTRO";
        }
        return sort <= 6 ? "필수" : "선택";
    }
}
