package com.kaii.dentix.domain.intakeSurvey;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IntakeSurveyPersistenceTest {
    @Test void draftAndCompletionPersistAcrossDatabaseSessionsWithRevision() {
        try (var factory = new Configuration().addAnnotatedClass(UserIntakeSurvey.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:intake-persistence;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory()) {
            try (var session = factory.openSession()) {
                var transaction = session.beginTransaction();
                var survey = new UserIntakeSurvey(42L);
                survey.save("2026-09-17-v1", "{\"eat10_1\":0}", "{}", 2, false);
                session.persist(survey);
                transaction.commit();
                assertThat(survey.getRevision()).isZero();
            }
            try (var session = factory.openSession()) {
                var transaction = session.beginTransaction();
                var survey = session.find(UserIntakeSurvey.class, 42L);
                assertThat(survey.getAnswersJson()).contains("eat10_1");
                assertThat(survey.getCurrentTab()).isEqualTo(2);
                assertThat(survey.getCompletedAt()).isNull();
                survey.save("2026-09-17-v1", "{\"eat10_1\":0}", "{\"1\":0}", 7, true);
                transaction.commit();
            }
            try (var session = factory.openSession()) {
                var survey = session.find(UserIntakeSurvey.class, 42L);
                assertThat(survey.getRevision()).isEqualTo(1L);
                assertThat(survey.getCompletedAt()).isNotNull();
                assertThat(survey.getScoresJson()).isEqualTo("{\"1\":0}");
                assertThat(session.find(UserIntakeSurvey.class, 99L)).isNull();
            }
        }
    }
}
