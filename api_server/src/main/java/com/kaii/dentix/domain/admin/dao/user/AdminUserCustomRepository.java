package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserSignUpCountDto;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.admin.dto.superAdmin.SuperAdminUserStatisticResponse;
import com.kaii.dentix.domain.user.domain.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminUserCustomRepository {

    Page<AdminUserInfoDto> findAll(AdminUserListRequest request);
    Page<AdminUserInfoDto> findAllByOrganization(AdminUserListRequest request); // 기관별 조회
    AdminUserSignUpCountDto userSignUpCount(AdminStatisticRequest request);
    List<SuperAdminUserStatisticResponse> getAllOrganizationUserStats();
}
