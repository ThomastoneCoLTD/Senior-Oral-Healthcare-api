package com.kaii.dentix.global.common.dto;
import lombok.*;
import org.springframework.data.domain.PageRequest;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagingRequest {

    private Integer page = 1;
    private Integer size = 10;

    public PageRequest of() {

        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size < 1) ? 10 : size;

        // 1-based → 0-based
        return PageRequest.of(safePage - 1, safeSize);
    }
}