//package com.kaii.dentix.domain.serviceAgreement.domain;
//
//import com.kaii.dentix.domain.type.YnType;
//import com.kaii.dentix.global.common.entity.TimeEntity;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Getter @Builder
//@AllArgsConstructor @NoArgsConstructor
//@Table(name = "service_agreement")
//public class ServiceAgreement extends TimeEntity {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long serviceAgreeId;
//
//    @Column(nullable = false)
//    private Long serviceAgreeSort;
//
//    @Column(length = 50, nullable = false)
//    private String serviceAgreeName;
//
//    @Column(length = 50, nullable = false)
//    private String serviceAgreeMenuName;
//
//    @Enumerated(EnumType.STRING)
//    @Column(columnDefinition = "enum", nullable = false)
//    private YnType isServiceAgreeRequired;
//
//    @Column(nullable = false)
//    private String serviceAgreePath;
//}
