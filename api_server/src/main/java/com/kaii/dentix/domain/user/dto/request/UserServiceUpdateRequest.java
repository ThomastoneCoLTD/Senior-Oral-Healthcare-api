package com.kaii.dentix.domain.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserServiceUpdateRequest {
    private List<Long> serviceIds; // 현재 체크된 서비스들의 ID 목록
}