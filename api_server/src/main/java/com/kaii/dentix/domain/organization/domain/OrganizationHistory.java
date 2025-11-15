package com.kaii.dentix.domain.organization.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "organization_history")
public class OrganizationHistory extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String fieldName;   // 변경된 필드명: name, phone, email

    @Column(nullable = false)
    private String beforeValue;

    @Column(nullable = false)
    private String afterValue;

    @Column(nullable = false)
    private Long modifiedByAdminId;  // 누가 수정했는지

    @Column(nullable = false)
    private LocalDateTime modifiedAt;
}
