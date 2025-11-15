package com.kaii.dentix.domain.oralStatusToContents.domain;

import com.kaii.dentix.domain.contents.domain.Contents;
import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oral_status_to_contents")
public class OralStatusToContents extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oralStatusToContentsId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "oralStatus_type", nullable = false)
    private OralStatus oralStatus;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contents_id", nullable = false)
    private Contents contents;
}
