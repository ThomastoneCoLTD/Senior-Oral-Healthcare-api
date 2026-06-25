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

    private final OralExerciseContentRepository oralExerciseContentRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOralExerciseContents() {
        for (OralExerciseContent defaultContent : defaultContents()) {
            oralExerciseContentRepository.findByContentSort(defaultContent.getContentSort())
                    .ifPresentOrElse(
                            content -> content.fillVideoUrlIfBlank(TEST_VIDEO_URL),
                            () -> oralExerciseContentRepository.save(defaultContent)
                    );
        }
    }

    private List<OralExerciseContent> defaultContents() {
        return List.of(
                content(
                        1,
                        "구강체조의 효능",
                        "구강노쇠(Oral Frailty)를 이해하고 구강체조가 필요한 이유를 학습합니다.",
                        "구강기능과 전신건강의 관계"
                ),
                content(
                        2,
                        "목 스트레칭",
                        "경부 근육을 이완하고 어깨 가동성 향상과 자세 교정 운동을 익힙니다.",
                        "경부 근육 이완, 어깨 가동성 향상, 자세 교정"
                ),
                content(
                        3,
                        "타액이 나오는 구강체조",
                        "침샘 마사지, 혀 운동, 입술 운동으로 타액분비를 촉진합니다.",
                        "침샘 마사지, 혀 운동, 입술 운동, 타액분비 촉진"
                ),
                content(
                        4,
                        "삼키는 힘 기르는 구강체조",
                        "연하근육과 목 근육을 강화하고 삼킴 훈련을 따라 합니다.",
                        "연하근육 강화, 목 근육 강화, 삼킴 훈련"
                ),
                content(
                        5,
                        "말하는 힘 기르는 구강체조",
                        "파·타·카·라 발성 훈련으로 혀 민첩성과 입술 근력을 높입니다.",
                        "파·타·카·라 발성 훈련, 혀 민첩성 향상, 입술 근력 강화"
                ),
                content(
                        6,
                        "씹는 힘 기르는 구강체조",
                        "저작근 강화, 턱관절 운동, 볼 근육 강화를 실습합니다.",
                        "저작근 강화, 턱관절 운동, 볼 근육 강화"
                ),
                content(
                        7,
                        "구강건조증 관리법",
                        "구강건조증 원인과 생활관리 방법, 수분 섭취 습관을 학습합니다.",
                        "구강건조증 원인, 생활관리 방법, 수분 섭취 및 타액분비 촉진"
                ),
                content(
                        8,
                        "의치 사용 및 관리법",
                        "의치 사용 방법과 세척·보관법, 사용 시 주의사항을 안내합니다.",
                        "의치 사용 방법, 의치 세척 및 보관법, 의치 사용 시 주의사항"
                ),
                content(
                        9,
                        "올바른 칫솔질",
                        "올바른 칫솔 선택과 칫솔질 방법, 치간관리 및 혀 관리를 익힙니다.",
                        "올바른 칫솔 선택, 올바른 칫솔질 방법, 치간관리 및 혀 관리"
                ),
                content(
                        10,
                        "구취 예방과 관리",
                        "구취 발생 원인을 이해하고 관리 방법과 생활습관 개선법을 학습합니다.",
                        "구취 발생 원인, 구취 관리 방법, 생활습관 개선"
                ),
                content(
                        11,
                        "테스트 동영상",
                        "구강체조 콘텐츠 영상 재생을 확인하기 위한 테스트 영상입니다.",
                        "영상 업로드 및 재생 테스트",
                        TEST_VIDEO_URL,
                        "테스트"
                ),
                content(
                        12,
                        "구강 건강 생활습관",
                        "일상에서 실천할 수 있는 구강 건강 관리 습관을 학습합니다.",
                        "구강 건강 생활습관, 정기 관리, 예방 중심 관리"
                )
        );
    }

    private OralExerciseContent content(
            int sort,
            String title,
            String description,
            String learningPoint
    ) {
        return content(sort, title, description, learningPoint, TEST_VIDEO_URL, sort == 1 ? "교육" : "실습");
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
                .durationSeconds(300)
                .level(level)
                .active(true)
                .build();
    }
}
