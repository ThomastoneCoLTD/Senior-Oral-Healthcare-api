package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminStatisticDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminUserCustomRepository {

    // 1. 사용자 관리 (AdminUserDto 사용)
    Page<AdminUserDto.Info> findAll(AdminUserDto.SearchRequest request);

    Page<AdminUserDto.Info> findAllByOrganization(AdminUserDto.SearchRequest request);

    // 2. 통계 (AdminStatisticDto 사용) - 파라미터와 리턴타입 모두 변경
    AdminStatisticDto.SignUpCount userSignUpCount(AdminStatisticDto.SearchRequest request);

    // 슈퍼관리자용 기관별 통계 리스트
    List<SuperAdminStatisticDto.OrgUserStats> getAllOrganizationUserStats();
}