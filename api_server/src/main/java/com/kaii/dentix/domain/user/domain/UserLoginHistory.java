package com.kaii.dentix.domain.user.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_login_history",
        indexes = {
                @Index(name = "idx_user_login_history_user_id", columnList = "userId")
        }
)
public class UserLoginHistory extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userLoginHistoryId;

    @Column(nullable = false)
    private Long userId;
}
