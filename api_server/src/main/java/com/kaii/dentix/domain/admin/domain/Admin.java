package com.kaii.dentix.domain.admin.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
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
    @Column(columnDefinition = "ENUM('Y','N') DEFAULT 'N'", nullable = false)
    private YnType adminIsSuper;

    // NULL denotes an account created before the approval workflow was introduced.
    // Public registration always explicitly stores PENDING.
    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "varchar(20)")
    private AdminApprovalStatus approvalStatus;

    private LocalDateTime approvedAt;
    private Long approvedByAdminId;

    public AdminApprovalStatus effectiveApprovalStatus() {
        return approvalStatus == null ? AdminApprovalStatus.APPROVED : approvalStatus;
    }

    public boolean isApproved() {
        return effectiveApprovalStatus() == AdminApprovalStatus.APPROVED;
    }

    public void approve(Long approvingAdminId) {
        if (isApproved()) return;
        approvalStatus = AdminApprovalStatus.APPROVED;
        approvedAt = LocalDateTime.now();
        approvedByAdminId = approvingAdminId;
        adminRefreshToken = null;
    }

    @Temporal(TemporalType.TIMESTAMP)
    private Date adminLastLoginDate;

    @Column(nullable = false)
    private String adminPassword;

    @Column(nullable = false)
    private Long findPwdQuestionId;

    @Column(length = 200)
    private String findPwdAnswer;

    @Column(length = 2048)
    private String adminRefreshToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = true)
    private Organization organization;

    public boolean isSuperAdmin() {
        return this.adminIsSuper == YnType.Y;
    }

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
        this.adminRefreshToken = null;
    }

    /**
     *  관리자 삭제
     */
    public void deleteAdmin(){
        this.deleted = new Date();
    }

    public boolean isFirstLogin() {
        return this.adminLastLoginDate == null;
    }
}
