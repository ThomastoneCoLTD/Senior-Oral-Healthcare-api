package com.kaii.dentix.domain.oralExercise.controller;

import com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService.*;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OralExerciseHistoryController {
    private final OralExerciseHistoryService service;

    @GetMapping("/oral-exercise/history")
    public DataResponse<List<Summary>> mine(HttpServletRequest request) {
        return new DataResponse<>(service.summary(service.currentUser(request)));
    }

    @PostMapping("/oral-exercise/failures")
    public DataResponse<Boolean> failure(HttpServletRequest request, @RequestBody FailureRequest body) {
        service.recordFailure(service.currentUser(request), body, false);
        return new DataResponse<>(true);
    }

    @GetMapping("/admin/oral-exercise-history/users")
    public DataResponse<Page<Member>> members(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return new DataResponse<>(service.members(keyword, page, size));
    }

    @GetMapping("/admin/oral-exercise-history/users/{userId}")
    public DataResponse<List<Summary>> summary(@PathVariable Long userId) {
        return new DataResponse<>(service.summary(userId));
    }

    @GetMapping("/admin/oral-exercise-history/users/{userId}/{contentId}")
    public DataResponse<Page<Event>> events(@PathVariable Long userId, @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page) {
        return new DataResponse<>(service.events(userId, contentId, page));
    }
}
