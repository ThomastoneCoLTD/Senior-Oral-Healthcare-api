package com.kaii.dentix.domain.AppService.domain;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppService extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long serviceId; // 서비스 ID

    @Column(nullable = false, unique = true, length = 100)
    private String name; // 서비스 이름
}
