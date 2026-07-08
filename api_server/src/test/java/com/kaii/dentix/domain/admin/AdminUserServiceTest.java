package com.kaii.dentix.domain.admin;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserRepositoryImpl;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminUserDto;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.agreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.agreement.domain.ServiceAgreement;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.findPwdQuestion.domain.FindPwdQuestion;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private ModelMapper modelMapper;
    @Mock private AdminService adminService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserLoginService userLoginService;
    @Mock private OralCheckRepository oralCheckRepository;
    @Mock private AdminUserRepositoryImpl adminUserRepository;
    @Mock private AdminUserCustomRepository adminUserCustomRepository;
    @Mock private OrganizationSubscriptionService organizationSubscriptionService;
    @Mock private FindPwdQuestionRepository findPwdQuestionRepository;
    @Mock private ServiceAgreementRepository serviceAgreementRepository;
    @Mock private ServiceAgreementConsentService serviceAgreementConsentService;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                modelMapper,
                adminService,
                userRepository,
                passwordEncoder,
                userLoginService,
                oralCheckRepository,
                adminUserRepository,
                adminUserCustomRepository,
                organizationSubscriptionService,
                findPwdQuestionRepository,
                serviceAgreementRepository,
                serviceAgreementConsentService,
                new NoOpTransactionManager()
        );
    }

    @Test
    void processExcelUpload_returnsSuccessAndFailCounts() throws Exception {
        Organization organization = Organization.builder().organizationId(2L).build();
        Admin admin = Admin.builder().adminId(1L).organization(organization).build();
        FindPwdQuestion question = FindPwdQuestion.builder().findPwdQuestionId(1L).findPwdQuestionSort(1L).findPwdQuestionTitle("질문").build();
        ServiceAgreement requiredAgreement = ServiceAgreement.builder()
                .serviceAgreeId(1L)
                .serviceAgreeSort(1L)
                .serviceAgreeName("필수 약관")
                .serviceAgreeMenuName("필수 약관")
                .isServiceAgreeRequired(YnType.Y)
                .serviceAgreePath("/required")
                .build();

        given(findPwdQuestionRepository.findAll(any(org.springframework.data.domain.Sort.class))).willReturn(List.of(question));
        given(serviceAgreementRepository.findAll(any(org.springframework.data.domain.Sort.class))).willReturn(List.of(requiredAgreement));
        given(passwordEncoder.encode("test1234!")).willReturn("encoded-password");
        given(userRepository.findByUserLoginIdentifier("valid01")).willReturn(Optional.empty());
        given(userRepository.findByUserLoginIdentifier("valid02")).willReturn(Optional.of(User.builder().userId(99L).build()));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(10L);
            return user;
        });
        doNothing().when(serviceAgreementConsentService).saveUserServiceAgreements(any(Long.class), ArgumentMatchers.anyList());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                createWorkbookBytes()
        );

        AdminUserDto.BulkUploadResponse response = adminUserService.processExcelUpload(file, admin);

        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getCreatedCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailList()).hasSize(1);
        assertThat(response.getFailList().get(0).getRow()).isEqualTo(3);
        assertThat(response.getFailList().get(0).getReason()).isEqualTo("아이디 중복");

        verify(userRepository).save(any(User.class));
        verify(serviceAgreementConsentService).saveUserServiceAgreements(any(Long.class), ArgumentMatchers.anyList());
    }

    private byte[] createWorkbookBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Template");
            var header = sheet.createRow(0);
            String[] headers = {
                    "userLoginIdentifier", "userPassword", "userName", "userGender",
                    "userPhoneNumber", "findPwdQuestionId", "findPwdAnswer",
                    "userServiceAgreementIds"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            var validRow = sheet.createRow(1);
            validRow.createCell(0).setCellValue("valid01");
            validRow.createCell(1).setCellValue("test1234!");
            validRow.createCell(2).setCellValue("홍길동");
            validRow.createCell(3).setCellValue("M");
            validRow.createCell(4).setCellValue("01012345678");
            validRow.createCell(5).setCellValue("1");
            validRow.createCell(6).setCellValue("답변");
            validRow.createCell(7).setCellValue("1");

            var failRow = sheet.createRow(2);
            failRow.createCell(0).setCellValue("valid02");
            failRow.createCell(1).setCellValue("test1234!");
            failRow.createCell(2).setCellValue("김덴티");
            failRow.createCell(3).setCellValue("W");
            failRow.createCell(4).setCellValue("01099998888");
            failRow.createCell(5).setCellValue("1");
            failRow.createCell(6).setCellValue("답변");
            failRow.createCell(7).setCellValue("1");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static class NoOpTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
        }
    }
}
