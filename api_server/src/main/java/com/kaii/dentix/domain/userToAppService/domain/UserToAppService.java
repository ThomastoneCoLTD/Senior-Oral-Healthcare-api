package com.kaii.dentix.domain.userToAppService.domain;

import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.user.domain.User;
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
public class UserToAppService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appServiceId", nullable = false) // ✅ 변경됨
    private AppService appService;
}
