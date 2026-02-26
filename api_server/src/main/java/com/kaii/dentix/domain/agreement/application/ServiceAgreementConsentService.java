package com.kaii.dentix.domain.agreement.application;

import com.kaii.dentix.domain.agreement.dao.ServiceAgreementConsentRepository;
import com.kaii.dentix.domain.agreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.agreement.domain.ServiceAgreement;
import com.kaii.dentix.domain.agreement.domain.ServiceAgreementConsent;
import com.kaii.dentix.domain.agreement.dto.ServiceAgreementConsentDto;
import com.kaii.dentix.domain.agreement.dto.ServiceAgreementDto;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceAgreementConsentService {
    private final UserService userService;
    private final ServiceAgreementService serviceAgreementService;
    private final ServiceAgreementConsentRepository serviceAgreementConsentRepository;
    private final ServiceAgreementRepository serviceAgreementRepository;
    /**
     *  사용자 서비스 이용동의 여부 수정
     */
    @Transactional
    public ServiceAgreementConsentDto.ModifyResponse userModifyServiceAgree(HttpServletRequest httpServletRequest, ServiceAgreementConsentDto.ModifyRequest request){
        User user = userService.getTokenUser(httpServletRequest);

        ServiceAgreement serviceAgreement = serviceAgreementRepository.findById(request.getServiceAgreeId()).orElseThrow(() -> new NotFoundDataException("존재하지 않는 서비스 이용 동의입니다."));
        if (serviceAgreement.getIsServiceAgreeRequired().equals(YnType.Y)) throw new BadRequestApiException("필수 항목은 수정할 수 없습니다.");

        ServiceAgreementConsent serviceAgreementConsent = serviceAgreementConsentRepository.findByServiceAgreeIdAndUserId(serviceAgreement.getServiceAgreeId(), user.getUserId()).orElse(null);

        if (serviceAgreementConsent == null) {
            serviceAgreementConsent = serviceAgreementConsentRepository.save(ServiceAgreementConsent.builder()
                    .userId(user.getUserId())
                    .serviceAgreeId(serviceAgreement.getServiceAgreeId())
                    .isUserServiceAgree(request.getIsUserServiceAgree())
                    .userServiceAgreeDate(new Date())
                    .build());
        } else {
            serviceAgreementConsent.modifyServiceAgree(request.getIsUserServiceAgree());
        }

        return ServiceAgreementConsentDto.ModifyResponse.builder()
                .serviceAgreeId(serviceAgreementConsent.getServiceAgreeId())
                .isUserServiceAgree(serviceAgreementConsent.getIsUserServiceAgree())
                .date(serviceAgreementConsent.getUserServiceAgreeDate())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ServiceAgreementConsentDto.Response> getUserServiceAgreements(HttpServletRequest httpServletRequest) {
        User user = userService.getTokenUser(httpServletRequest);
        Long currentUserId = user.getUserId();

        return serviceAgreementConsentRepository.findAllByUserIdWithServiceName(currentUserId);
    }

    //서비스 약관 저장
    @Transactional
    public void saveUserServiceAgreements(Long userId, List<Long> request) {
        List<ServiceAgreementDto.Response> list =
                serviceAgreementService.serviceAgreementList().getServiceAgreement();

        // 1) 요청 id가 실제 약관 목록에 존재하는지 검증
        boolean hasInvalid = request.stream()
                .anyMatch(reqId -> list.stream().noneMatch(d -> d.getId().equals(reqId)));

        if (hasInvalid) {
            throw new NotFoundDataException("존재하지 않는 서비스 이용 동의입니다.");
        }

        // 2) 필수 약관 누락 검증
        list.forEach(agree -> {
            if (agree.getIsServiceAgreeRequired() == YnType.Y && !request.contains(agree.getId())) {
                throw new BadRequestApiException(agree.getName() + "는(은) 필수 항목입니다.");
            }
        });

        // 3) 저장 (전체 약관 기준으로 Y/N 저장)
        Date now = new Date();
        list.forEach(agree -> serviceAgreementConsentRepository.save(
                ServiceAgreementConsent.builder()
                        .userId(userId)
                        .serviceAgreeId(agree.getId())
                        .isUserServiceAgree(request.contains(agree.getId()) ? YnType.Y : YnType.N)
                        .userServiceAgreeDate(now)
                        .build()
        ));
    }
}
