package com.kaii.dentix.domain.userServiceAgreement.application;

import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.userServiceAgreement.dao.UserServiceAgreementRepository;
import com.kaii.dentix.domain.userServiceAgreement.domain.UserServiceAgreement;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreeList;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreementResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceAgreementService {

    private final UserServiceAgreementRepository userServiceAgreementRepository;
//    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<UserServiceAgreementResponse> getUserServiceAgreements(HttpServletRequest httpServletRequest) {
        User user = userService.getTokenUser(httpServletRequest);
        Long currentUserId = user.getUserId();

        return userServiceAgreementRepository.findAllByUserIdWithServiceName(currentUserId);
    }
}