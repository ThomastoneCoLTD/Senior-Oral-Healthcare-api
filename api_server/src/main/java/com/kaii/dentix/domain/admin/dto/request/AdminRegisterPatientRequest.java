//package com.kaii.dentix.domain.admin.dto.request;
//
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//import jakarta.validation.constraints.Size;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Getter @Builder
//@AllArgsConstructor @NoArgsConstructor
//public class AdminRegisterPatientRequest {
//
//    @NotBlank(message = "환자 이름은 필수입니다.")
//    @Size(min = 2, max = 100, message = "이름은 최소 2자 이상 입력해야 됩니다.")
//    @Pattern(regexp = "^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]+$", message = "이름은 한글이나 영문으로만 입력해 주세요.")
//    private String patientName;
//
//    @NotBlank(message = "환자 연락처는 필수입니다.")
//    @Size(min = 10, max = 11, message = "연락처는 최소 10자부터 최대 11자입니다.")
//    @Pattern(regexp = "^[0-9]+$", message = "연락처는 숫자만 입력해 주세요.")
//    private String patientPhoneNumber;
//
//}
