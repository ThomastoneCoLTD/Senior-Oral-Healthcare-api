package com.kaii.dentix.domain.daeguChain.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daegu_chain_api_log",
        indexes = {
                @Index(name = "idx_daegu_chain_api_log_user_id", columnList = "userId"),
                @Index(name = "idx_daegu_chain_api_log_created", columnList = "created")
        }
)
public class DaeguChainApiLog extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long daeguChainApiLogId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String feature;

    @Column(nullable = false, length = 500)
    private String api;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String responsePayload;

    @Column(nullable = false)
    private boolean success;
}
