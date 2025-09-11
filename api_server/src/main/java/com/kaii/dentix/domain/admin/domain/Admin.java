package com.kaii.dentix.domain.admin.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;

@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(name = "admin")
@Where(clause = "deleted IS NULL")
public class Admin extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminId;

    @Column(length = 45, nullable = false)
    private String adminName;

    @Column(length = 45, nullable = false)
    private String adminLoginIdentifier;

    @Column(length = 11, nullable = false)
    private String adminPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum", nullable = false)
    private YnType adminIsSuper;

    @Temporal(TemporalType.TIMESTAMP)
    private Date adminLastLoginDate;

    private String adminPassword;

    private String adminRefreshToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationId", nullable = false)
    private Organization organization;

//    private Integer idlePeriod;

//    private String planId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deleted;

    /**
     *  관리자 로그인
     */
    public void updateAdminLogin(String refreshToken) {
        this.adminRefreshToken = refreshToken;
        this.adminLastLoginDate = new Date();
    }

    /**
     *  관리자 비밀번호 변경
     */
    public void updatePassword(PasswordEncoder passwordEncoder, String adminPassword) {
        this.adminPassword = passwordEncoder.encode(adminPassword);
    }

    /**
     *  관리자 삭제
     */
    public void deleteAdmin(){
        this.deleted = new Date();
    }

}
