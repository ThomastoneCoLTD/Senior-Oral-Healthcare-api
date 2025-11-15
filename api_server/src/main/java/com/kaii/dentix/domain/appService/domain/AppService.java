package com.kaii.dentix.domain.appService.domain;
import com.kaii.dentix.domain.type.ServiceType;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_service")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppService extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appServiceId; // 서비스 ID

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 서비스 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "app_service_type", nullable = false, length = 50)
    private ServiceType serviceType;
}
