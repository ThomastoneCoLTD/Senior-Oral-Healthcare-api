package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.*;
import com.kaii.dentix.domain.admin.dto.request.AdminModifyPasswordRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminSignUpRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationReResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.type.SubscriptionStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    private final JwtTokenUtil jwtTokenUtil;

    private final PasswordEncoder passwordEncoder;
    private final FindPwdQuestionRepository findPwdQuestionRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    private final ModelMapper modelMapper;
    private AdminService adminService;
    private AdminUserCustomRepository adminUserCustomRepository;
    private final OrganizationRepository organizationRepository;
    /**
     * 토큰에서 Admin 추출
     */
    @Transactional(readOnly = true)
    public Admin getTokenAdmin(HttpServletRequest servletRequest) {
        String token = jwtTokenUtil.getAccessToken(servletRequest);
        UserRole role = jwtTokenUtil.getRoles(token, TokenType.AccessToken);

        //관리자 또는 슈퍼관리자만 접근 가능
        if (!(role == UserRole.ROLE_ADMIN || role == UserRole.ROLE_SUPER_ADMIN)) {
            throw new UnauthorizedException("관리자 권한이 필요합니다.");
        }

        Long adminId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);

        return adminRepository.findByIdWithOrganizationAndPlan(adminId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
    }

    /**
     *로그인한 관리자의 기관 정보 조회
     */
    public OrganizationReResponse getMyOrganization(Admin admin) {
        Organization org = admin.getOrganization();
        if (org == null) {
            throw new IllegalArgumentException("해당 관리자는 기관에 소속되어 있지 않습니다.");
        }

        OrganizationSubscriptionHistory currentHistory =
                organizationSubscriptionHistoryRepository
                        .findTopByOrganization_OrganizationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanOrderByStartDateDesc(
                                org.getOrganizationId(),
                                SubscriptionStatus.ACTIVE,
                                LocalDateTime.now(),
                                LocalDateTime.now()
                        )
                        .orElse(null);
        return OrganizationReResponse.from(org, currentHistory);
    }

    /**
     *  관리자 등록
     */
    @Transactional
    public AdminSignUpDto adminSignUp(AdminSignUpRequest request) {

        //연락처 중복 확인
        Optional<Admin> existAdmin = adminRepository.findByAdminPhoneNumber(request.getAdminPhoneNumber());
        if (existAdmin.isPresent()) {
            if (existAdmin.get().getAdminName().equals(request.getAdminName()))
                throw new AlreadyDataException("이미 가입한 관리자입니다.");
            throw new BadRequestApiException("이미 사용중인 번호에요. 번호를 다시 확인해 주세요.");
        }

        //아이디 중복 확인
        if (adminRepository.findByAdminLoginIdentifier(request.getAdminLoginIdentifier()).isPresent())
            throw new AlreadyDataException("이미 존재하는 아이디입니다.");

        //비밀번호 찾기 질문 유효성 검사
        if (findPwdQuestionRepository.findById(request.getFindPwdQuestionId()).isEmpty()) {
            throw new NotFoundDataException("존재하지 않는 비밀번호 찾기 질문입니다.");
        }

        //관리자 저장
        Admin admin = adminRepository.save(Admin.builder()
                .adminName(request.getAdminName())
                .adminLoginIdentifier(request.getAdminLoginIdentifier())
                .adminPhoneNumber(request.getAdminPhoneNumber())
                .adminPassword(passwordEncoder.encode(request.getAdminPassword())) // 입력된 비밀번호 저장
                .findPwdQuestionId(request.getFindPwdQuestionId()) // 질문 ID
                .findPwdAnswer(request.getFindPwdAnswer())         //질문 답변
                .adminIsSuper(YnType.N)
                .organization(null)
                .build());

        //응답 반환
        return AdminSignUpDto.builder()
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())
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

    @Transactional
    public void adminResetPassword(AdminResetPasswordRequest request){
        Admin admin = adminRepository.findById(request.getAdminId())
                .orElseThrow(() -> new NotFoundDataException("관리자를 찾을 수 없습니다."));

        admin.updatePassword(passwordEncoder, request.getNewPassword());
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

        //현재 로그인한 관리자 정보 가져오기
        Admin admin = adminService.getTokenAdmin(servletRequest);

        //권한 판단
        boolean isSuperAdmin = admin.getAdminIsSuper() == YnType.Y;

        Page<AdminUserInfoDto> userList;

        //슈퍼관리자면 전체 사용자 조회
        if (isSuperAdmin) {
            userList = adminUserCustomRepository.findAll(request); // 모든 기관 포함
        }
        //일반관리자면 자신의 기관 사용자만 조회
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

    /** 일반관리자 - 기관등록 */
//    @Transactional
//    public void assignCurrentAdminToOrganization(Organization organization) {
//
//        Long adminId = jwtTokenUtil.getCurrentAdminId();
//        Admin admin = adminRepository.findById(adminId)
//                .orElseThrow(() -> new IllegalStateException("관리자 없음"));
//
//        if (admin.getOrganization() != null) {
//            throw new IllegalStateException("이미 기관에 소속됨");
//        }
//
//        admin.setOrganization(organization);
//    }
}
