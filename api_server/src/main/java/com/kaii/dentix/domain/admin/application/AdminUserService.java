package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.dto.AdminUserInfoDto;
import com.kaii.dentix.domain.admin.dto.AdminUserListDto;
import com.kaii.dentix.domain.admin.dto.AdminUserModifyInfoDto;
import com.kaii.dentix.domain.admin.dto.request.AdminUserListRequest;
import com.kaii.dentix.domain.admin.dto.request.AdminUserModifyRequest;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    private final AdminUserCustomRepository adminUserCustomRepository;

    private final ModelMapper modelMapper;
    private final UserLoginService userLoginService;
    private JwtTokenUtil jwtTokenUtil;

    /**
     *  사용자 인증
     */
    @Transactional
    public void userVerify(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

        if (user.getIsVerify().equals(YnType.Y)) throw new BadRequestApiException("이미 인증된 사용자입니다.");

        user.setIsVerify(YnType.Y);
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
    public AdminUserListDto userList(AdminUserListRequest request){
        Page<AdminUserInfoDto> userList = adminUserCustomRepository.findAll(request);

        PagingDTO pagingDTO = modelMapper.map(userList, PagingDTO.class);

        return AdminUserListDto.builder()
                .paging(pagingDTO)
                .userList(userList.getContent())
                .build();
    }

//    public ResponseEntity<AdminUserListDto> getUsersByOrganization(
//            @ModelAttribute AdminUserListRequest request,
//            HttpServletRequest httpRequest
//    ) {
//        // ✅ 1. JWT에서 기관 ID 추출
//        Long organizationId = jwtTokenUtil.getOrganizationIdFromAccessToken(httpRequest);
//        if (organizationId == null) {
//            throw new NotFoundDataException("기관 정보가 없습니다. 관리자 토큰을 확인해주세요.");
//        }
//
//        // ✅ 2. 요청 객체에 기관 ID 주입
//        request.setOrganizationId(organizationId);

        // ✅ 3. 기존 서비스 그대로 호출 (페이징 + 검색 포함)
//        AdminUserListDto result = adminUserService.userList(request);

//        return ResponseEntity.ok(result);
//    }
}
