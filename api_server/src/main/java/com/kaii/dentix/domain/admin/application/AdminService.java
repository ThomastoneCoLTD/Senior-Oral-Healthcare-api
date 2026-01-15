package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminAccountDto;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.domain.admin.dto.AdminDto;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.global.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ModelMapper modelMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final FindPwdQuestionRepository findPwdQuestionRepository;

    /**
     * 토큰에서 Admin 추출
     */
    @Transactional(readOnly = true)
    public Admin getTokenAdmin(HttpServletRequest servletRequest) {
        String token = jwtTokenUtil.getAccessToken(servletRequest);
        UserRole role = jwtTokenUtil.getRoles(token, TokenType.AccessToken);

        // 관리자 또는 슈퍼관리자만 접근 가능
        if (!(role == UserRole.ROLE_ADMIN || role == UserRole.ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("관리자 권한이 필요합니다.");
        }

        Long adminId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);

        return adminRepository.findByIdWithOrganizationAndPlan(adminId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
    }

    /**
     * 로그인한 관리자의 기관 정보 조회
     */
    public Organization getMyOrganization(Admin admin) {
        if (admin.getOrganization() == null) {
            throw new IllegalArgumentException("해당 관리자는 기관에 소속되어 있지 않습니다.");
        }
        return admin.getOrganization();
    }

    /**
     * 관리자 등록
     */
    @Transactional
    public AdminAuthDto.SignUpResponse adminSignUp(AdminAuthDto.SignUpRequest request) {
        // 연락처 중복 확인
        Optional<Admin> existAdmin = adminRepository.findByAdminPhoneNumber(request.getPhoneNumber());
        if (existAdmin.isPresent()) {
            if (existAdmin.get().getAdminName().equals(request.getName())) {
                throw new AlreadyDataException("이미 가입한 관리자입니다.");
            }
            throw new BadRequestApiException("이미 사용중인 번호에요. 번호를 다시 확인해 주세요.");
        }

        // 아이디 중복 확인
        if (adminRepository.findByAdminLoginIdentifier(request.getLoginId()).isPresent()) {
            throw new AlreadyDataException("이미 존재하는 아이디입니다.");
        }

        // 비밀번호 찾기 질문 유효성 검사
        if (findPwdQuestionRepository.findById(request.getFindPwdQuestionId()).isEmpty()) {
            throw new NotFoundDataException("존재하지 않는 비밀번호 찾기 질문입니다.");
        }

        // 관리자 저장
        Admin admin = adminRepository.save(Admin.builder()
                .adminName(request.getName())
                .adminLoginIdentifier(request.getLoginId())
                .adminPhoneNumber(request.getPhoneNumber())
                .adminPassword(passwordEncoder.encode(request.getPassword()))
                .findPwdQuestionId(request.getFindPwdQuestionId())
                .findPwdAnswer(request.getFindPwdAnswer())
                .adminIsSuper(YnType.N)
                .organization(null)
                .build());

        // DTO의 정적 팩토리 메서드 사용
        return AdminAuthDto.SignUpResponse.of(admin);
    }

    /**
     * 관리자 비밀번호 변경
     */
    @Transactional
    public void adminModifyPassword(HttpServletRequest httpServletRequest, AdminAuthDto.ModifyPasswordRequest request) {
        Admin admin = this.getTokenAdmin(httpServletRequest);
        admin.updatePassword(passwordEncoder, request.getPassword());
    }

    /**
     * 관리자 삭제
     */
    @Transactional
    public void adminDelete(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
        admin.deleteAdmin();
    }

    /**
     * 관리자 비밀번호 초기화
     */
    @Transactional
    public AdminAuthDto.ModifyPasswordRequest adminPasswordReset(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));

        // 기본 비밀번호로 초기화
        admin.updatePassword(passwordEncoder, SecurityUtil.defaultPassword);

        // 초기화된 비밀번호 반환 (임시로 ModifyPasswordRequest 활용하거나 별도 Response 생성 권장)
        AdminAuthDto.ModifyPasswordRequest response = new AdminAuthDto.ModifyPasswordRequest();
        response.setPassword(SecurityUtil.defaultPassword);
        return response;
    }

    /**
     * 관리자 목록 조회
     * (AdminListDto, AdminAccountDto는 아직 통합되지 않았으므로 기존 유지)
     */
    @Transactional(readOnly = true)
    public AdminDto.ListResponse adminList(AdminDto.SearchRequest request) {
        Page<AdminAccountDto> pageResult = adminRepository.findAllByNotSuper(request);
        List<AdminDto.Summary> summaryList = pageResult.getContent().stream()
                .map(a -> AdminDto.Summary.builder()
                        .adminId(a.getAdminId())
                        .loginId(a.getAdminLoginIdentifier())
                        .name(a.getAdminName())
                        .phoneNumber(a.getAdminPhoneNumber())
                        .createdDate(a.getCreated())
                        .build())
                .toList();

        PagingDTO pagingDTO = modelMapper.map(pageResult, PagingDTO.class);

        return AdminDto.ListResponse.of(pagingDTO, summaryList);
    }

    /**
     * 관리자 자동 로그인
     */
    public AdminAuthDto.AutoLoginResponse adminAutoLogin(HttpServletRequest httpServletRequest) {
        Admin admin = this.getTokenAdmin(httpServletRequest);
        String accessToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(admin, TokenType.RefreshToken);
        admin.updateAdminLogin(refreshToken);

        return AdminAuthDto.AutoLoginResponse.from(admin, accessToken, refreshToken);
    }
}