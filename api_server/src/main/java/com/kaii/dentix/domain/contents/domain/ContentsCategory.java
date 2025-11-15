package com.kaii.dentix.domain.contents.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *  콘텐츠 카테고리
 */
@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(name = "contents_category")
public class ContentsCategory extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int contentsCategoryId;

    @Column(nullable = false)
    private int contentsCategorySort;

    @Column(length = 45, nullable = false)
    private String contentsCategoryName;

    @Column(length = 7, nullable = false)
    private String contentsCategoryColor;

}
