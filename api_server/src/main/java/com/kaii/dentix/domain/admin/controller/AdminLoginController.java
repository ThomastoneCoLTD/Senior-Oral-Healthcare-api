package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminLoginController {

    private final AdminLoginService adminLoginService;

    @PostMapping(value = "/find-password", name = "어드민 비밀번호 찾기")
    public DataResponse<AdminAuthDto.FindPasswordResponse> adminFindPassword( // 리턴 타입 변경
                                                                              @Valid @RequestBody AdminAuthDto.FindPasswordRequest request // 요청 DTO 변경
    ) {
        return new DataResponse<>(adminLoginService.adminFindPassword(request));
    }

}
