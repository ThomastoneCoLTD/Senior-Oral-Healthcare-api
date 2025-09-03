//package com.kaii.dentix.domain.patient.dao;
//
//import com.kaii.dentix.domain.patient.domain.Patient;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PatientRepository extends JpaRepository<Patient, Long> {
//    List<Patient> findByPatientPhoneNumberOrPatientName(String patientPhoneNumber, String patientName);
//
//    Optional<Patient> findByPatientPhoneNumber(String patientPhoneNumber);
//
//}