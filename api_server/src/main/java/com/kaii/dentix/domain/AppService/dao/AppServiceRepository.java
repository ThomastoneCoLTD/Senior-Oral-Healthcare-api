package com.kaii.dentix.domain.AppService.dao;

import com.kaii.dentix.domain.AppService.domain.AppService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppServiceRepository extends JpaRepository<AppService, Long> {

}