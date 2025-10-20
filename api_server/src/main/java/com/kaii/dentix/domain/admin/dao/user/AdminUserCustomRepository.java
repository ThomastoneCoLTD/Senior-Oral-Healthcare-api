package com.kaii.dentix.domain.admin.dao.user;

import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserSignUpCountDto;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.user.domain.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminUserCustomRepository {

    Page<AdminUserInfoDto> findAll(AdminUserListRequest request);

    AdminUserSignUpCountDto userSignUpCount(AdminStatisticRequest request);

}
