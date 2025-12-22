package com.kaii.dentix.domain.userServiceAgreement.domain;



import jakarta.persistence.*;
import java.util.Date;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.entity.TimeEntity;
@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(name = "user_service_agreement")
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
