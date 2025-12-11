package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserRepositoryImpl;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserListDto;
import com.kaii.dentix.domain.admin.dto.AdminUserModifyInfoDto;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserModifyRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckUsageDto;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    private final AdminUserCustomRepository adminUserCustomRepository;

    private final ModelMapper modelMapper;
    private final UserLoginService userLoginService;
    private JwtTokenUtil jwtTokenUtil;
    private final AdminService adminService;
    private final OralCheckRepository oralcheckRepository;
    private final AdminUserRepositoryImpl adminUserRepository;
    /**
     *  사용자 인증
     */
//    @Transactional
//    public void userVerify(Long userId){
//        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));
//
//        if (user.getIsVerify().equals(YnType.Y)) throw new BadRequestApiException("이미 인증된 사용자입니다.");
//
//        user.setIsVerify(YnType.Y);
//    }
    @Transactional(readOnly = true)
    public Page<AdminUserInfoDto> getUsersByOrganization(AdminUserListRequest request) {
        return adminUserRepository.findAllByOrganization(request);
    }
    /**
     *  사용자 정보 조회
     */
    public AdminUserModifyInfoDto userInfo(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

        return AdminUserModifyInfoDto.builder()
            .userLoginIdentifier(user.getUserLoginIdentifier())
            .userName(user.getUserName())
            .userGender(user.getUserGender())
            .build();
    }

    /**
     *  사용자 정보 수정
     */
    @Transactional
    public void userModify(AdminUserModifyRequest request){
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

        if (!user.getUserLoginIdentifier().equals(request.getUserLoginIdentifier())) { // 자신의 아이디가 아닌 경우
            userLoginService.loginIdCheck(request.getUserLoginIdentifier()); // 아이디 중복확인
        }

        user.adminModifyInfo(request.getUserLoginIdentifier(), request.getUserName(), request.getUserGender());
    }

    /**
     *  사용자 삭제
     */
    @Transactional
    public void userDelete(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

        user.revoke();
    }

    /**
     *  사용자 목록 조회
     */
    
//    public AdminUserListDto userList(AdminUserListRequest request){
//        Page<AdminUserInfoDto> userList = adminUserCustomRepository.findAll(request);
//
//        PagingDTO pagingDTO = modelMapper.map(userList, PagingDTO.class);
//
//        return AdminUserListDto.builder()
//                .paging(pagingDTO)
//                .userList(userList.getContent())
//                .build();
//    }
    /**
     * ✅ 사용자 인증
     */
    @Transactional
    public void userVerify(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

        if (user.getIsVerify() == YnType.Y)
            throw new BadRequestApiException("이미 인증된 사용자입니다.");

        user.setIsVerify(YnType.Y);
    }

    @Transactional
    public void userUnverify(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

        if (user.getIsVerify() == YnType.N)
            throw new BadRequestApiException("이미 인증 취소된 사용자입니다.");

        user.setIsVerify(YnType.N);
    }
    /**
     * ✅ 사용자 목록 조회 (슈퍼관리자/기관관리자 구분)
     */
    @Transactional(readOnly = true)
    public AdminUserListDto userList(AdminUserListRequest request, HttpServletRequest servletRequest) {

        Admin admin = adminService.getTokenAdmin(servletRequest);

        // ⭐ page / size null 안전 처리
        if (request.getPage() == null) request.setPage(1);
        if (request.getSize() == null) request.setSize(50);

        Page<AdminUserInfoDto> userList;

        if (admin.isSuperAdmin()) {
            userList = adminUserCustomRepository.findAll(request);
        } else {
            if (admin.getOrganization() == null) {
                throw new BadRequestApiException("소속 기관이 없습니다.");
            }
            request.setOrganizationId(admin.getOrganization().getOrganizationId());
            userList = adminUserCustomRepository.findAll(request);
        }

        PagingDTO pagingDTO = modelMapper.map(userList, PagingDTO.class);

        return AdminUserListDto.builder()
                .paging(pagingDTO)
                .userList(userList.getContent())
                .build();
    }
    /**
     * 구강검진 분석 성공 카운트
     */
    public DataResponse<Map<String, Object>> getOrganizationUserUsage(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        Organization org = admin.getOrganization();

        if (org == null) {
            throw new BadRequestApiException("기관 정보가 없습니다.");
        }

        List<OralCheckUsageDto> usageList =
                oralcheckRepository.findUserUsageByOrganization(org.getOrganizationId());

        // ✅ 총합 계산
        long total = usageList.stream()
                .mapToLong(OralCheckUsageDto::getSuccessCount)
                .sum();

        // ✅ DataResponse로 JSON 반환
        return new DataResponse<>(
                200,
                "기관 사용자 사용량 조회 성공",
                Map.of(
                        "totalCount", total,
                        "users", usageList
                )
        );
    }
}
