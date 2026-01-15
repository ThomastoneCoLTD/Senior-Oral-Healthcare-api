package com.kaii.dentix.domain.appService.dao;

import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.UserServiceUsageDto;
import com.kaii.dentix.domain.appService.domain.UserToAppService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserToAppServiceRepository extends JpaRepository<UserToAppService, Long> {
    List<UserToAppService> findByUser(User user);
    //User ID로 조회하는 메서드
    List<UserToAppService> findByUser_UserId(Long userId);

    //중복 연동 방지 체크용 메서드
    boolean existsByUserAndAppService(User user, AppService appService);
    //전체 목록 + 특정 서비스 필터 둘 다 가능
    @Query("""
        SELECT new com.kaii.dentix.domain.user.dto.UserServiceUsageDto(
            u.userId, u.userName, u.userPhoneNumber, o.organizationName, s.name, COUNT(uas.id)
        )
        FROM UserToAppService uas
        JOIN uas.user u
        JOIN u.organization o
        JOIN uas.appService s
        WHERE (:serviceName IS NULL OR s.name = :serviceName)
        GROUP BY u.userId, u.userName, u.userPhoneNumber, o.organizationName, s.name
    """)
    List<UserServiceUsageDto> findUsageByServiceName(@Param("serviceName") String serviceName);
}
