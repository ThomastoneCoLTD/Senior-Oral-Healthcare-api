//package com.kaii.dentix.domain.admin.controller;
//
//import com.kaii.dentix.domain.admin.application.AdminPatientService;
//import com.kaii.dentix.domain.admin.dto.AdminPatientListDto;
//import com.kaii.dentix.domain.admin.dto.AdminRegisterPatientDto;
//import com.kaii.dentix.domain.admin.dto.request.AdminPatientListRequest;
//import com.kaii.dentix.domain.admin.dto.request.AdminRegisterPatientRequest;
//import com.kaii.dentix.global.common.response.DataResponse;
//import com.kaii.dentix.global.common.response.SuccessResponse;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/admin/patient")
//@CrossOrigin(origins = "*", allowedHeaders = "*")
//public class AdminPatientController {
//
//    private final AdminPatientService adminPatientService;
//
//    /**
//     *  관리자 환자 등록
//     */
//    @PostMapping(name = "환자 등록")
//    public DataResponse<AdminRegisterPatientDto> adminRegisterPatient(@RequestBody @Valid AdminRegisterPatientRequest request){
//        DataResponse<AdminRegisterPatientDto> response = new DataResponse<>(adminPatientService.adminRegisterPatient(request));
//        return response;
//    }
//
//    /**
//     *  관리자 환자 목록 조회
//     */
//    @GetMapping(name = "환자 목록 조회")
//    public DataResponse<AdminPatientListDto> adminPatientList(AdminPatientListRequest request){
//        DataResponse<AdminPatientListDto> response = new DataResponse<>(adminPatientService.adminPatientList(request));
//        return response;
//    }
//
//    /**
//     *  관리자 환자 삭제
//     */
//    @DeleteMapping(name = "환자 삭제")
//    public SuccessResponse adminDeletePatient(@RequestParam Long patientId){
//        adminPatientService.adminDeletePatient(patientId);
//        return new SuccessResponse();
//    }
//
//}
