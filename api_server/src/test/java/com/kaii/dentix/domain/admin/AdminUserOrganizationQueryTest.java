package com.kaii.dentix.domain.admin;

import com.kaii.dentix.domain.admin.dao.user.AdminUserRepositoryImpl;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.bind.WebDataBinder;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:adminorg;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(statements = {
    "CREATE TABLE IF NOT EXISTS user (user_id BIGINT PRIMARY KEY, created TIMESTAMP, modified TIMESTAMP, deleted TIMESTAMP, user_login_identifier VARCHAR(45), user_password VARCHAR(255), user_name VARCHAR(100), user_phone_number VARCHAR(45), user_birth_date VARCHAR(10), real_organization VARCHAR(20), oral_analysis_service_enabled BOOLEAN, onboarding_survey_required BOOLEAN, user_gender VARCHAR(20), find_pwd_question_id BIGINT, find_pwd_answer VARCHAR(255), user_refresh_token VARCHAR(255), success_count INTEGER, user_last_login_date TIMESTAMP, is_verify VARCHAR(10), daegu_did VARCHAR(255), daegu_did_key VARCHAR(255), daegu_did_status VARCHAR(20), organization_id BIGINT)",
    "CREATE TABLE IF NOT EXISTS organization (organization_id BIGINT PRIMARY KEY, created TIMESTAMP, modified TIMESTAMP, deleted TIMESTAMP, organization_name VARCHAR(100), organization_email VARCHAR(100), organization_phone_number VARCHAR(20), organization_address VARCHAR(200), description VARCHAR(500), active BOOLEAN, subscription_start_date TIMESTAMP, subscription_end_date TIMESTAMP, subscription_plan_id BIGINT)",
    "CREATE TABLE IF NOT EXISTS oral_check (oral_check_id BIGINT PRIMARY KEY, created TIMESTAMP, modified TIMESTAMP, subscription_history_id BIGINT, user_id BIGINT, oral_check_picture_path VARCHAR(200), oral_check_analysis_state VARCHAR(20), oral_check_total_range REAL, oral_check_up_right_range REAL, oral_check_up_left_range REAL, oral_check_down_right_range REAL, oral_check_down_left_range REAL, oral_check_result_json_data JSON, oral_check_result_total_type VARCHAR(20), oral_check_up_right_score_type VARCHAR(20), oral_check_up_left_score_type VARCHAR(20), oral_check_down_right_score_type VARCHAR(20), oral_check_down_left_score_type VARCHAR(20))",
    "DELETE FROM user",
    "INSERT INTO user(user_id,user_name,user_login_identifier,real_organization,deleted) VALUES (1,'Alpha','alpha','대구1',NULL),(2,'Beta','beta',' 대구1 ',NULL),(3,'Prefix','prefix','대구10',NULL),(4,'Null','null',NULL,NULL),(5,'Blank','blank','   ',NULL),(6,'Deleted','deleted','삭제기관',CURRENT_TIMESTAMP)"
})
class AdminUserOrganizationQueryTest {
    @org.springframework.boot.test.mock.mockito.MockBean JPAQueryFactory unusedFactory;
    @Autowired EntityManager em;
    @Autowired com.kaii.dentix.domain.user.dao.UserRepository users;

    private AdminUserDto.SearchRequest request(String organization, int page) {
        var request = AdminUserDto.SearchRequest.builder().page(page).size(1).build();
        new WebDataBinder(request).bind(new MutablePropertyValues(java.util.Map.of("realOrganization", organization)));
        return request;
    }

    @Test void exactInstitutionFiltersBeforePaginationAndKeepsTotalsOnAnEmptyPage() {
        var repository = new AdminUserRepositoryImpl(new JPAQueryFactory(em));
        var first = repository.findAll(request("대구1", 1));
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).extracting(AdminUserDto.Info::getUserId).containsExactly(2L);
        assertThat(repository.findAll(request("대구1", 2)).getContent())
            .extracting(AdminUserDto.Info::getUserId).containsExactly(1L);
        assertThat(repository.findAll(request("대구1", 3)).getTotalElements()).isEqualTo(2);
    }

    @Test void unassignedInstitutionCombinesNullAndBlankAndKeepsKeywordSearch() {
        var repository = new AdminUserRepositoryImpl(new JPAQueryFactory(em));
        var request = request("", 1);
        assertThat(repository.findAll(request).getTotalElements()).isEqualTo(2);
        request.setKeyword("blank");
        assertThat(repository.findAll(request).getContent()).extracting(AdminUserDto.Info::getUserId).containsExactly(5L);
    }

    @Test void institutionOptionsIncludeUsersOutsideCurrentPageAndExcludeDeletedUsers() {
        assertThat(users.findRealOrganizations()).containsExactly("", "대구1", "대구10");
    }
}
