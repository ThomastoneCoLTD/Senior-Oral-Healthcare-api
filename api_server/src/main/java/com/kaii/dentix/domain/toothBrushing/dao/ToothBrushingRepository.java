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
    @Query(value = "INSERT INTO toothBrushing (userId, created) VALUES (:userId, :created)", nativeQuery = true)
    int nativeInsert(Long userId, Date created);

    @Modifying
    @Transactional
    @Query("UPDATE ToothBrushing tb SET tb.created = :created, tb.modified = :modified WHERE tb.toothBrushingId = :toothBrushingId")
    int updateCreated(Long toothBrushingId, Date created, Date modified);

    List<ToothBrushing> findByUserIdAndCreatedStartingWithOrderByCreated(Long userId, String createdPrefix);
}
