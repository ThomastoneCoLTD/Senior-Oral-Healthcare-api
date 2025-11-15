package com.kaii.dentix.domain.contents.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *  콘텐츠 카테고리 연관관계 매핑
 */
@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(name = "contents_to_category")
public class ContentsToCategory extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contentsToCategoryId;

    @Column(nullable = false)
    private int contentsCategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contents_id") // ✅ FK 관계로 명시
    private Contents contents;

}
