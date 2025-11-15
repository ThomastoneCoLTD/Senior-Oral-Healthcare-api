package com.kaii.dentix.domain.contents.dto;

import com.kaii.dentix.domain.type.ContentsType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentsDto {

    private Long id;                // int -> Long
    private String title;
    private Integer sort;           // int -> Integer
    private ContentsType type;
    private String typeColor;
    private String thumbnail;
    private String videoURL;
    private List<Integer> categoryIds;
}