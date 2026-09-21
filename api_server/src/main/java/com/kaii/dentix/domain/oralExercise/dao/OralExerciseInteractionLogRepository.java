package com.kaii.dentix.domain.oralExercise.dao;

import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Date;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionEventType;

public interface OralExerciseInteractionLogRepository extends JpaRepository<OralExerciseInteractionLog, Long> {
    boolean existsByUserIdAndContent_OralExerciseContentIdAndSessionIdAndEventTypeIn(
            Long userId, Long contentId, String sessionId, List<OralExerciseInteractionEventType> types);

    interface Counts {
        Long getContentId();
        long getCompletedViews();
        long getFailures();
        long getWrongCount();
        long getTimeoutCount();
        long getErrorCount();
        Date getLastViewedAt();
    }

    @Query("""
        select l.content.oralExerciseContentId as contentId,
        count(distinct case when l.eventType = 'COMPLETE' and l.completed = true then l.sessionId end) as completedViews,
        count(distinct case when l.eventType in ('TOKEN_WRONG','TOKEN_TIMEOUT','TOKEN_FAILED') then l.sessionId end) as failures,
        count(distinct case when l.eventType = 'TOKEN_WRONG' then l.sessionId end) as wrongCount,
        count(distinct case when l.eventType = 'TOKEN_TIMEOUT' then l.sessionId end) as timeoutCount,
        count(distinct case when l.eventType = 'TOKEN_FAILED' then l.sessionId end) as errorCount,
        max(l.created) as lastViewedAt
        from OralExerciseInteractionLog l where l.userId = :userId
        group by l.content.oralExerciseContentId
        """)
    List<Counts> summarize(Long userId);

    Page<OralExerciseInteractionLog> findByUserIdAndContent_OralExerciseContentIdAndEventTypeInOrderByCreatedDescOralExerciseInteractionLogIdDesc(
            Long userId, Long contentId, List<OralExerciseInteractionEventType> types, Pageable pageable);
}
