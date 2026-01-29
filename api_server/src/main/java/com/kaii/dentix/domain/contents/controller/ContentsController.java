package com.kaii.dentix.domain.contents.controller;

import com.kaii.dentix.domain.contents.application.ContentsService;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contents")
public class ContentsController {

    private final ContentsService contentsService;
    private final UserService userService;

    /**
     * 콘텐츠 목록 조회
     */
    @GetMapping
    public DataResponse<ContentsDto.ListResponse> getContentsList(HttpServletRequest request) {
        // 컨트롤러에서 User 추출 (토큰이 없거나 만료시 null 반환 가능성 있음 - 로직에 따라 처리)
        User user = userService.getTokenUserNullable(request);

        return new DataResponse<>(contentsService.getContentsList(user));
    }

    /**
     * 콘텐츠 카드뉴스 조회
     */
    @GetMapping("/card")
    public DataResponse<ContentsDto.CardListResponse> getContentsCard(@RequestParam Long contentsId) {
        return new DataResponse<>(contentsService.getContentsCard(contentsId));
    }
}