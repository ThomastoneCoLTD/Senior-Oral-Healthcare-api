package com.kaii.dentix.domain.curation.domain;

import com.kaii.dentix.domain.contents.domain.Contents;
import com.kaii.dentix.domain.type.AnalysisType;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "content_curation_rule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_content_curation_rule",
                columnNames = {"analysis_type", "result_key", "contents_id"}
        ),
        indexes = @Index(name = "idx_curation_type_key", columnList = "analysis_type, result_key")
)
public class ContentCurationRule extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 32)
    private AnalysisType analysisType;

    @Column(name = "result_key", nullable = false, length = 32)
    private String resultKey;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contents_id", nullable = false)
    private Contents contents;

    @Column(name = "curation_rank")
    private Integer rank;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
