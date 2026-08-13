package com.kaii.dentix.domain.findPwdQuestion.application;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class FindIdQuestionInitializer implements ApplicationRunner {

    private static final String UPSERT_SQL = """
            INSERT INTO find_pwd_question (
                find_pwd_question_id,
                find_pwd_question_sort,
                find_pwd_question_title,
                created,
                modified
            ) VALUES (?, ?, ?, NOW(6), NOW(6))
            ON DUPLICATE KEY UPDATE
                find_pwd_question_sort = VALUES(find_pwd_question_sort),
                find_pwd_question_title = VALUES(find_pwd_question_title)
            """;

    private static final List<String> QUESTION_TITLES = List.of(
            "내가 가장 좋아하는 색은?",
            "내가 졸업한 초등학교의 이름은?",
            "내가 가장 소중하게 생각하는 것은?",
            "나의 좌우명은?",
            "아무도 모르는 나만의 비밀은?",
            "나랑 가장 친한 친구의 이름은?",
            "나의 첫 해외여행지는?",
            "내가 가장 좋아하는 음식은?",
            "내가 가장 존경하는 인물은?"
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (int index = 0; index < QUESTION_TITLES.size(); index++) {
            long idAndSort = index + 1L;
            jdbcTemplate.update(UPSERT_SQL, idAndSort, idAndSort, QUESTION_TITLES.get(index));
        }
    }
}
