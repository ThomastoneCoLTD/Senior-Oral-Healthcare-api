//package com.kaii.dentix.domain.admin.application;
//
//import com.kaii.dentix.domain.admin.dto.AdminPatientInfoDto;
//import com.kaii.dentix.domain.admin.dto.AdminPatientListDto;
//import com.kaii.dentix.domain.admin.dto.AdminRegisterPatientDto;
//import com.kaii.dentix.domain.admin.dto.request.AdminPatientListRequest;
//import com.kaii.dentix.domain.admin.dto.request.AdminRegisterPatientRequest;
//import com.kaii.dentix.domain.patient.dao.AdminPatientCustomRepository;
//import com.kaii.dentix.domain.patient.dao.PatientRepository;
//import com.kaii.dentix.domain.patient.domain.Patient;
//import com.kaii.dentix.domain.user.dao.UserRepository;
//import com.kaii.dentix.global.common.dto.PagingDTO;
//import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
//import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
//import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
//import lombok.RequiredArgsConstructor;
//import org.modelmapper.ModelMapper;
//import org.springframework.data.domain.Page;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//public class AdminPatientService {
//
//    private final PatientRepository patientRepository;
//
//    private final AdminPatientCustomRepository adminPatientCustomRepository;
//
//    private final ModelMapper modelMapper;
//
//    private final UserRepository userRepository;
//
//    /**
//     *  관리자 환자 등록
//     */
//    @Transactional
//    public AdminRegisterPatientDto adminRegisterPatient(AdminRegisterPatientRequest request){
//        boolean isExistPhoneNumber = patientRepository.findByPatientPhoneNumber(request.getPatientPhoneNumber()).isPresent();
//        if (isExistPhoneNumber) throw new AlreadyDataException("이미 등록된 연락처입니다.");
//
//        Patient patient = Patient.builder()
//                .patientName(request.getPatientName())
//                .patientPhoneNumber(request.getPatientPhoneNumber())
//                .build();
//
//        patientRepository.save(patient);
//
//        return AdminRegisterPatientDto.builder()
//                .patientId(patient.getPatientId())
//                .build();
//    }
//
//    /**
//     *  관리자 환자 목록 조회
//     */
//    @Transactional(readOnly = true)
//    public AdminPatientListDto adminPatientList(AdminPatientListRequest request){
//        Page<AdminPatientInfoDto> patientList = adminPatientCustomRepository.findAll(request);
//
//        PagingDTO pagingDTO = modelMapper.map(patientList, PagingDTO.class);
//
//        return AdminPatientListDto.builder()
//                .paging(pagingDTO)
//                .patientList(patientList.getContent())
//                .build();
//    }
//
//    /**
//     *  관리자 환자 삭제
//     */
//    @Transactional
//    public void adminDeletePatient(Long patientId){
//        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 환자입니다."));
//
//        // 회원 가입을 한 환자는 삭제 불가능
//        boolean isUser = userRepository.findByPatientId(patientId).isPresent();
//        if (isUser) throw new BadRequestApiException("가입된 환자는 삭제할 수 없습니다.");
//
//        patient.revoke();
//    }
//
//}
