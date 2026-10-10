package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainPointService;
import com.kaii.dentix.domain.daeguChain.application.DaeguRewardWalletProvisioningService;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.reward.config.UserRewardProperties;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardJourneyState;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRewardServiceTest {

    private UserRewardWalletRepository walletRepository;
    private UserRewardTransactionRepository transactionRepository;
    private OralExerciseContentRepository contentRepository;
    private DaeguRewardWalletProvisioningService rewardWalletProvisioningService;
    private DaeguChainDidService daeguChainDidService;
    private DaeguChainPointService daeguChainPointService;
    private ExternalTokenClient externalTokenClient;
    private UserRepository userRepository;
    private DaeguChainProperties daeguChainProperties;
    private JwtTokenUtil jwtTokenUtil;
    private Environment environment;
    private UserRewardService service;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        walletRepository = mock(UserRewardWalletRepository.class);
        transactionRepository = mock(UserRewardTransactionRepository.class);
        contentRepository = mock(OralExerciseContentRepository.class);
        rewardWalletProvisioningService = mock(DaeguRewardWalletProvisioningService.class);
        daeguChainDidService = mock(DaeguChainDidService.class);
        daeguChainPointService = mock(DaeguChainPointService.class);
        externalTokenClient = mock(ExternalTokenClient.class);
        userRepository = mock(UserRepository.class);
        daeguChainProperties = new DaeguChainProperties();
        jwtTokenUtil = mock(JwtTokenUtil.class);
        environment = mock(Environment.class);
        request = mock(HttpServletRequest.class);

        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(3L);
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                rewardWalletProvisioningService,
                daeguChainDidService,
                daeguChainPointService,
                externalTokenClient,
                userRepository,
                properties,
                daeguChainProperties,
                jwtTokenUtil,
                environment
        );

        when(jwtTokenUtil.getAccessToken(request)).thenReturn("access-token");
        when(jwtTokenUtil.getRoles("access-token", TokenType.AccessToken)).thenReturn(com.kaii.dentix.domain.type.UserRole.ROLE_USER);
        when(jwtTokenUtil.getUserId("access-token", TokenType.AccessToken)).thenReturn(7L);
        when(contentRepository.findById(11L)).thenReturn(Optional.of(content()));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = com.kaii.dentix.domain.type.UserRole.class, names = {"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
    @org.junit.jupiter.params.provider.NullSource
    void nonUserRolesCannotReadOrChangeRewardsEvenWhenServiceIsCalledDirectly(com.kaii.dentix.domain.type.UserRole role) {
        when(jwtTokenUtil.getRoles("access-token", TokenType.AccessToken)).thenReturn(role);
        assertThatThrownBy(() -> service.getWallet(request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> service.getTransactions(request)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> service.connectWallet(request, null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(request, null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(walletRepository, transactionRepository, contentRepository, userRepository,
                rewardWalletProvisioningService, daeguChainDidService, daeguChainPointService, externalTokenClient);
    }

    @Test
    void legacyClientIsRejectedBeforeAnyRewardOrWalletWrites() {
        ((UserRewardProperties) org.springframework.test.util.ReflectionTestUtils.getField(service, "userRewardProperties"))
                .setTokenTransferEnabled(true);
        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(request,
                new UserRewardDto.ButtonClickRequest(11L, "legacy", 1, 1)))
                .isInstanceOf(DeferredRewardClientRequiredException.class);
        verifyNoInteractions(walletRepository, transactionRepository, externalTokenClient);
    }

    @Test
    void callerSelectedWalletIsRejectedBeforeSavingOrProvisioning() {
        var connect = new UserRewardDto.WalletConnectRequest("did:attacker", "attacker-wallet");
        assertThatThrownBy(() -> service.connectWallet(request, connect)).isInstanceOf(BadRequestApiException.class);
        verify(walletRepository, never()).save(any());
        verifyNoInteractions(rewardWalletProvisioningService, daeguChainDidService, externalTokenClient);
    }

    @Test
    void contractLookupFailureDoesNotPersistAnApparentlyReceivedLocalReward() {
        ((UserRewardProperties) org.springframework.test.util.ReflectionTestUtils.getField(service, "userRewardProperties"))
                .setTokenTransferEnabled(true);
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L).walletAddress("wallet").daeguDid("did").walletPrivateKeyCiphertext("encrypted").build()));
        when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(externalTokenClient.getTokenList()).thenThrow(new BadRequestApiException("test lookup unavailable"));
        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(request,
                new UserRewardDto.ButtonClickRequest(11L, "lookup", 1, 1, true)))
                .isInstanceOf(BadRequestApiException.class);
        verify(transactionRepository, never()).save(any());
        verify(externalTokenClient, never()).transferTokenToWallet(any(),any(),any(),anyLong());
    }

    @Test
    void rewardOralExerciseButtonClickCreatesWalletAndTransaction() {
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.empty());
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(UserRewardTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3, true)
        );

        assertThat(response.getAmount()).isEqualTo(3L);
        assertThat(response.getPointBalance()).isEqualTo(3L);
        assertThat(response.isDuplicated()).isFalse();
        assertThat(response.getStatus()).isEqualTo(UserRewardTransactionStatus.LOCAL_RECORDED);
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_COIN
                        && transaction.getIdempotencyKey().equals("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                        && transaction.getCoinId().equals("essential_video_1")
        ));
    }

    @Test
    void rewardOralExerciseButtonClickReturnsExistingTransactionWhenTokenAlreadyRewarded() {
        UserRewardTransaction transaction = UserRewardTransaction.builder()
                .userId(7L)
                .oralExerciseContent(content())
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                .amount(3L)
                .balanceAfter(9L)
                .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                .coinId("essential_video_1")
                .build();
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.of(transaction));
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of(
                transaction,
                rewardTransaction(
                        UserRewardTransactionType.ORAL_EXERCISE_COIN,
                        UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                        3L,
                        "essential_video_2"
                ),
                rewardTransaction(
                        UserRewardTransactionType.ORAL_EXERCISE_COIN,
                        UserRewardTransactionStatus.POINT_MINTED,
                        3L,
                        "essential_video_3"
                )
        ));
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(9L)
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-2", 4, 4, true)
        );

        assertThat(response.isDuplicated()).isTrue();
        assertThat(response.getPointBalance()).isEqualTo(9L);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rewardOralExerciseButtonClickDoesNotReissueTokenAfterReclaim() {
        UserRewardTransaction rewardedTransaction = UserRewardTransaction.builder()
                .userId(7L)
                .oralExerciseContent(content())
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                .amount(3L)
                .balanceAfter(5L)
                .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                .coinId("essential_video_1")
                .tokenContractAddress("0x-token-contract")
                .build();
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.of(rewardedTransaction));
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of(
                rewardedTransaction,
                rewardTransaction(
                        UserRewardTransactionType.ORAL_EXERCISE_COIN,
                        UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                        5L,
                        "essential_video_2"
                ),
                rewardTransaction(
                        UserRewardTransactionType.ORAL_EXERCISE_RECLAIM,
                        UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                        3L,
                        "essential_video_1"
                )
        ));
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(5L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "rewatch-session", 2, 2, true)
        );

        assertThat(response.isDuplicated()).isTrue();
        assertThat(response.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFERRED);
        assertThat(response.getPointBalance()).isEqualTo(5L);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void getWalletExcludesFailedRewardTransactionsFromPointBalance() {
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(List.of(
                rewardTransaction(
                        UserRewardTransactionType.ORAL_EXERCISE_COIN,
                        UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                        2L,
                        "essential_video_1"
                ),
                rewardTransaction(
                        UserRewardTransactionType.ORAL_EXERCISE_COIN,
                        UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED,
                        3L,
                        "essential_video_2"
                )
        ));
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(5L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.getWallet(request);

        assertThat(response.getPointBalance()).isEqualTo(2L);
        assertThat(response.getDaeguDid()).isEqualTo("did:mitum:minic:0x-user-wallet");
        assertThat(response.getWalletAddress()).isEqualTo("0x-user-wallet");
        verify(walletRepository).save(argThat(wallet -> wallet.getPointBalance() == 2L));
    }

    @Test
    void getTransactionsReturnsZeroAmountForFailedRewards() {
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(java.util.List.of(
                UserRewardTransaction.builder()
                        .userId(7L)
                        .oralExerciseContent(content())
                        .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                        .status(UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED)
                        .amount(3L)
                        .balanceAfter(3L)
                        .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                        .coinId("essential_video_1")
                        .build()
        ));

        UserRewardDto.TransactionListResponse response = service.getTransactions(request);

        assertThat(response.getTransactions()).singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
                    assertThat(transaction.getAmount()).isZero();
                });
        assertThat(response.getRewardJourney().getState()).isEqualTo(UserRewardJourneyState.COLLECTING);
        assertThat(response.getRewardJourney().getEssentialReceivedCount()).isZero();
    }

    @Test
    void rewardOralExerciseButtonClickRejectsNewTokenAfterJourneyCompletion() {
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.empty());
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        List<UserRewardTransaction> transactions = new java.util.ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            transactions.add(rewardTransaction(
                    UserRewardTransactionType.ORAL_EXERCISE_COIN,
                    UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                    3L,
                    "essential_video_" + index
            ));
            transactions.add(rewardTransaction(
                    UserRewardTransactionType.ORAL_EXERCISE_RECLAIM,
                    UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                    3L,
                    "essential_video_" + index
            ));
        }
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(transactions);

        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "completed-session", 3, 3, true)
        ))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("이미 완료");

        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void rewardOralExerciseButtonClickTransfersMappedTokenWhenConfigured() throws Exception {
        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(1L);
        properties.setTokenTransferEnabled(true);
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                rewardWalletProvisioningService,
                daeguChainDidService,
                daeguChainPointService,
                externalTokenClient,
                userRepository,
                properties,
                daeguChainProperties,
                jwtTokenUtil,
                environment
        );
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.empty());
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(0L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .walletPrivateKeyCiphertext("encrypted-private-key")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(UserRewardTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(externalTokenClient.getTokenList()).thenReturn(rewardTokenList());
        when(externalTokenClient.transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-token-contract",
                "0x-user-wallet",
                1L
        )).thenReturn(new ObjectMapper().readTree("""
                {
                  "Date": "2026-06-25T01:00:00Z",
                  "Sender": "0x-admin",
                  "Receiver": "0x-user-wallet",
                  "Amount": "1"
                }
                """));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3, true)
        );

        assertThat(response.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING);
        assertThat(response.getAmount()).isZero();
        verify(externalTokenClient, never()).transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-token-contract",
                "0x-user-wallet",
                1L
        );
        verify(rewardWalletProvisioningService, never()).approveRewardContract(
                7L,
                "0x-token-contract",
                "0x-user-wallet",
                "encrypted-private-key", 1L
        );
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getCoinId().equals("essential_video_1")
                        && transaction.getTokenContractAddress().equals("0x-token-contract")
        ));
    }

    @Test
    void rewardOralExerciseButtonClickRepairsMissingDidWithASeparateRewardWallet() throws Exception {
        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(1L);
        properties.setTokenTransferEnabled(true);
        daeguChainProperties.getRewardTokenContracts().put("ESSENTIAL_VIDEO_1", "0x-token-contract");
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                rewardWalletProvisioningService,
                daeguChainDidService,
                daeguChainPointService,
                externalTokenClient,
                userRepository,
                properties,
                daeguChainProperties,
                jwtTokenUtil,
                environment
        );
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("local-user")
                .daeguDidStatus(UserDaeguIdentityStatus.FAILED)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.empty());
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(UserRewardTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(daeguChainDidService.createAccount(any())).thenReturn(new DaeguChainDto.ApiResponse<>(
                "OK",
                null,
                "",
                new ObjectMapper().readTree("""
                        {
                          "did": "did:key:z6MkLocalUser",
                          "publicKey": "public-key",
                          "walletAddress": "0x-local-user-wallet"
                        }
                        """),
                "cid-did"
        ));
        when(rewardWalletProvisioningService.createActivatedWallet(7L)).thenReturn(
                new DaeguRewardWalletProvisioningService.ProvisionedWallet(
                        "0x-reward-wallet", "encrypted-reward-wallet-private-key"
                )
        );
        when(externalTokenClient.transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-token-contract",
                "0x-reward-wallet",
                1L
        )).thenReturn(new ObjectMapper().readTree("""
                {
                  "Date": "2026-08-21T01:00:00Z",
                  "Receiver": "0x-reward-wallet",
                  "Amount": "1"
                }
                """));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-local", 3, 3, true)
        );

        assertThat(response.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING);
        assertThat(response.getAmount()).isZero();
        assertThat(user.getDaeguDid()).isEqualTo("did:key:z6MkLocalUser");
        assertThat(user.getDaeguDidKey()).isEqualTo("public-key");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        verify(daeguChainDidService).createAccount(argThat(body ->
                "local-user".equals(body.get("label"))
        ));
        verify(walletRepository, atLeastOnce()).save(argThat(wallet ->
                "did:key:z6MkLocalUser".equals(wallet.getDaeguDid())
                        && "0x-reward-wallet".equals(wallet.getWalletAddress())
                        && "encrypted-reward-wallet-private-key".equals(
                                wallet.getWalletPrivateKeyCiphertext()
                        )
        ));
    }

    @Test
    void rewardOralExerciseButtonClickUsesConfiguredContractWhenTokenNamesAreDuplicated() throws Exception {
        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(1L);
        properties.setTokenTransferEnabled(true);
        daeguChainProperties.getRewardTokenContracts().put("ESSENTIAL_VIDEO_1", "0x-allowed-contract");
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                rewardWalletProvisioningService,
                daeguChainDidService,
                daeguChainPointService,
                externalTokenClient,
                userRepository,
                properties,
                daeguChainProperties,
                jwtTokenUtil,
                environment
        );
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.empty());
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(0L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(UserRewardTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(externalTokenClient.transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-allowed-contract",
                "0x-user-wallet",
                1L
        )).thenReturn(new ObjectMapper().readTree("""
                {
                  "Date": "2026-06-25T01:00:00Z"
                }
                """));

        service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3, true)
        );

        verify(externalTokenClient, never()).getTokenList();
        verify(externalTokenClient, never()).transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-allowed-contract",
                "0x-user-wallet",
                1L
        );
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getCoinId().equals("essential_video_1")
                        && transaction.getTokenContractAddress().equals("0x-allowed-contract")
        ));
    }

    @Test
    void rewardOralExerciseButtonClickQueuesBeforeCallingExternalTransfer() throws Exception {
        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(1L);
        properties.setTokenTransferEnabled(true);
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                rewardWalletProvisioningService,
                daeguChainDidService,
                daeguChainPointService,
                externalTokenClient,
                userRepository,
                properties,
                daeguChainProperties,
                jwtTokenUtil,
                environment
        );
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.empty());
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(0L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(UserRewardTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(externalTokenClient.getTokenList()).thenReturn(rewardTokenList());
        when(externalTokenClient.transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-token-contract",
                "0x-user-wallet",
                1L
        )).thenThrow(new BadRequestApiException("token transfer failed"));

        var queued = service.rewardOralExerciseButtonClick(request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3, true));
        assertThat(queued.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING);
        verify(externalTokenClient, never()).transferTokenToWallet(anyString(), anyString(), anyString(), anyLong());
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING
                        && transaction.getCoinId().equals("essential_video_1")
        ));
        verify(walletRepository, never()).save(argThat(wallet -> wallet.getPointBalance() > 0));
    }

    @Test
    void rewardOralExerciseButtonClickHoldsLegacyFailureForReview() throws Exception {
        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(1L);
        properties.setTokenTransferEnabled(true);
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                rewardWalletProvisioningService,
                daeguChainDidService,
                daeguChainPointService,
                externalTokenClient,
                userRepository,
                properties,
                daeguChainProperties,
                jwtTokenUtil,
                environment
        );
        UserRewardTransaction transaction = UserRewardTransaction.builder()
                .userId(7L)
                .oralExerciseContent(content())
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED)
                .amount(1L)
                .balanceAfter(1L)
                .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                .coinId("essential_video_1")
                .build();
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.of(transaction));
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(1L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(externalTokenClient.getTokenList()).thenReturn(rewardTokenList());
        when(externalTokenClient.transferTokenToWallet(any(), any(), any(), anyLong()))
                .thenThrow(new BadRequestApiException("token transfer failed"));

        var queued = service.rewardOralExerciseButtonClick(request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3, true));
        assertThat(queued.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        verify(externalTokenClient, never()).transferTokenToWallet(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void rewardOralExerciseButtonClickDoesNotResendUnconfirmedLegacyTransfer() throws Exception {
        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(1L);
        properties.setTokenTransferEnabled(true);
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                rewardWalletProvisioningService,
                daeguChainDidService,
                daeguChainPointService,
                externalTokenClient,
                userRepository,
                properties,
                daeguChainProperties,
                jwtTokenUtil,
                environment
        );
        UserRewardTransaction transaction = UserRewardTransaction.builder()
                .userId(7L)
                .oralExerciseContent(content())
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED)
                .amount(1L)
                .balanceAfter(1L)
                .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_1")
                .coinId("essential_video_1")
                .build();
        when(transactionRepository.findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                7L,
                "essential_video_1",
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.CANCELED
        )).thenReturn(Optional.of(transaction));
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(1L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(externalTokenClient.getTokenList()).thenReturn(rewardTokenList());
        when(externalTokenClient.transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-token-contract",
                "0x-user-wallet",
                1L
        )).thenReturn(new ObjectMapper().readTree("""
                {
                  "tx_hash": "0x-retry-tx",
                  "fact_hash": "retry-fact"
                }
                """));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3, true)
        );

        assertThat(response.isDuplicated()).isTrue();
        assertThat(response.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        assertThat(response.getAmount()).isZero();
        verify(externalTokenClient, never()).transferTokenToWallet(
                "ESSENTIAL_VIDEO_1",
                "0x-token-contract",
                "0x-user-wallet",
                1L
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rewardOralExerciseButtonClickRequiresSelectedButtonNumber() {
        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", null, null, true)
        ))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("selectedButtonNumber is required");
    }

    @Test
    void rewardOralExerciseButtonClickRejectsMismatchedTargetButtonNumber() {
        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 2, 4, true)
        ))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("selectedButtonNumber does not match targetButtonNumber");
    }

    @Test
    void connectWalletCreatesDidWalletWhenWalletAddressIsEmpty() throws Exception {
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        new ObjectMapper().readTree("""
                                {
                                  "did": "did:mitum:minic:0x-wallet",
                                  "address": "0x-wallet"
                                }
                                """),
                        "cid"
                ));
        when(rewardWalletProvisioningService.createActivatedWallet(7L)).thenReturn(
                new DaeguRewardWalletProvisioningService.ProvisionedWallet(
                        "0x-reward-wallet", "encrypted-private-key"
                )
        );
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.connectWallet(request, null);

        assertThat(response.getDaeguDid()).isEqualTo("did:mitum:minic:0x-wallet");
        assertThat(response.getWalletAddress()).isEqualTo("0x-reward-wallet");
        verify(daeguChainDidService).createAccount(any());
        verify(rewardWalletProvisioningService).createActivatedWallet(7L);
    }

    @Test
    void connectWalletCreatesDaeguWalletWhenDidResponseHasNoAddress() throws Exception {
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        new ObjectMapper().readTree("""
                                {
                                  "did": "did:key:z6MkUser"
                                }
                                """),
                        "cid"
                ));
        when(rewardWalletProvisioningService.createActivatedWallet(7L)).thenReturn(
                new DaeguRewardWalletProvisioningService.ProvisionedWallet(
                        "0x-wallet", "encrypted-private-key"
                )
        );
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.connectWallet(request, null);

        assertThat(response.getDaeguDid()).isEqualTo("did:key:z6MkUser");
        assertThat(response.getWalletAddress()).isEqualTo("0x-wallet");
        verify(rewardWalletProvisioningService).createActivatedWallet(7L);
    }

    @Test
    void getWalletReplacesAnExistingAddressWhenItsSigningKeyIsMissing() {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("local-user")
                .daeguDid("did:key:self-issued")
                .daeguDidStatus(UserDaeguIdentityStatus.ISSUED)
                .build();
        UserRewardWallet wallet = UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(0L)
                .daeguDid("did:key:self-issued")
                .walletAddress("0x-wallet-without-key")
                .build();
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(rewardWalletProvisioningService.createActivatedWallet(7L)).thenReturn(
                new DaeguRewardWalletProvisioningService.ProvisionedWallet(
                        "0x-repaired-wallet", "encrypted-repaired-private-key"
                )
        );
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.getWallet(request);

        assertThat(response.getDaeguDid()).isEqualTo("did:key:self-issued");
        assertThat(response.getWalletAddress()).isEqualTo("0x-repaired-wallet");
        assertThat(wallet.getWalletPrivateKeyCiphertext()).isEqualTo("encrypted-repaired-private-key");
        verify(rewardWalletProvisioningService).createActivatedWallet(7L);
    }

    @Test
    void getWalletReplacesAnExistingAddressWhenItContainsALegacyDidKey() {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("local-user")
                .daeguDid("did:key:self-issued")
                .daeguDidStatus(UserDaeguIdentityStatus.ISSUED)
                .build();
        UserRewardWallet wallet = UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(0L)
                .daeguDid("did:key:self-issued")
                .walletAddress("0x-legacy-wallet")
                .walletPrivateKeyCiphertext("encrypted-legacy-did-key")
                .build();
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(rewardWalletProvisioningService.requiresWalletReplacement("encrypted-legacy-did-key"))
                .thenReturn(true);
        when(rewardWalletProvisioningService.createActivatedWallet(7L)).thenReturn(
                new DaeguRewardWalletProvisioningService.ProvisionedWallet(
                        "0x-repaired-wallet", "encrypted-repaired-private-key"
                )
        );
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.getWallet(request);

        assertThat(response.getDaeguDid()).isEqualTo("did:key:self-issued");
        assertThat(response.getWalletAddress()).isEqualTo("0x-repaired-wallet");
        assertThat(wallet.getWalletPrivateKeyCiphertext()).isEqualTo("encrypted-repaired-private-key");
        verify(rewardWalletProvisioningService).createActivatedWallet(7L);
    }

    @Test
    void connectWalletCreatesLocalTestAddressInDevWhenDaeguTokenIsMissing() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(daeguChainDidService.createAccount(any()))
                .thenThrow(new BadRequestApiException("token is required"));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.connectWallet(request, null);

        assertThat(response.getWalletAddress()).startsWith("0x");
        assertThat(response.getWalletAddress()).hasSize(42);
    }

    private OralExerciseContent content() {
        OralExerciseContent content = OralExerciseContent.builder()
                .contentSort(2)
                .title("입 체조")
                .description("description")
                .learningPoint("learning point")
                .durationSeconds(60)
                .level("easy")
                .active(true)
                .build();
        ReflectionTestUtils.setField(content, "oralExerciseContentId", 11L);
        return content;
    }

    private UserRewardTransaction rewardTransaction(
            UserRewardTransactionType type,
            UserRewardTransactionStatus status,
            long amount,
            String coinId
    ) {
        return UserRewardTransaction.builder()
                .userId(7L)
                .oralExerciseContent(type == UserRewardTransactionType.ORAL_EXERCISE_COIN ? content() : null)
                .type(type)
                .status(status)
                .amount(amount)
                .balanceAfter(0L)
                .idempotencyKey("%s:%s:%s".formatted(type, coinId, status))
                .coinId(coinId)
                .build();
    }

    private com.fasterxml.jackson.databind.JsonNode rewardTokenList() throws Exception {
        return new ObjectMapper().readTree("""
                {
                  "data": [
                    {
                      "contract": "0x-token-contract",
                      "data": {
                        "name": "ESSENTIAL_VIDEO_1",
                        "symbol": "MYT",
                        "supply": 100
                      }
                    }
                  ]
                }
                """);
    }
}
