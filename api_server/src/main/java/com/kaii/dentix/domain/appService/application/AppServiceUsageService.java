package com.kaii.dentix.domain.appService.application;


import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.user.dto.UserServiceUsageDto;
import com.kaii.dentix.domain.userToAppService.dao.UserToAppServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppServiceUsageService {

    private final UserToAppServiceRepository userToAppServiceRepository;

    @Transactional(readOnly = true)
    public List<UserServiceUsageDto> getServiceUserUsage(String serviceName) {
        // 하나의 메서드만 호출
        return userToAppServiceRepository.findUsageByServiceName(serviceName);
    }
}