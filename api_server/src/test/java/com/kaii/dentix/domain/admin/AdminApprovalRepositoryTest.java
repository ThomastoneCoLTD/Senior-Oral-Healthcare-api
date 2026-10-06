package com.kaii.dentix.domain.admin;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.AdminApprovalStatus;
import com.kaii.dentix.domain.admin.dto.AdminDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import java.sql.Timestamp;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties={
    "spring.datasource.url=jdbc:h2:mem:adminapproval;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import(AdminApprovalRepositoryTest.QueryConfig.class)
@Sql(statements={
    "CREATE TABLE IF NOT EXISTS admin (admin_id BIGINT PRIMARY KEY, created TIMESTAMP, modified TIMESTAMP, deleted TIMESTAMP, admin_name VARCHAR(45), admin_login_identifier VARCHAR(45), admin_phone_number VARCHAR(11), admin_is_super VARCHAR(1), admin_password VARCHAR(255), admin_last_login_date TIMESTAMP, admin_refresh_token VARCHAR(1000), find_pwd_question_id BIGINT, find_pwd_answer VARCHAR(200), organization_id BIGINT, approval_status VARCHAR(20), approved_at TIMESTAMP, approved_by_admin_id BIGINT)",
    "CREATE TABLE IF NOT EXISTS organization (organization_id BIGINT PRIMARY KEY, organization_name VARCHAR(100), deleted TIMESTAMP)",
    "CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR 'com.kaii.dentix.domain.admin.AdminApprovalRepositoryTest.dateFormat'",
    "INSERT INTO organization (organization_id,organization_name) VALUES (11,'테스트 기관')",
    "INSERT INTO admin (admin_id,created,admin_name,admin_login_identifier,admin_phone_number,admin_is_super,organization_id,approval_status) VALUES (1,'2026-10-06 00:00:00','기존 관리자','legacy','01000000001','N',11,NULL),(2,'2026-10-05 00:00:00','신청자','pending','01000000002','N',NULL,'PENDING'),(3,'2026-10-06 00:00:00','슈퍼','super','01000000003','Y',NULL,NULL)",
    "INSERT INTO admin (admin_id,created,deleted,admin_name,admin_is_super,approval_status) VALUES (4,'2026-10-06 00:00:00','2026-10-06 01:00:00','삭제됨','N','PENDING')"
})
public class AdminApprovalRepositoryTest {
    @Autowired AdminRepository repository;
    @Autowired JdbcTemplate jdbc;
    @TestConfiguration static class QueryConfig {
        @Bean JPAQueryFactory queryFactory(EntityManager entityManager) { return new JPAQueryFactory(entityManager); }
    }
    public static String dateFormat(Timestamp timestamp, String ignored) { return timestamp.toLocalDateTime().toLocalDate().toString(); }

    @Test void pendingInstitutionlessApplicantAppearsFirstAndDeletedAccountsAreExcluded() {
        var page=repository.findAllByNotSuper(AdminDto.SearchRequest.builder().page(1).size(20).build());
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(a -> a.getAdminId()).containsExactly(2L,1L);
        assertThat(page.getContent().get(0).getApprovalStatus()).isEqualTo(AdminApprovalStatus.PENDING);
        assertThat(page.getContent().get(0).getOrganizationName()).isNull();
        assertThat(page.getContent().get(1).getOrganizationName()).isEqualTo("테스트 기관");
        assertThat(page.getContent().get(1).getApprovalStatus()).isNull();
    }

    @Test void approvalPersistsAuditAndNeverMakesApplicantSuperAdmin() {
        var admin=repository.findByIdForApproval(2L).orElseThrow();
        admin.approve(3L);
        repository.saveAndFlush(admin);
        assertThat(jdbc.queryForObject("SELECT approval_status FROM admin WHERE admin_id=2",String.class)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT approved_by_admin_id FROM admin WHERE admin_id=2",Long.class)).isEqualTo(3L);
        assertThat(jdbc.queryForObject("SELECT admin_is_super FROM admin WHERE admin_id=2",String.class)).isEqualTo("N");
        assertThat(repository.findByIdForApproval(4L)).isEmpty();
    }
}
