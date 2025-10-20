package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminLoginDto;
import com.kaii.dentix.domain.admin.dto.request.AdminLoginRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.common.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AdminLoginService {
    private final JwtTokenUtil jwtTokenUtil;

    private final AdminRepository adminRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     *  관리자 로그인
     */
    @Transactional
    public AdminLoginDto adminLogin(AdminLoginRequest request){
        Admin admin = adminRepository.findByAdminLoginIdentifier(request.getAdminLoginIdentifier()).orElseThrow(() -> new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요."));

        YnType isFirstLogin = admin.getAdminLastLoginDate() == null ? YnType.Y : YnType.N;

        // 처음 로그인 시도인 경우
        if (admin.getAdminPassword() == null || admin.getAdminPassword().isEmpty()){
            isFirstLogin = YnType.Y; // 비밀번호 초기화를 위해
            admin.updatePassword(passwordEncoder, SecurityUtil.defaultPassword);
        }

        if (!passwordEncoder.matches(request.getAdminPassword(), admin.getAdminPassword())){
            throw new UnauthorizedException("입력하신 정보가 일치하지 않습니다. 다시 확인해주세요.");
        }

        String accessToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(admin, TokenType.RefreshToken);

        admin.updateAdminLogin(refreshToken);

        return AdminLoginDto.builder()
                .isFirstLogin(isFirstLogin)
                .adminId(admin.getAdminId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .adminName(admin.getAdminName())
                .adminIsSuper(admin.getAdminIsSuper())
                .build();

    }

}
