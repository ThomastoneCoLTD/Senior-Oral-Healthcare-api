package com.kaii.dentix.domain.userServiceAgreement.domain;

import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import java.util.Date;

@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(name = "userServiceAgreement")
public class UserServiceAgreement extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_service_agree_id")
    private Long userServiceAgreeId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "service_agree_id")
    private Long serviceAgreeId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum", nullable = false, name = "is_user_service_agree")
    private YnType isUserServiceAgree;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "user_service_agree_date")
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
