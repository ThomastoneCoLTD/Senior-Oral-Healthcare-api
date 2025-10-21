package com.kaii.dentix.domain.oralCheck.dao;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface OralCheckRepository extends JpaRepository<OralCheck, Long> {

    List<OralCheck> findAllByUserIdOrderByCreatedDesc(Long userId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO oralCheck (userId, oralCheckPicturePath, oralCheckAnalysisState, oralCheckResultTotalType, oralCheckResultJsonData, " +
        "oralCheckTotalRange, oralCheckUpRightRange, oralCheckUpLeftRange, oralCheckDownRightRange, oralCheckDownLeftRange, " +
        "oralCheckUpRightScoreType, oralCheckUpLeftScoreType, oralCheckDownRightScoreType, oralCheckDownLeftScoreType, created) " +
        "VALUES (:#{#oralCheck.userId}, :#{#oralCheck.oralCheckPicturePath}, :#{#oralCheck.oralCheckAnalysisState.toString()}, :#{#oralCheck.oralCheckResultTotalType.toString()}, :#{#oralCheck.oralCheckResultJsonData}, " +
        ":#{#oralCheck.oralCheckTotalRange}, :#{#oralCheck.oralCheckUpRightRange}, :#{#oralCheck.oralCheckUpLeftRange}, :#{#oralCheck.oralCheckDownRightRange}, :#{#oralCheck.oralCheckDownLeftRange}, " +
        ":#{#oralCheck.oralCheckUpRightScoreType.toString()}, :#{#oralCheck.oralCheckUpLeftScoreType.toString()}, :#{#oralCheck.oralCheckDownRightScoreType.toString()}, :#{#oralCheck.oralCheckDownLeftScoreType.toString()}, :created)", nativeQuery = true)
    int nativeInsert(OralCheck oralCheck, Date created);
//    List<Admin> findByOrganization_OrganizationId(Long organizationId);
    long countByUserId(Long userId);
    long countByUserIdAndOralCheckAnalysisState(Long userId, OralCheckAnalysisState state);

    @Query("SELECT COUNT(oc) FROM OralCheck oc " +
            "JOIN oc.user u " +
            "JOIN u.organization o " +
            "WHERE o.organizationId = :organizationId " +
            "AND oc.oralCheckAnalysisState = 'SUCCESS'")


    long countSuccessByOrganization(@Param("organizationId") Long organizationId);

    @Query("SELECT new com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount(o.oralCheckResultTotalType, COUNT(o)) " +
            "FROM OralCheck o WHERE o.user.organization.organizationId = :organizationId GROUP BY o.oralCheckResultTotalType")
    List<OralCheckResultTypeCount> countByOrganization(@Param("organizationId") Long organizationId);

}