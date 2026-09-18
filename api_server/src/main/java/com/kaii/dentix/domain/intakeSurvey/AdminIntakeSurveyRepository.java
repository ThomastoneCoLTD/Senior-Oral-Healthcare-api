package com.kaii.dentix.domain.intakeSurvey;

import com.kaii.dentix.domain.user.domain.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.List;

public interface AdminIntakeSurveyRepository extends JpaRepository<User, Long> {
    interface Summary {
        Long getUserId();
        String getUserName();
        String getUserLoginIdentifier();
        String getRealOrganization();
        Instant getCompletedAt();
        Instant getUpdatedAt();
        Long getRevision();
    }
    String FILTER = """
        from User u left join UserIntakeSurvey s on s.userId = u.userId
        where (:organization is null or coalesce(trim(u.realOrganization), '') = :organization)
        and (:keyword = '' or lower(u.userName) like lower(concat('%', :keyword, '%'))
             or lower(u.userLoginIdentifier) like lower(concat('%', :keyword, '%')))
        and (:status = 'ALL' or (:status = 'NOT_STARTED' and s.userId is null)
             or (:status = 'DRAFT' and s.userId is not null and s.completedAt is null)
             or (:status = 'COMPLETED' and s.completedAt is not null))
        """;
    @Query(value = """
        select u.userId as userId, u.userName as userName,
        u.userLoginIdentifier as userLoginIdentifier, u.realOrganization as realOrganization,
        s.completedAt as completedAt, s.updatedAt as updatedAt, s.revision as revision
        """ + FILTER + " order by coalesce(trim(u.realOrganization), ''), u.userName, u.userId",
        countQuery = "select count(u.userId) " + FILTER)
    Page<Summary> search(@Param("organization") String organization, @Param("keyword") String keyword,
                         @Param("status") String status, Pageable pageable);

    @Query("select distinct coalesce(trim(u.realOrganization), '') from User u order by coalesce(trim(u.realOrganization), '')")
    List<String> organizations();
}
