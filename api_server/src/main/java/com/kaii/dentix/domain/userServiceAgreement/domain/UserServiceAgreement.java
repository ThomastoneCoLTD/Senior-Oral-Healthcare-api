package com.kaii.dentix.domain.userServiceAgreement.domain;

import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(name = "userServiceAgreement")
public class UserServiceAgreement extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userServiceAgreeId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long serviceAgreeId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum", nullable = false)
    private YnType isUserServiceAgree;

    @Temporal(TemporalType.TIMESTAMP)
    private Date userServiceAgreeDate;

    /**
     *  사용자 서비스 이용동의 여부 수정
     */
    public void modifyServiceAgree(YnType isUserServiceAgree){
        if (!this.isUserServiceAgree.equals(isUserServiceAgree)){
            this.isUserServiceAgree = isUserServiceAgree;
            this.userServiceAgreeDate = new Date();
        }
    }

}
