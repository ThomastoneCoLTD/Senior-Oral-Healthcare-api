package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminOrganizationUsageResponse;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserListDto;
import com.kaii.dentix.domain.admin.dto.AdminUserModifyInfoDto;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserModifyRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    private final AdminUserCustomRepository adminUserCustomRepository;

    private final ModelMapper modelMapper;
    private final UserLoginService userLoginService;
    private JwtTokenUtil jwtTokenUtil;
private final AdminService adminService;
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

    /**
     * ✅ 사용자 목록 조회 (슈퍼관리자/기관관리자 구분)
     */
    @Transactional(readOnly = true)
    public AdminUserListDto userList(AdminUserListRequest request, HttpServletRequest servletRequest) {
        Admin admin = adminService.getTokenAdmin(servletRequest);
        Page<AdminUserInfoDto> userList;

        if (admin.isSuperAdmin()) {
            // ✅ 슈퍼관리자 → 모든 기관 사용자 조회
            userList = adminUserCustomRepository.findAll(request);
        } else {
            // ✅ 일반 관리자 → 본인 기관만
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

}
