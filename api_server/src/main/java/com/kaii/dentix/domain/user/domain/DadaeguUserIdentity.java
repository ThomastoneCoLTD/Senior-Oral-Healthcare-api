package com.kaii.dentix.domain.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "dadaegu_user_identity")
public class DadaeguUserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dadaeguUserIdentityId;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, unique = true, length = 255)
    private String externalDid;

    @Column(unique = true, length = 64)
    private String ciHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateCiHash(String ciHash) {
        if (ciHash != null && !ciHash.isBlank()
                && (this.ciHash == null || this.ciHash.isBlank())) {
            this.ciHash = ciHash;
        }
    }

    public void updateExternalDid(String externalDid) {
        if (externalDid != null && !externalDid.isBlank()) {
            this.externalDid = externalDid;
        }
    }
}
