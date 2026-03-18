package com.kaii.dentix.domain.oralCheck.dao;

import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminDto;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OralCheckRepository extends JpaRepository<OralCheck, Long> {

    List<OralCheck> findAllByUser_UserIdOrderByCreatedDesc(Long userId);

    Optional<OralCheck> findTopByUser_UserIdOrderByCreatedDesc(Long userId);

    // =================================================================
    // 1. 네이티브 쿼리 (Native Query)
    // =================================================================
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


    // =================================================================
    // 2. 단순 카운트 쿼리 (Dashboard Stats)
    // =================================================================

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
        SELECT COUNT(o)
        FROM OralCheck o
        WHERE o.user.organization.organizationId = :orgId
          AND DATE(o.created) = CURRENT_DATE
    """)
    long countTodayUsage(@Param("orgId") Long orgId);

    @Query("""
        SELECT COUNT(o)
        FROM OralCheck o
        WHERE o.user.organization.organizationId = :orgId
          AND YEARWEEK(o.created, 1) = YEARWEEK(CURRENT_DATE, 1)
    """)
    long countThisWeekUsage(@Param("orgId") Long orgId);

    @Query("""
        SELECT COUNT(o)
        FROM OralCheck o
        WHERE o.user.organization.organizationId = :orgId
          AND YEAR(o.created) = YEAR(CURRENT_DATE)
          AND MONTH(o.created) = MONTH(CURRENT_DATE)
    """)
    long countThisMonthUsage(@Param("orgId") Long orgId);

    @Query("""
        SELECT COUNT(oc)
        FROM OralCheck oc
        WHERE oc.user.organization.organizationId = :orgId
          AND oc.oralCheckAnalysisState = com.kaii.dentix.domain.type.oral.OralCheckAnalysisState.SUCCESS
          AND oc.created >= :start
          AND oc.created < :end
    """)
    Long countSubscriptionPeriodUsage(
            @Param("orgId") Long orgId,
            @Param("start") Date start,
            @Param("end") Date end
    );


    // 1. TopUser -> OrganizationDto
    @Query("""
        SELECT new com.kaii.dentix.domain.organization.dto.OrganizationDto$TopUser(
            u.userId, u.userName, u.userLoginIdentifier, COUNT(o)
        )
        FROM OralCheck o JOIN o.user u WHERE u.organization.organizationId = :orgId
        GROUP BY u.userId, u.userName, u.userLoginIdentifier
        ORDER BY COUNT(o) DESC
    """)
    List<OrganizationDto.TopUser> findTopUsers(@Param("orgId") Long orgId, Pageable pageable);

    default List<OrganizationDto.TopUser> findTopUsers(Long orgId) {
        return findTopUsers(orgId, Pageable.ofSize(5));
    }

    @Query("""
        SELECT new com.kaii.dentix.domain.organization.dto.OrganizationDto$RecentUsage(
            o.oralCheckId, u.userName, u.userLoginIdentifier, o.oralCheckResultTotalType, o.created
        )
        FROM OralCheck o JOIN o.user u WHERE u.organization.organizationId = :orgId
        ORDER BY o.created DESC
    """)
    List<OrganizationDto.RecentUsage> findRecentUsages(@Param("orgId") Long orgId, Pageable pageable);

    default List<OrganizationDto.RecentUsage> findRecentUsages(Long orgId) {
        return findRecentUsages(orgId, Pageable.ofSize(5));
    }

    // 3. UserUsage -> OralCheckDto (기존 유지)
    @Query("""
        SELECT new com.kaii.dentix.domain.oralCheck.dto.OralCheckDto$Usage(
            u.userId, u.userName, COUNT(oc)
        )
        FROM OralCheck oc JOIN oc.user u JOIN u.organization o
        WHERE o.organizationId = :orgId
          AND oc.oralCheckAnalysisState = com.kaii.dentix.domain.type.oral.OralCheckAnalysisState.SUCCESS
          AND oc.created >= :start
          AND oc.created < :end
        GROUP BY u.userId, u.userName
        ORDER BY COUNT(oc) DESC
    """)
    List<OralCheckDto.Usage> findUserUsageByOrganizationAndPeriod(
            @Param("orgId") Long orgId,
            @Param("start") Date start,
            @Param("end") Date end
    );

    long countByUser_UserIdAndOralCheckAnalysisState(Long userId, OralCheckAnalysisState state);


}
