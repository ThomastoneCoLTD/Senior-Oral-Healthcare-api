package com.kaii.dentix.global.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter @Setter @SuperBuilder
@AllArgsConstructor @NoArgsConstructor
public class PageAndSizeRequest {

    private Integer page; // 요청 페이지

    private Integer size; // 한 페이지 당 보여질 개수

}
