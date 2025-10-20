package com.kaii.dentix.domain.appService.dao;

import com.kaii.dentix.domain.appService.domain.AppService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppServiceRepository extends JpaRepository<AppService, Long> {

}