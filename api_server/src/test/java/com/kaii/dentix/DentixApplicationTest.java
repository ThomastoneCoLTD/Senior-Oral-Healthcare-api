package com.kaii.dentix;

import com.kaii.dentix.domain.agreement.application.ServiceAgreementInitializer;
import com.kaii.dentix.domain.oralCheck.application.OralCheckSchemaUpdater;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseContentInitializer;
import com.kaii.dentix.domain.organization.application.DaeguDefaultOrganizationInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dentix;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database=h2",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class DentixApplicationTest {

    @MockBean
    private ServiceAgreementInitializer serviceAgreementInitializer;

    @MockBean
    private OralExerciseContentInitializer oralExerciseContentInitializer;

    @MockBean
    private OralCheckSchemaUpdater oralCheckSchemaUpdater;

    @MockBean
    private DaeguDefaultOrganizationInitializer daeguDefaultOrganizationInitializer;

    @Test
    void contextLoads() {
    }

}
