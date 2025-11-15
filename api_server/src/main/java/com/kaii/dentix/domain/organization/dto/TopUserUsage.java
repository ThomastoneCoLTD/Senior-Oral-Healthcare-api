package com.kaii.dentix.domain.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopUserUsage {
    private Long userId;
    private Long count;
}