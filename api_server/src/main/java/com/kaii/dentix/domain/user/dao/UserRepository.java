package com.kaii.dentix.domain.user.dao;

//import com.kaii.dentix.domain.patient.domain.Patient;
import com.kaii.dentix.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

//    Optional<User> findByPatientId(Long patientId);
    Optional<User> findByUserLoginIdentifier(String userLoginIdentifier);
    Optional<User> findByUserId(Long userId);

    List<User> findByUserPhoneNumberOrUserName(String userPhoneNumber, String userName);
    Optional<User> findByUserPhoneNumber(String userPhoneNumber);
    List<User> findByOrganization_OrganizationId(Long organizationId);

}