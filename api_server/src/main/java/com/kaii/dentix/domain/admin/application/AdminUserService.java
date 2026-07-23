package com.kaii.dentix.domain.admin.application;

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
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.dao.UserOralExerciseProgressRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.oralExercise.domain.UserOralExerciseProgress;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dao.UserLoginHistoryRepository;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.domain.user.domain.UserLoginHistory;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String[] TEMPLATE_HEADERS = {
            "userLoginIdentifier", "userPassword", "userName", "userGender",
            "userPhoneNumber", "findPwdQuestionId", "findPwdAnswer",
            "userServiceAgreementIds"
    };
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-zA-Z])(?=.*[!@#$%^&*])[a-zA-Z!@#$%^&*0-9]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,11}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]+$");

    private final ModelMapper modelMapper;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLoginService userLoginService;
    private final OralCheckRepository oralcheckRepository;
    private final AdminUserRepositoryImpl adminUserRepository;
    private final AdminUserCustomRepository adminUserCustomRepository;
    private final OrganizationSubscriptionService organizationSubscriptionService;
    private final FindPwdQuestionRepository findPwdQuestionRepository;
    private final ServiceAgreementRepository serviceAgreementRepository;
    private final ServiceAgreementConsentService serviceAgreementConsentService;
    private final PlatformTransactionManager transactionManager;
    private final OralExerciseContentRepository oralExerciseContentRepository;
    private final UserOralExerciseProgressRepository oralExerciseProgressRepository;
    private final UserRewardTransactionRepository userRewardTransactionRepository;
    private final UserRewardWalletRepository userRewardWalletRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;

    /**
     * 일반관리자 - 본인 기관의 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public AdminUserDto.ListResponse userList(AdminUserDto.SearchRequest request, HttpServletRequest servletRequest) {
        Admin admin = adminService.getTokenAdmin(servletRequest);

        // 기본값 설정
        if (request.getPage() == null) request.setPage(1);
        if (request.getSize() == null) request.setSize(50);

        Page<AdminUserDto.Info> pageResult;

        // 슈퍼/일반 관리자 분기 처리
        if (admin.isSuperAdmin()) {
            pageResult = adminUserCustomRepository.findAll(request);
        } else {
            if (admin.getOrganization() == null) {
                throw new BadRequestApiException("소속 기관이 없습니다.");
            }
            request.setOrganizationId(admin.getOrganization().getOrganizationId());
            pageResult = adminUserCustomRepository.findAll(request);
        }

        PagingDTO pagingDTO = modelMapper.map(pageResult, PagingDTO.class);
        return AdminUserDto.ListResponse.of(pagingDTO, pageResult.getContent());
    }

    /**
     * 기관별 사용자 목록 조회 (슈퍼관리자용)
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDto.Info> getUsersByOrganization(AdminUserDto.SearchRequest request) {
        return adminUserRepository.findAllByOrganization(request);
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 정보 조회 (수정용)
     */
    @Transactional(readOnly = true)
    public AdminUserDto.DetailResponse userInfo(Long userId) {
        User user = getUser(userId);

        return AdminUserDto.DetailResponse.from(
                user.getUserLoginIdentifier(),
                user.getUserName(),
                user.getUserGender()
        );
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 정보 수정
     */
    @Transactional
    public void userModify(AdminUserDto.ModifyRequest request) {
        User user = getUser(request.getUserId());

        // 아이디 변경 시 중복 체크
        if (!user.getUserLoginIdentifier().equals(request.getLoginId())) {
            userLoginService.loginIdCheck(request.getLoginId());
        }

        user.adminModifyInfo(request.getLoginId(), request.getName(), request.getGender());
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 인증
     */
    @Transactional
    public void userVerify(Long userId) {
        User user = getUser(userId);
        if (user.getIsVerify() == YnType.Y) throw new BadRequestApiException("이미 인증된 사용자입니다.");
        user.setIsVerify(YnType.Y);
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 인증 취소
     */
    @Transactional
    public void userUnverify(Long userId) {
        User user = getUser(userId);
        if (user.getIsVerify() == YnType.N) throw new BadRequestApiException("이미 인증 취소된 사용자입니다.");
        user.setIsVerify(YnType.N);
    }

    /**
     * 일반관리자 - 본인 기관의 사용자 삭제
     */
    @Transactional
    public void userDelete(Long userId) {
        User user = getUser(userId);
        user.revoke(); // 회원 탈퇴(삭제) 처리
    }

    /**
     * 기관 사용자 사용량 조회
     */
    public DataResponse<Map<String, Object>> getOrganizationUserUsage(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        Organization org = admin.getOrganization();

        if (org == null) throw new BadRequestApiException("기관 정보가 없습니다.");

        OrganizationSubscription activeSubscription = organizationSubscriptionService.getActiveSubscription(org);

        // 날짜 변환 로직
        Date startDate = toDate(activeSubscription.getSubscriptionStartDate());
        Date endDate = toDate(activeSubscription.getUsageResetDate() != null
                ? activeSubscription.getUsageResetDate()
                : activeSubscription.getSubscriptionEndDate());

        List<OralCheckDto.Usage> usageList = oralcheckRepository.findUserUsageByOrganizationAndPeriod(
                org.getOrganizationId(), startDate, endDate
        );

        long total = usageList.stream().mapToLong(OralCheckDto.Usage::getSuccessCount).sum();
        return new DataResponse<>(200, "기관 사용자 사용량 조회 성공", Map.of("totalCount", total, "users", usageList));
    }

    @Transactional(readOnly = true)
    public AdminUserDto.ExerciseProgressResponse getExerciseProgress(HttpServletRequest request) {
        List<User> users = getVisibleUsers(request);

        List<OralExerciseContent> contents = oralExerciseContentRepository.findByActiveTrueOrderByContentSortAsc();
        List<Long> userIds = users.stream().map(User::getUserId).toList();
        List<UserOralExerciseProgress> progressList = userIds.isEmpty()
                ? List.of()
                : oralExerciseProgressRepository.findByUserIdIn(userIds);
        List<UserRewardTransaction> rewards = userIds.isEmpty()
                ? List.of()
                : userRewardTransactionRepository.findByUserIdInAndType(
                        userIds,
                        UserRewardTransactionType.ORAL_EXERCISE_COIN
                );

        Map<Long, Map<Long, UserOralExerciseProgress>> progressByUser = progressList.stream()
                .collect(Collectors.groupingBy(
                        UserOralExerciseProgress::getUserId,
                        Collectors.toMap(
                                item -> item.getContent().getOralExerciseContentId(),
                                Function.identity(),
                                (left, right) -> left
                        )
                ));
        Map<Long, Map<Long, UserRewardTransaction>> rewardsByUser = rewards.stream()
                .filter(UserRewardTransaction::isRewardReceived)
                .filter(item -> item.getOralExerciseContent() != null)
                .collect(Collectors.groupingBy(
                        UserRewardTransaction::getUserId,
                        Collectors.toMap(
                                item -> item.getOralExerciseContent().getOralExerciseContentId(),
                                Function.identity(),
                                (left, right) -> left.getCreated().after(right.getCreated()) ? left : right
                        )
                ));

        List<AdminUserDto.ExerciseProgressUser> rows = users.stream().map(user -> {
            Map<Long, UserOralExerciseProgress> userProgress = progressByUser.getOrDefault(user.getUserId(), Map.of());
            Map<Long, UserRewardTransaction> userRewards = rewardsByUser.getOrDefault(user.getUserId(), Map.of());
            List<AdminUserDto.ExerciseProgressContent> contentRows = contents.stream().map(content -> {
                UserOralExerciseProgress progress = userProgress.get(content.getOralExerciseContentId());
                UserRewardTransaction reward = userRewards.get(content.getOralExerciseContentId());
                boolean rewardEligible = content.getContentSort() >= 2 && content.getContentSort() <= 6;
                return AdminUserDto.ExerciseProgressContent.builder()
                        .contentId(content.getOralExerciseContentId())
                        .contentSort(content.getContentSort())
                        .title(content.getTitle())
                        .completionRate(progress == null ? 0 : progress.getCompletionRate())
                        .completed(progress != null && progress.isCompleted())
                        .lastPositionSeconds(progress == null ? 0 : progress.getLastPositionSeconds())
                        .durationSeconds(content.getDurationSeconds())
                        .lastViewedAt(progress == null ? null : progress.getLastViewedAt())
                        .rewardEligible(rewardEligible)
                        .rewardReceived(rewardEligible && reward != null)
                        .rewardReceivedAt(reward == null ? null : reward.getCreated())
                        .build();
            }).toList();
            int completedCount = (int) contentRows.stream().filter(AdminUserDto.ExerciseProgressContent::isCompleted).count();
            int rewardCount = (int) contentRows.stream().filter(AdminUserDto.ExerciseProgressContent::isRewardReceived).count();
            int overallRate = contentRows.isEmpty() ? 0 : (int) Math.round(
                    contentRows.stream().mapToInt(AdminUserDto.ExerciseProgressContent::getCompletionRate).average().orElse(0)
            );
            return AdminUserDto.ExerciseProgressUser.builder()
                    .userId(user.getUserId())
                    .userLoginIdentifier(user.getUserLoginIdentifier())
                    .userName(user.getUserName())
                    .completedCount(completedCount)
                    .overallCompletionRate(overallRate)
                    .rewardReceivedCount(rewardCount)
                    .contents(contentRows)
                    .build();
        }).toList();

        return AdminUserDto.ExerciseProgressResponse.builder()
                .contentCount(contents.size())
                .rewardContentCount((int) contents.stream().filter(item -> item.getContentSort() >= 2 && item.getContentSort() <= 6).count())
                .users(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserDto.DaeguRewardStatusResponse getDaeguRewardStatus(HttpServletRequest request) {
        List<User> users = getVisibleUsers(request);
        List<Long> userIds = users.stream().map(User::getUserId).toList();
        List<OralExerciseContent> essentialContents = oralExerciseContentRepository.findByActiveTrueOrderByContentSortAsc()
                .stream()
                .filter(content -> content.getContentSort() >= 2 && content.getContentSort() <= 6)
                .toList();

        Map<Long, UserRewardWallet> walletsByUserId = userIds.isEmpty()
                ? Map.of()
                : userRewardWalletRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(UserRewardWallet::getUserId, Function.identity(), (left, right) -> left));
        Map<Long, List<UserLoginHistory>> loginHistoriesByUserId = userIds.isEmpty()
                ? Map.of()
                : userLoginHistoryRepository.findByUserIdInOrderByCreatedDesc(userIds).stream()
                        .collect(Collectors.groupingBy(UserLoginHistory::getUserId));
        List<UserRewardTransaction> rewardTransactions = userIds.isEmpty()
                ? List.of()
                : userRewardTransactionRepository.findByUserIdInAndTypeOrderByCreatedDesc(
                        userIds,
                        UserRewardTransactionType.ORAL_EXERCISE_COIN
                );
        List<UserRewardTransaction> reclaimTransactions = userIds.isEmpty()
                ? List.of()
                : userRewardTransactionRepository.findByUserIdInAndTypeOrderByCreatedDesc(
                        userIds,
                        UserRewardTransactionType.ORAL_EXERCISE_RECLAIM
                );

        Map<Long, Map<Long, UserRewardTransaction>> rewardsByUserAndContent = rewardTransactions.stream()
                .filter(UserRewardTransaction::isRewardReceived)
                .filter(transaction -> transaction.getOralExerciseContent() != null)
                .collect(Collectors.groupingBy(
                        UserRewardTransaction::getUserId,
                        Collectors.toMap(
                                transaction -> transaction.getOralExerciseContent().getOralExerciseContentId(),
                                Function.identity(),
                                (left, right) -> left.getCreated().after(right.getCreated()) ? left : right
                        )
                ));
        Map<Long, List<UserRewardTransaction>> reclaimsByUserId = reclaimTransactions.stream()
                .filter(UserRewardTransaction::isRewardReceived)
                .collect(Collectors.groupingBy(UserRewardTransaction::getUserId));

        List<AdminUserDto.DaeguRewardStatusUser> rows = users.stream().map(user -> {
            UserRewardWallet wallet = walletsByUserId.get(user.getUserId());
            List<UserLoginHistory> loginHistories = loginHistoriesByUserId.getOrDefault(user.getUserId(), List.of());
            Map<Long, UserRewardTransaction> userRewards = rewardsByUserAndContent.getOrDefault(user.getUserId(), Map.of());
            List<UserRewardTransaction> userReclaims = reclaimsByUserId.getOrDefault(user.getUserId(), List.of());
            List<AdminUserDto.EssentialReward> essentialRewards = essentialContents.stream()
                    .map(content -> toEssentialReward(content, userRewards.get(content.getOralExerciseContentId())))
                    .toList();
            int essentialRewardReceivedCount = (int) essentialRewards.stream()
                    .filter(AdminUserDto.EssentialReward::isRewardReceived)
                    .count();
            long reclaimedAmount = userReclaims.stream().mapToLong(UserRewardTransaction::getAmount).sum();
            boolean didIssued = user.getDaeguDidStatus() == UserDaeguIdentityStatus.ISSUED && StringUtils.hasText(user.getDaeguDid());
            boolean walletCreated = wallet != null && StringUtils.hasText(wallet.getWalletAddress());

            return AdminUserDto.DaeguRewardStatusUser.builder()
                    .userId(user.getUserId())
                    .userLoginIdentifier(user.getUserLoginIdentifier())
                    .userName(user.getUserName())
                    .organizationName(user.getOrganization() == null ? null : user.getOrganization().getOrganizationName())
                    .created(user.getCreated())
                    .userLastLoginDate(user.getUserLastLoginDate())
                    .daeguDid(user.getDaeguDid())
                    .daeguDidStatus(user.getDaeguDidStatus())
                    .didIssued(didIssued)
                    .walletDaeguDid(wallet == null ? null : wallet.getDaeguDid())
                    .walletAddress(wallet == null ? null : wallet.getWalletAddress())
                    .pointBalance(wallet == null ? 0 : wallet.getPointBalance())
                    .walletCreated(walletCreated)
                    .loginHistoryCount(loginHistories.size())
                    .loginHistories(loginHistories.stream()
                            .limit(20)
                            .map(this::toLoginHistory)
                            .toList())
                    .essentialRewardReceivedCount(essentialRewardReceivedCount)
                    .essentialRewardCompleted(essentialRewardReceivedCount >= essentialContents.size() && !essentialContents.isEmpty())
                    .essentialRewards(essentialRewards)
                    .reclaimCount(userReclaims.size())
                    .reclaimedAmount(reclaimedAmount)
                    .rewardReclaimed(!userReclaims.isEmpty())
                    .reclaims(userReclaims.stream()
                            .limit(20)
                            .map(this::toRewardTransactionSummary)
                            .toList())
                    .build();
        }).toList();

        return AdminUserDto.DaeguRewardStatusResponse.builder()
                .essentialRewardContentCount(essentialContents.size())
                .users(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserDto.DaeguChainUsageLogResponse getDaeguChainUsageLogs(HttpServletRequest request) {
        List<User> users = getVisibleUsers(request);
        List<Long> userIds = users.stream().map(User::getUserId).toList();
        Map<Long, User> usersById = users.stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<AdminUserDto.DaeguChainUsageLog> logs = new ArrayList<>();

        if (!userIds.isEmpty()) {
            userLoginHistoryRepository.findByUserIdInOrderByCreatedDesc(userIds).stream()
                    .map(history -> toDaeguChainUsageLog("DID 로그인", usersById.get(history.getUserId()), history.getCreated()))
                    .filter(Objects::nonNull)
                    .forEach(logs::add);

            userRewardTransactionRepository.findByUserIdInOrderByCreatedDesc(userIds).stream()
                    .map(transaction -> toDaeguChainUsageLog(
                            transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_RECLAIM
                                    ? "구강체조 리워드 회수"
                                    : "구강체조 리워드 지급",
                            usersById.get(transaction.getUserId()),
                            transaction.getCreated()
                    ))
                    .filter(Objects::nonNull)
                    .forEach(logs::add);
        }

        users.stream()
                .filter(user -> user.getDaeguDidStatus() == UserDaeguIdentityStatus.ISSUED)
                .map(user -> toDaeguChainUsageLog("DID 계정 발급", user, user.getCreated()))
                .filter(Objects::nonNull)
                .forEach(logs::add);

        logs.sort((left, right) -> right.getUsedAt().compareTo(left.getUsedAt()));
        return AdminUserDto.DaeguChainUsageLogResponse.builder()
                .logs(logs)
                .build();
    }

    private AdminUserDto.DaeguChainUsageLog toDaeguChainUsageLog(String feature, User user, Date usedAt) {
        if (user == null || usedAt == null) {
            return null;
        }
        return AdminUserDto.DaeguChainUsageLog.builder()
                .feature(feature)
                .userLoginIdentifier(user.getUserLoginIdentifier())
                .usedAt(usedAt)
                .build();
    }

    private AdminUserDto.EssentialReward toEssentialReward(
            OralExerciseContent content,
            UserRewardTransaction reward
    ) {
        return AdminUserDto.EssentialReward.builder()
                .contentId(content.getOralExerciseContentId())
                .contentSort(content.getContentSort())
                .title(content.getTitle())
                .rewardReceived(reward != null)
                .rewardReceivedAt(reward == null ? null : reward.getCreated())
                .transactionId(reward == null ? null : reward.getUserRewardTransactionId())
                .tokenName(reward == null ? null : reward.getCoinId())
                .status(reward == null ? null : reward.getStatus())
                .txHash(reward == null ? null : reward.getDaeguChainTxHash())
                .factHash(reward == null ? null : reward.getDaeguChainFactHash())
                .build();
    }

    private AdminUserDto.LoginHistory toLoginHistory(UserLoginHistory history) {
        return AdminUserDto.LoginHistory.builder()
                .historyId(history.getUserLoginHistoryId())
                .loggedInAt(history.getCreated())
                .build();
    }

    private AdminUserDto.RewardTransactionSummary toRewardTransactionSummary(UserRewardTransaction transaction) {
        return AdminUserDto.RewardTransactionSummary.builder()
                .transactionId(transaction.getUserRewardTransactionId())
                .tokenName(transaction.getCoinId())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .txHash(transaction.getDaeguChainTxHash())
                .factHash(transaction.getDaeguChainFactHash())
                .created(transaction.getCreated())
                .build();
    }

    private List<User> getVisibleUsers(HttpServletRequest request) {
        Admin admin = adminService.getTokenAdmin(request);
        List<User> users;
        if (admin.isSuperAdmin()) {
            users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "userName"));
        } else {
            if (admin.getOrganization() == null) {
                throw new BadRequestApiException("소속 기관 정보가 없습니다.");
            }
            users = userRepository.findByOrganization_OrganizationId(admin.getOrganization().getOrganizationId());
            users.sort(java.util.Comparator.comparing(
                    user -> user.getUserName() == null ? "" : user.getUserName(),
                    String.CASE_INSENSITIVE_ORDER
            ));
        }
        return users;
    }

    /**
     * 일반관리자 - 기관 사용자 일괄등록 엑셀 양식 다운로드
     */
    @Transactional(readOnly = true)
    public byte[] createBulkUploadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            createTemplateSheet(workbook);
            createGuideSheet(workbook);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BadRequestApiException("엑셀 양식을 생성하지 못했어요.");
        }
    }

    /**
     * 일반관리자 - 사용자 일괄등록 업로드
     */
    public AdminUserDto.BulkUploadResponse processExcelUpload(MultipartFile file, Admin admin) {
        Organization org = admin.getOrganization();
        if (org == null) throw new BadRequestApiException("관리자에 연결된 기관이 없습니다.");
        if (file == null || file.isEmpty()) throw new BadRequestApiException("업로드할 엑셀 파일을 선택해 주세요.");

        BulkUploadReference reference = getBulkUploadReference();
        Set<String> fileLoginIdSet = new HashSet<>();
        List<AdminUserDto.FailInfo> failList = new ArrayList<>();
        int successCount = 0;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0 || isEmptyRow(row, formatter)) {
                    continue;
                }

                int excelRowNumber = row.getRowNum() + 1;
                String failureReason = transactionTemplate.execute(status -> {
                    try {
                        registerUserFromRow(row, org, formatter, reference, fileLoginIdSet);
                        return null;
                    } catch (RuntimeException e) {
                        status.setRollbackOnly();
                        return getFailReason(e);
                    }
                });

                if (failureReason == null) {
                    successCount++;
                } else {
                    failList.add(AdminUserDto.FailInfo.builder()
                            .row(excelRowNumber)
                            .reason(failureReason)
                            .build());
                }
            }
        } catch (BadRequestApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestApiException("엑셀 파일을 읽지 못했어요. .xlsx 파일인지 확인해 주세요.");
        }

        return AdminUserDto.BulkUploadResponse.of(successCount, failList);
    }

    // --- Private Helper Methods ---

    // 사용자 조회 헬퍼
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));
    }

    // LocalDateTime -> Date 변환 헬퍼
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void createTemplateSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Template");
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(TEMPLATE_HEADERS[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6500);
        }

        Row example = sheet.createRow(1);
        example.createCell(0).setCellValue("user01");
        example.createCell(1).setCellValue("test1234!");
        example.createCell(2).setCellValue("홍길동");
        example.createCell(3).setCellValue("M");
        example.createCell(4).setCellValue("01012345678");
        example.createCell(5).setCellValue("1");
        example.createCell(6).setCellValue("우리집");
        example.createCell(7).setCellValue("1,2,3");
    }

    private void createGuideSheet(Workbook workbook) {
        Sheet guideSheet = workbook.createSheet("Guide");
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row titleRow = guideSheet.createRow(0);
        titleRow.createCell(0).setCellValue("column");
        titleRow.createCell(1).setCellValue("description");
        titleRow.createCell(2).setCellValue("example");
        for (int i = 0; i < 3; i++) {
            titleRow.getCell(i).setCellStyle(headerStyle);
            guideSheet.setColumnWidth(i, 9000);
        }

        Object[][] guides = new Object[][]{
                {"userLoginIdentifier", "4~12자의 영문/숫자 아이디", "user01"},
                {"userPassword", "8~20자, 영문과 특수문자 포함", "test1234!"},
                {"userName", "이름", "홍길동"},
                {"userGender", "M 또는 W(F 입력 시 W로 처리)", "M"},
                {"userPhoneNumber", "숫자만 10~11자리", "01012345678"},
                {"findPwdQuestionId", "비밀번호 찾기 질문 ID", "1"},
                {"findPwdAnswer", "비밀번호 찾기 답변", "우리집"},
                {"userServiceAgreementIds", "약관 ID를 쉼표로 구분, 비우면 필수 약관만 동의 처리", "1,2,3"}
        };

        int rowIndex = 1;
        for (Object[] guide : guides) {
            Row row = guideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(Objects.toString(guide[0], ""));
            row.createCell(1).setCellValue(Objects.toString(guide[1], ""));
            row.createCell(2).setCellValue(Objects.toString(guide[2], ""));
        }

        rowIndex = writeQuestionGuideRows(guideSheet, rowIndex, headerStyle);
        writeAgreementGuideRows(guideSheet, rowIndex, headerStyle);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    private int writeQuestionGuideRows(Sheet guideSheet, int rowIndex, CellStyle headerStyle) {
        Row questionHeader = guideSheet.createRow(rowIndex++);
        questionHeader.createCell(0).setCellValue("findPwdQuestionId");
        questionHeader.createCell(1).setCellValue("questionTitle");
        questionHeader.getCell(0).setCellStyle(headerStyle);
        questionHeader.getCell(1).setCellStyle(headerStyle);

        List<FindPwdQuestion> questionList = findPwdQuestionRepository.findAll(Sort.by(Sort.Direction.ASC, "findPwdQuestionSort"));
        for (FindPwdQuestion question : questionList) {
            Row row = guideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(question.getFindPwdQuestionId());
            row.createCell(1).setCellValue(question.getFindPwdQuestionTitle());
        }
        return rowIndex + 1;
    }

    private void writeAgreementGuideRows(Sheet guideSheet, int rowIndex, CellStyle headerStyle) {
        Row agreementHeader = guideSheet.createRow(rowIndex++);
        agreementHeader.createCell(0).setCellValue("serviceAgreementId");
        agreementHeader.createCell(1).setCellValue("agreementName");
        agreementHeader.createCell(2).setCellValue("required");
        agreementHeader.getCell(0).setCellStyle(headerStyle);
        agreementHeader.getCell(1).setCellStyle(headerStyle);
        agreementHeader.getCell(2).setCellStyle(headerStyle);

        List<ServiceAgreement> agreements = serviceAgreementRepository.findAll(Sort.by(Sort.Direction.ASC, "serviceAgreeSort"));
        for (ServiceAgreement agreement : agreements) {
            Row row = guideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(agreement.getServiceAgreeId());
            row.createCell(1).setCellValue(agreement.getServiceAgreeName());
            row.createCell(2).setCellValue(agreement.getIsServiceAgreeRequired().name());
        }
    }

    private void registerUserFromRow(Row row,
                                     Organization organization,
                                     DataFormatter formatter,
                                     BulkUploadReference reference,
                                     Set<String> fileLoginIdSet) {
        String loginId = getStringValue(row.getCell(0), formatter);
        String password = getStringValue(row.getCell(1), formatter);
        String name = getStringValue(row.getCell(2), formatter);
        String genderRaw = getStringValue(row.getCell(3), formatter);
        String phoneNumber = getStringValue(row.getCell(4), formatter);
        String questionIdRaw = getStringValue(row.getCell(5), formatter);
        String findPwdAnswer = getStringValue(row.getCell(6), formatter);
        String agreementIdsRaw = getStringValue(row.getCell(7), formatter);

        validateRequiredText(loginId, "아이디");
        validateRequiredText(password, "비밀번호");
        validateRequiredText(name, "이름");
        validateRequiredText(phoneNumber, "전화번호");
        validateRequiredText(questionIdRaw, "비밀번호 찾기 질문");
        validateRequiredText(findPwdAnswer, "비밀번호 찾기 답변");

        validateLoginId(loginId);
        validatePassword(password);
        validateUserName(name);
        validatePhoneNumber(phoneNumber);

        if (fileLoginIdSet.contains(loginId)) {
            throw new AlreadyDataException("파일 내 아이디 중복");
        }
        if (userRepository.findByUserLoginIdentifier(loginId).isPresent()) {
            throw new AlreadyDataException("아이디 중복");
        }

        Long questionId = parseLong(questionIdRaw, "비밀번호 찾기 질문 ID");
        if (!reference.questionMap.containsKey(questionId)) {
            throw new NotFoundDataException("존재하지 않는 비밀번호 찾기 질문입니다.");
        }

        GenderType gender = parseGender(genderRaw);
        List<Long> agreementIds = parseIdList(agreementIdsRaw, "약관 ID");
        if (agreementIds.isEmpty()) {
            agreementIds = new ArrayList<>(reference.requiredAgreementIds);
        }
        validateIds(agreementIds, reference.serviceAgreementMap.keySet(), "존재하지 않는 약관 ID가 포함되어 있습니다.");

        User user = userRepository.save(User.builder()
                .userLoginIdentifier(loginId)
                .userPassword(passwordEncoder.encode(password))
                .userName(name)
                .userPhoneNumber(phoneNumber)
                .userGender(gender)
                .findPwdQuestionId(questionId)
                .findPwdAnswer(findPwdAnswer)
                .organization(organization)
                .isVerify(YnType.N)
                .successCount(0)
                .build());

        serviceAgreementConsentService.saveUserServiceAgreements(user.getUserId(), agreementIds);
        fileLoginIdSet.add(loginId);
    }

    private BulkUploadReference getBulkUploadReference() {
        List<FindPwdQuestion> findPwdQuestions = findPwdQuestionRepository.findAll(Sort.by(Sort.Direction.ASC, "findPwdQuestionSort"));
        List<ServiceAgreement> serviceAgreements = serviceAgreementRepository.findAll(Sort.by(Sort.Direction.ASC, "serviceAgreeSort"));


        Map<Long, FindPwdQuestion> questionMap = new HashMap<>();
        for (FindPwdQuestion findPwdQuestion : findPwdQuestions) {
            questionMap.put(findPwdQuestion.getFindPwdQuestionId(), findPwdQuestion);
        }

        Map<Long, ServiceAgreement> serviceAgreementMap = new HashMap<>();
        List<Long> requiredAgreementIds = new ArrayList<>();
        for (ServiceAgreement serviceAgreement : serviceAgreements) {
            serviceAgreementMap.put(serviceAgreement.getServiceAgreeId(), serviceAgreement);
            if (serviceAgreement.getIsServiceAgreeRequired() == YnType.Y) {
                requiredAgreementIds.add(serviceAgreement.getServiceAgreeId());
            }
        }

        return new BulkUploadReference(
                questionMap,
                serviceAgreementMap,
                requiredAgreementIds
        );
    }

    private void validateRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestApiException(fieldName + "는 필수입니다.");
        }
    }

    private void validateLoginId(String loginId) {
        if (loginId.length() < 4 || loginId.length() > 12 || !LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new BadRequestApiException("아이디 형식 오류");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 20 || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestApiException("비밀번호 형식 오류");
        }
    }

    private void validateUserName(String name) {
        if (name.length() < 2 || name.length() > 100 || !NAME_PATTERN.matcher(name).matches()) {
            throw new BadRequestApiException("이름 형식 오류");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new BadRequestApiException("전화번호 형식 오류");
        }
    }

    private GenderType parseGender(String genderValue) {
        if (!StringUtils.hasText(genderValue)) {
            throw new BadRequestApiException("성별은 필수입니다.");
        }

        String normalizedGender = genderValue.trim().toUpperCase();
        if ("F".equals(normalizedGender)) {
            normalizedGender = "W";
        }
        if (!"M".equals(normalizedGender) && !"W".equals(normalizedGender)) {
            throw new BadRequestApiException("성별 형식 오류");
        }
        return GenderType.valueOf(normalizedGender);
    }

    private void validateIds(List<Long> ids, Set<Long> validIds, String errorMessage) {
        if (ids.stream().anyMatch(id -> !validIds.contains(id))) {
            throw new NotFoundDataException(errorMessage);
        }
    }

    private List<Long> parseIdList(String rawValue, String fieldName) {
        if (!StringUtils.hasText(rawValue)) {
            return new ArrayList<>();
        }

        try {
            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::parseLong)
                    .distinct()
                    .toList();
        } catch (NumberFormatException e) {
            throw new BadRequestApiException(fieldName + " 형식 오류");
        }
    }

    private Long parseLong(String rawValue, String fieldName) {
        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestApiException(fieldName + " 형식 오류");
        }
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }

        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            if (StringUtils.hasText(getStringValue(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String getFailReason(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        String message = throwable.getMessage();
        if (!StringUtils.hasText(message) && current != null) {
            message = current.getMessage();
        }
        return StringUtils.hasText(message) ? message.replace("\n", " ") : "행 처리 중 오류가 발생했습니다.";
    }

    private String getStringValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private record BulkUploadReference(
            Map<Long, FindPwdQuestion> questionMap,
            Map<Long, ServiceAgreement> serviceAgreementMap,
            List<Long> requiredAgreementIds
    ) {}
}
