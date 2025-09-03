//package com.kaii.dentix.domain.patient.domain;
//
//import com.kaii.dentix.global.common.entity.TimeEntity;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.Where;
//
//import java.util.Date;
//
//@Entity
//@Getter @Builder
//@AllArgsConstructor @NoArgsConstructor
//@Table(name = "patient")
//@Where(clause = "deleted IS NULL")
//public class Patient extends TimeEntity {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long patientId;
//
//    @Column(length = 45, nullable = false)
//    private String patientName;
//
//    @Column(length = 45, nullable = false)
//    private String patientPhoneNumber;
//
//    @Temporal(TemporalType.TIMESTAMP)
//    public Date deleted;
//
//    /**
//     *  환자 삭제
//     */
//    public void revoke(){
//        this.deleted = new Date();
//    }
//
//}
