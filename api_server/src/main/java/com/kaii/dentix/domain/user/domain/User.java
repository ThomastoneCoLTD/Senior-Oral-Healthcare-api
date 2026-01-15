package com.kaii.dentix.domain.user.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.appService.domain.UserToAppService;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter @Builder
@Setter
@AllArgsConstructor @NoArgsConstructor
@Table(name = "user")
@Where(clause = "deleted IS NULL")
public class User extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(length = 45, nullable = false)
    private String userLoginIdentifier;

    @Column(nullable = false)
    private String userPassword;

    @Column(length = 100, nullable = false)
    private String userName;

    @Column(length = 45, nullable = false)
    private String userPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum")
    private GenderType userGender;

    @Column(nullable = false)
    private Long findPwdQuestionId;

    @Column(nullable = false)
    private String findPwdAnswer;

    private String userRefreshToken;

    @Column(nullable = true)
    private Integer successCount = 0;

    @Temporal(TemporalType.TIMESTAMP)
    public Date deleted;

    @Temporal(TemporalType.TIMESTAMP)
    public Date userLastLoginDate;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum", nullable = false)
    private YnType isVerify;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationId")
    private Organization organization;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserToAppService> userToAppServices = new ArrayList<>();

    /**
     * RefreshToken, 최근 로그인 일자 업데이트
     */
    public void updateLogin(String refreshToken) {
        this.userRefreshToken = refreshToken;
        this.userLastLoginDate = new Date();
    }

    /**
     *  비밀번호 수정
     */
    public void modifyUserPassword(PasswordEncoder passwordEncoder, String userPassword) {
        this.userPassword = passwordEncoder.encode(userPassword);
    }

    /**
     *  질문과 답변 수정
     */
    public void modifyQnA(Long findPwdQuestionId, String findPwdAnswer){
        this.findPwdQuestionId = findPwdQuestionId;
        this.findPwdAnswer = findPwdAnswer;
    }

    /**
     *  회원 정보 수정
     */
    public void modifyInfo(String userName, GenderType userGender){
        this.userName = userName;
        this.userGender = userGender; // 미선택으로 원복 가능
    }

    /**
     *  회원 정보 수정
     */
    public void adminModifyInfo(String userLoginIdentifier, String userName, GenderType userGender) {
        this.userLoginIdentifier = userLoginIdentifier;
        this.userName = userName;
        if (userGender != null) {
            this.userGender = userGender;
        }
    }
    public void resetSuccessCount() {
        this.successCount = 0;
    }

    /**
     *  로그아웃
     */
    public void logout(){
        this.userRefreshToken = null;
    }
    /**
     *  회원탈퇴
     */
    public void revoke(){
        this.deleted = new Date();
    }

}