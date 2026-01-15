package com.kaii.dentix.domain.admin.dao;

import org.springframework.data.domain.Page;
import com.kaii.dentix.domain.admin.dto.AdminAccountDto;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;

public interface AdminCustomRepository {
    Page<AdminAccountDto> findAllByNotSuper(PageAndSizeRequest request);
}
