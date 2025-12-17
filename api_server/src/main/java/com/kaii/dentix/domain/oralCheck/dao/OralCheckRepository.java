package com.kaii.dentix.domain.oralCheck.dao;

import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto;
import com.kaii.dentix.domain.organization.dao.OrganizationUsageResponse;
import com.kaii.dentix.domain.organization.dto.RecentUsage;
import com.kaii.dentix.domain.organization.dto.TopUserUsage;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OralCheckRepository extends JpaRepository<OralCheck, Long> {

    List<OralCheck> findAllByUser_UserIdOrderByCreatedDesc(Long userId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO oralCheck (
          userId, oralCheckPicturePath, oralCheckAnalysisState, oralCheckResultTotalType, oralCheckResultJsonData,
          oralCheckTotalRange, oralCheckUpRightRange, oralCheckUpLeftRange, oralCheckDownRightRange, oralCheckDownLeftRange,
          oralCheckUpRightScoreType, oralCheckUpLeftScoreType, oralCheckDownRightScoreType, oralCheckDownLeftScoreType, created
        )
        VALUES (
          :#{#oralCheck.user.id}, :#{#oralCheck.oralCheckPicturePath}, :#{#oralCheck.oralCheckAnalysisState.toString()},
          :#{#oralCheck.oralCheckResultTotalType.toString()}, :#{#oralCheck.oralCheckResultJsonData},
          :#{#oralCheck.oralCheckTotalRange}, :#{#oralCheck.oralCheckUpRightRange}, :#{#oralCheck.oralCheckUpLeftRange},
          :#{#oralCheck.oralCheckDownRightRange}, :#{#oralCheck.oralCheckDownLeftRange},
          :#{#oralCheck.oralCheckUpRightScoreType.toString()}, :#{#oralCheck.oralCheckUpLeftScoreType.toString()},
          :#{#oralCheck.oralCheckDownRightScoreType.toString()}, :#{#oralCheck.oralCheckDownLeftScoreType.toString()}, :created
        )
        """, nativeQuery = true)
    int nativeInsert(OralCheck oralCheck, Date created);

    long countByUser_UserId(Long userId);

    // ✅ 수정된 부분
    long countByUser_UserIdAndOralCheckAnalysisState(Long userId, OralCheckAnalysisState state);

    @Query("""
        SELECT COUNT(oc)
        FROM OralCheck oc
        JOIN oc.user u
        JOIN u.organization o
        WHERE o.organizationId = :organizationId
          AND oc.oralCheckAnalysisState = 'SUCCESS'
    """)
    long countSuccessByOrganization(@Param("organizationId") Long organizationId);

    @Query("""
        SELECT new com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount(
            o.oralCheckResultTotalType, COUNT(o)
        )
        FROM OralCheck o
        WHERE o.user.organization.organizationId = :organizationId
        GROUP BY o.oralCheckResultTotalType
    """)
    List<OralCheckResultTypeCount> countByOrganization(@Param("organizationId") Long organizationId);

    @Query("""
        SELECT COUNT(oc)
        FROM OralCheck oc
        WHERE oc.user.organization.organizationId = :organizationId
          AND oc.oralCheckAnalysisState = :state
    """)
    long countByOrganizationIdAndOralCheckAnalysisState(
            @Param("organizationId") Long organizationId,
            @Param("state") OralCheckAnalysisState state
    );

    @Query("""
        SELECT COUNT(o)
        FROM OralCheck o
        WHERE o.user.organization.organizationId = :orgId
          AND o.oralCheckAnalysisState = 'SUCCESS'
          AND (:fromDate IS NULL OR o.created >= :fromDate)
    """)
    long countSuccessSince(@Param("orgId") Long organizationId,
                           @Param("fromDate") LocalDateTime fromDate);

    @Query("""
SELECT new com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto(
    oc.user.userId,
    oc.user.userName,
    COUNT(oc)
)
FROM OralCheck oc
WHERE oc.user.organization.organizationId = :orgId
GROUP BY oc.user.userId, oc.user.userName
ORDER BY COUNT(oc) DESC
""")
    List<OralCheckUsageDto> findUserUsageByOrganization(@Param("orgId") Long orgId);

    @Query("""
    SELECT s FROM SubscriptionHistory s
    WHERE s.organization.organizationId = :orgId
    ORDER BY s.startDate DESC
""")
    Optional<SubscriptionHistory> findLatestActiveSubscription(Long orgId);

    @Query("""
    SELECT COUNT(o)
    FROM OralCheck o
    WHERE o.user.organization.organizationId = :orgId
      AND DATE(o.created) = CURRENT_DATE
""")
    long countTodayUsage(Long orgId);


    /** 이번 주 사용량 */
    @Query("""
        SELECT COUNT(o)
        FROM OralCheck o
        WHERE o.user.organization.organizationId= :orgId
          AND YEARWEEK(o.created, 1) = YEARWEEK(CURRENT_DATE, 1)
    """)
    long countThisWeekUsage(@Param("orgId") Long orgId);


    /** 이번 달 사용량 */
    @Query("""
        SELECT COUNT(o)
        FROM OralCheck o
        WHERE o.user.organization.organizationId = :orgId
          AND YEAR(o.created) = YEAR(CURRENT_DATE)
          AND MONTH(o.created) = MONTH(CURRENT_DATE)
    """)
    long countThisMonthUsage(@Param("orgId") Long orgId);


    @Query("""
    SELECT new com.kaii.dentix.domain.organization.dto.TopUserUsage(
        u.userId,
        u.userName,
        u.userLoginIdentifier,
        COUNT(o)
    )
    FROM OralCheck o
    JOIN o.user u
    WHERE u.organization.organizationId = :orgId
    GROUP BY u.userId, u.userName, u.userLoginIdentifier
    ORDER BY COUNT(o) DESC
""")
    List<TopUserUsage> findTopUsers(Long orgId);


    @Query("""
    SELECT new com.kaii.dentix.domain.organization.dto.RecentUsage(
        o.oralCheckId,
        u.userName,
        u.userLoginIdentifier,
        o.oralCheckResultTotalType,
        o.created
    )
    FROM OralCheck o
    JOIN o.user u
    WHERE u.organization.organizationId = :orgId
    ORDER BY o.created DESC
""")
    List<RecentUsage> findRecentUsages(Long orgId);

    @Query("""
    SELECT COUNT(oc)
    FROM OralCheck oc
    WHERE oc.user.organization.organizationId = :orgId
      AND oc.created >= :start
      AND oc.created < :end
""")
    Long countSubscriptionPeriodUsage(
            Long orgId,
            Date start,
            Date end
    );

    @Query("""
    SELECT new com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto(
        u.userId,
        u.userName,
        COUNT(oc)
    )
    FROM OralCheck oc
    JOIN oc.user u
    JOIN u.organization o
    WHERE o.organizationId = :orgId
      AND oc.created >= :start
      AND oc.created < :end
    GROUP BY u.userId, u.userName
""")
    List<OralCheckUsageDto> findUserUsageByOrganizationAndPeriod(
            @Param("orgId") Long orgId,
            @Param("start") Date start,
            @Param("end") Date end
    );
}
