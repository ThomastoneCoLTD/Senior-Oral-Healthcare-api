package com.kaii.dentix.domain.findPwdQuestion.controller;

import com.kaii.dentix.domain.findPwdQuestion.application.FindPwdService;
import com.kaii.dentix.domain.findPwdQuestion.dto.FindPwdQuestionDto;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/password")
public class FindPwdController {

    private final FindPwdService findPwdService;

    /**
     *  사용자 비밀번호 찾기 질문 리스트
     */
    @GetMapping(value = "/questions", name = "사용자 비밀번호 찾기 질문 리스트")
    public DataResponse<FindPwdQuestionDto.Response> userFindPwdQuestions() {
        return new DataResponse<>(findPwdService.userFindPwdQuestions());
    }

}
