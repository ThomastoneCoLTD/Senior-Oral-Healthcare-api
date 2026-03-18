package com.kaii.dentix.domain.toothBrushing.dao;

import com.kaii.dentix.domain.toothBrushing.domain.ToothBrushing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface ToothBrushingRepository extends JpaRepository<ToothBrushing, Long> {

    @Query(value = "SELECT a FROM ToothBrushing a WHERE a.userId = :userId AND FUNCTION('DATE_FORMAT', a.created, '%Y-%m-%d') = :created ORDER BY a.created")
    List<ToothBrushing> findByUserIdAndCreatedOrderByCreated(Long userId, String created);

    List<ToothBrushing> findAllByUserIdOrderByCreatedDesc(Long userId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO tooth_brushing (user_id, created, modified) VALUES (:userId, :created, :modified)", nativeQuery = true)
    int nativeInsert(Long userId, Date created, Date modified);

    default int nativeInsert(Long userId, Date created) {
        return nativeInsert(userId, created, created);
    }

    List<ToothBrushing> findByUserIdAndCreatedStartingWithOrderByCreated(Long userId, String createdPrefix);
}
