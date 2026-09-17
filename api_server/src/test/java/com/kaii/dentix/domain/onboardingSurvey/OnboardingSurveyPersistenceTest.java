package com.kaii.dentix.domain.onboardingSurvey;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OnboardingSurveyPersistenceTest {
    @Test void schemaPersistsJsonAndTimestampAndEnforcesOneRowPerMember() {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:onboarding;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .build();
        try (var factory = new MetadataSources(registry).addAnnotatedClass(OnboardingSurvey.class)
                .buildMetadata().buildSessionFactory()) {
            try (var session = factory.openSession()) {
                var tx = session.beginTransaction();
                session.persist(new OnboardingSurvey(123L, "v1", "{\"eat10_1\":[\"0\"]}", "{\"eat10\":0}"));
                tx.commit();
            }
            try (var session = factory.openSession()) {
                var saved = session.find(OnboardingSurvey.class, 123L);
                assertThat(saved.getAnswers()).contains("eat10_1");
                assertThat(saved.getScores()).contains("eat10");
                assertThat(saved.getSubmittedAt()).isNotNull();
                assertThat(session.find(OnboardingSurvey.class, 456L)).isNull();
                var tx = session.beginTransaction();
                session.clear();
                session.persist(new OnboardingSurvey(123L, "v2", "{}", "{}"));
                assertThatThrownBy(session::flush).isInstanceOf(RuntimeException.class);
                tx.rollback();
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
}
