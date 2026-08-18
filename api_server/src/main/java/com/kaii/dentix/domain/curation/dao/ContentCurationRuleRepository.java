package com.kaii.dentix.domain.curation.dao;

import com.kaii.dentix.domain.curation.domain.ContentCurationRule;
import com.kaii.dentix.domain.type.AnalysisType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentCurationRuleRepository extends JpaRepository<ContentCurationRule, Long> {
    @Query("""
            select rule from ContentCurationRule rule
            join fetch rule.contents content
            where rule.analysisType = :analysisType
              and rule.resultKey in :resultKeys
              and rule.active = true
              and content.deleted is null
            order by content.contentsSort asc, content.contentsId asc
            """)
    List<ContentCurationRule> findQuestionnaireRules(
            @Param("analysisType") AnalysisType analysisType,
            @Param("resultKeys") List<String> resultKeys
    );

    @Query("""
            select rule from ContentCurationRule rule
            join fetch rule.contents content
            where rule.analysisType = :analysisType
              and rule.resultKey = :resultKey
              and rule.active = true
              and content.deleted is null
            order by rule.rank asc, content.contentsId asc
            """)
    List<ContentCurationRule> findRankedRules(
            @Param("analysisType") AnalysisType analysisType,
            @Param("resultKey") String resultKey
    );
}
