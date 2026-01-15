package com.kaii.dentix.domain.appService.application;


import com.kaii.dentix.domain.appService.dao.AppServiceRepository;
import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.appService.dto.AppServiceDto;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.appService.dao.UserToAppServiceRepository;
import com.kaii.dentix.domain.appService.domain.UserToAppService;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
@Service
@RequiredArgsConstructor
public class AppServiceUsageService {

    private final AppServiceRepository appServiceRepository;
    private final UserToAppServiceRepository userToAppServiceRepository;
    private final UserRepository userRepository;

    /**
     * 앱 서비스 목록 및 나의 연동 현황 조회
     */
    @Transactional(readOnly = true)
    public List<AppServiceDto.UsageStatus> getAppServiceUsageList(Long userId) {

        // 1. 전체 지원 서비스 목록 조회 (카카오, 네이버 등)
        List<AppService> allServices = appServiceRepository.findAll();

        // 2. 사용자가 이미 연동한 내역 조회 (Map 변환: ServiceId -> 연동정보)
        Map<Long, UserToAppService> myLinks = userToAppServiceRepository.findByUser_UserId(userId).stream()
                .collect(Collectors.toMap(
                        link -> link.getAppService().getAppServiceId(),
                        link -> link
                ));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
        // 3. 전체 목록을 순회하며 연동 여부(Y/N)와 날짜 매핑
        return allServices.stream()
                .map(service -> {
                    UserToAppService link = myLinks.get(service.getAppServiceId());
                    boolean isConnected = (link != null);

                    //날짜 변환 로직 수정 (Date -> String)
                    String dateStr = null;
                    if (isConnected && link.getCreated() != null) {
                        dateStr = dateFormat.format(link.getCreated());
                    }

                    return AppServiceDto.UsageStatus.builder()
                            .appServiceId(service.getAppServiceId())
                            .serviceName(service.getName())
                            .serviceType(service.getServiceType())
                            .isConnected(isConnected ? YnType.Y : YnType.N)
                            .connectedDate(dateStr)
                            .build();
                })
                .toList();
    }

    /**
     * 앱 서비스 연동하기
     */
    @Transactional
    public void connectAppService(Long userId, AppServiceDto.ConnectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

        AppService appService = appServiceRepository.findById(request.getAppServiceId())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 서비스입니다."));

        // 이미 연동된 경우 중복 저장 방지
        if (userToAppServiceRepository.existsByUserAndAppService(user, appService)) {
            return;
        }

        // 연동 정보 저장 (TimeEntity 덕분에 생성 시간 자동 기록)
        userToAppServiceRepository.save(UserToAppService.builder()
                .user(user)
                .appService(appService)
                .build());
    }
}