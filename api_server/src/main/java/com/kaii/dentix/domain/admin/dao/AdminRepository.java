package com.kaii.dentix.domain.admin.dao;

import com.kaii.dentix.domain.admin.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByAdminLoginIdentifier(String adminIdentifier);

    Optional<Admin> findByAdminPhoneNumber(String adminPhoneNumber);

}
