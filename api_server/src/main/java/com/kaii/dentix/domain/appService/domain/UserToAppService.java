package com.kaii.dentix.domain.appService.domain;

import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_to_app_service")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserToAppService extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_service_id", nullable = false)
    private AppService appService;
}