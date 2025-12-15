package com.kaii.dentix.domain.userToAppService.dao;

import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.UserServiceUsageDto;
import com.kaii.dentix.domain.userToAppService.domain.UserToAppService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserToAppServiceRepository extends JpaRepository<UserToAppService, Long> {
    List<UserToAppService> findByUser(User user);

    //전체 목록 + 특정 서비스 필터 둘 다 가능
    @Query("""
    SELECT new com.kaii.dentix.domain.user.dto.UserServiceUsageDto(
        u.userId,
        u.userName,
        u.userPhoneNumber,
        o.organizationName,
        s.name,
        COUNT(uas.id)
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
