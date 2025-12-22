package com.kaii.dentix.domain.systemLog.domain;

import java.util.Date;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.CreationTimestamp;

import com.kaii.dentix.domain.type.UserRole;

@Entity
@Table(name = "system_log")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@DynamicInsert
public class SystemLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long systemLogId;

    private Long tokenUserId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum")
    private UserRole tokenUserRole;
    
    private String requestName;

    private String requestUrl;

    @Column(columnDefinition = "json")
    private String header;

    @Column(columnDefinition = "json")
    private String requestBody;

    @Column(columnDefinition = "json")
    private String responseBody;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = false)
    private Date created;

    @Override
    public String toString() {
        return "{"
            + "\"systemLogId\":" + systemLogId
            + ", \"tokenUserId\":" + tokenUserId
            + ", \"tokenUserRole\":\"" + tokenUserRole + "\""
            + ", \"requestName\":\"" + requestName + "\""
            + ", \"requestUrl\":\"" + requestUrl + "\""
            + ", \"header\":\"" + header + "\""
            + ", \"requestBody\":\"" + requestBody + "\""
            + ", \"responseBody\":\"" + responseBody + "\""
            + ", \"created\":\"" + created + "\""
            + "}";
    }
}
