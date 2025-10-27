package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminCustomRepository;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.*;
import com.kaii.dentix.domain.admin.dto.request.AdminModifyPasswordRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminSignUpRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationUpdateRequest;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    private final JwtTokenUtil jwtTokenUtil;

    private final PasswordEncoder passwordEncoder;


    private final ModelMapper modelMapper;
private AdminService adminService;
private AdminUserCustomRepository adminUserCustomRepository;

    /**
     * 토큰에서 Admin 추출
     */
    @Transactional(readOnly = true)
    public Admin getTokenAdmin(HttpServletRequest servletRequest) {
        String token = jwtTokenUtil.getAccessToken(servletRequest);
        UserRole role = jwtTokenUtil.getRoles(token, TokenType.AccessToken);

        // ✅ 관리자 또는 슈퍼관리자만 접근 가능
        if (!(role == UserRole.ROLE_ADMIN || role == UserRole.ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("관리자 권한이 필요합니다.");
        }

        Long adminId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);

        return adminRepository.findByIdWithOrganizationAndPlan(adminId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
    }
    /**
     *  관리자 등록
     */
    @Transactional
    public AdminSignUpDto adminSignUp(AdminSignUpRequest request){

        Optional<Admin> existAdmin = adminRepository.findByAdminPhoneNumber(request.getAdminPhoneNumber());

        if (existAdmin.isPresent()){
            // 이미 가입된 사용자의 경우
            if (existAdmin.get().getAdminName().equals(request.getAdminName())) throw new AlreadyDataException("이미 가입한 관리자입니다.");

            // 연락처 중복 확인
            throw new BadRequestApiException("이미 사용중인 번호에요. 번호를 다시 확인해 주세요.");
        }

        // 아이디 중복 확인
        if (adminRepository.findByAdminLoginIdentifier(request.getAdminLoginIdentifier()).isPresent()) throw new AlreadyDataException("이미 존재하는 아이디입니다.");

        Admin admin = Admin.builder()
                .adminName(request.getAdminName())
                .adminLoginIdentifier(request.getAdminLoginIdentifier())
                .adminPhoneNumber(request.getAdminPhoneNumber())
                .adminIsSuper(YnType.N)
                .organization(null)
                .build();

        adminRepository.save(admin);

        return AdminSignUpDto.builder()
                .adminId(admin.getAdminId())
                .adminPassword(SecurityUtil.defaultPassword)
                .build();

    }

    /**
     *  관리자 비밀번호 변경 
     */
    @Transactional
    public void adminModifyPassword(HttpServletRequest httpServletRequest, AdminModifyPasswordRequest request){
        Admin admin = this.getTokenAdmin(httpServletRequest);

        admin.updatePassword(passwordEncoder, request.getAdminPassword());
    }

    /**
     *  관리자 삭제
     */
    @Transactional
    public void adminDelete(Long adminId){
        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
        admin.deleteAdmin();
    }

    /**
     *  관리자 비밀번호 초기화
     */
    @Transactional
    public AdminPasswordResetDto adminPasswordReset(Long adminId){
        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
        admin.updatePassword(passwordEncoder, SecurityUtil.defaultPassword);

        return AdminPasswordResetDto.builder()
                .adminPassword(SecurityUtil.defaultPassword)
                .build();
    }

    /**
     *  관리자 목록 조회
     */
    public AdminListDto adminList(PageAndSizeRequest request){
        Page<AdminAccountDto> adminList = adminRepository.findAllByNotSuper(request);

        PagingDTO pagingDTO = modelMapper.map(adminList, PagingDTO.class);

        return AdminListDto.builder()
                .paging(pagingDTO)
                .adminList(adminList.getContent())
                .build();
    }

    /**
     *  관리자 자동 로그인
     */
    public AdminAutoLoginDto adminAutoLogin(HttpServletRequest httpServletRequest){
        Admin admin = this.getTokenAdmin(httpServletRequest);

        String accessToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(admin, TokenType.RefreshToken);

        admin.updateAdminLogin(refreshToken);

        return AdminAutoLoginDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())
                .adminIsSuper(admin.getAdminIsSuper())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserListDto userList(AdminUserListRequest request, HttpServletRequest servletRequest) {

        // ✅ 현재 로그인한 관리자 정보 가져오기
        Admin admin = adminService.getTokenAdmin(servletRequest);

        // ✅ 권한 판단
        boolean isSuperAdmin = admin.getAdminIsSuper() == YnType.Y;

        Page<AdminUserInfoDto> userList;

        // ✅ 슈퍼관리자면 전체 사용자 조회
        if (isSuperAdmin) {
            userList = adminUserCustomRepository.findAll(request); // 모든 기관 포함
        }
        // ✅ 일반관리자면 자신의 기관 사용자만 조회
        else {
            if (admin.getOrganization() == null) {
                throw new BadRequestApiException("소속 기관이 없습니다.");
            }
            Long orgId = admin.getOrganization().getOrganizationId();
            request.setOrganizationId(orgId);
            userList = adminUserCustomRepository.findAll(request);
        }

        PagingDTO pagingDTO = modelMapper.map(userList, PagingDTO.class);

        return AdminUserListDto.builder()
                .paging(pagingDTO)
                .userList(userList.getContent())
                .build();
    }



}
