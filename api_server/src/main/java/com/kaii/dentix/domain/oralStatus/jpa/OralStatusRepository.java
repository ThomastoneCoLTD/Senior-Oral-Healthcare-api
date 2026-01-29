package com.kaii.dentix.domain.oralStatus.jpa;

import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OralStatusRepository extends JpaRepository<OralStatus, String> {
    /**
     * 구강 상태 타입 목록에 해당하는 데이터를 우선순위 순으로 조회
     */
    List<OralStatus> findAllByOralStatusTypeInOrderByOralStatusPriority(List<String> oralStatusTypeList);
}
