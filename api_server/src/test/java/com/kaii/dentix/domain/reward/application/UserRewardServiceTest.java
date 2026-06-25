package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainPointService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainToken20Service;
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
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRewardServiceTest {

    private UserRewardWalletRepository walletRepository;
    private UserRewardTransactionRepository transactionRepository;
    private OralExerciseContentRepository contentRepository;
    private DaeguChainAccountService daeguChainAccountService;
    private DaeguChainPointService daeguChainPointService;
    private DaeguChainToken20Service daeguChainToken20Service;
    private JwtTokenUtil jwtTokenUtil;
    private Environment environment;
    private UserRewardService service;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        walletRepository = mock(UserRewardWalletRepository.class);
        transactionRepository = mock(UserRewardTransactionRepository.class);
        contentRepository = mock(OralExerciseContentRepository.class);
        daeguChainAccountService = mock(DaeguChainAccountService.class);
        daeguChainPointService = mock(DaeguChainPointService.class);
        daeguChainToken20Service = mock(DaeguChainToken20Service.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        environment = mock(Environment.class);
        request = mock(HttpServletRequest.class);

        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(3L);
        DaeguChainProperties daeguChainProperties = new DaeguChainProperties();
        daeguChainProperties.setTokenOwnerAddress("0x-admin");
        daeguChainProperties.setTokenOwnerPrivateKey("admin-private-key");
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                daeguChainAccountService,
                daeguChainPointService,
                daeguChainToken20Service,
                daeguChainProperties,
                properties,
                jwtTokenUtil,
                environment
        );

        when(jwtTokenUtil.getAccessToken(request)).thenReturn("access-token");
        when(jwtTokenUtil.getUserId("access-token", TokenType.AccessToken)).thenReturn(7L);
        when(contentRepository.findById(11L)).thenReturn(Optional.of(content()));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
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
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3)
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

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-2", 4, 4)
        );

        assertThat(response.isDuplicated()).isTrue();
        assertThat(response.getPointBalance()).isEqualTo(9L);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rewardOralExerciseButtonClickTransfersMappedTokenWhenConfigured() throws Exception {
        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(1L);
        properties.setTokenTransferEnabled(true);
        DaeguChainProperties daeguChainProperties = new DaeguChainProperties();
        daeguChainProperties.setTokenOwnerAddress("0x-admin");
        daeguChainProperties.setTokenOwnerPrivateKey("admin-private-key");
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                daeguChainAccountService,
                daeguChainPointService,
                daeguChainToken20Service,
                daeguChainProperties,
                properties,
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
                .walletAddress("0x-user-wallet")
                .build()));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(UserRewardTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(daeguChainToken20Service.getTokenList(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        new ObjectMapper().readTree("""
                                [
                                  {
                                    "contract": "0x-token-contract",
                                    "data": {
                                      "name": "ESSENTIAL_VIDEO_1"
                                    }
                                  }
                                ]
                                """),
                        "cid"
                ));
        when(daeguChainToken20Service.transferToken(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        new ObjectMapper().readTree("""
                                {
                                  "tx": {
                                    "hash": "tx-hash",
                                    "fact_hash": "fact-hash"
                                  }
                                }
                                """),
                        "cid"
                ));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 3, 3)
        );

        assertThat(response.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFERRED);
        verify(daeguChainToken20Service).transferToken(argThat(transfer ->
                transfer.getContAddr().equals("0x-token-contract")
                        && transfer.getSender().equals("0x-admin")
                        && transfer.getSenderPkey().equals("admin-private-key")
                        && transfer.getReceiver().equals("0x-user-wallet")
                        && transfer.getAmount().equals("1")
        ));
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getCoinId().equals("essential_video_1")
        ));
    }

    @Test
    void rewardOralExerciseButtonClickRequiresSelectedButtonNumber() {
        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", null, null)
        ))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("selectedButtonNumber is required");
    }

    @Test
    void rewardOralExerciseButtonClickRejectsMismatchedTargetButtonNumber() {
        assertThatThrownBy(() -> service.rewardOralExerciseButtonClick(
                request,
                new UserRewardDto.ButtonClickRequest(11L, "session-1", 2, 4)
        ))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("selectedButtonNumber does not match targetButtonNumber");
    }

    @Test
    void connectWalletCreatesDaeguAccountWhenWalletAddressIsEmpty() {
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(daeguChainAccountService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "success",
                        null,
                        "ok",
                        new DaeguChainDto.KeyPairData(new DaeguChainDto.KeyPair(
                                "private-key",
                                "public-key",
                                "0x-wallet"
                        )),
                        "cid"
                ));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.connectWallet(
                request,
                new UserRewardDto.WalletConnectRequest("did:daegu:test", null)
        );

        assertThat(response.getDaeguDid()).isEqualTo("did:daegu:test");
        assertThat(response.getWalletAddress()).isEqualTo("0x-wallet");
        verify(daeguChainAccountService).createAccount(any());
    }

    @Test
    void connectWalletCreatesLocalTestAddressInDevWhenDaeguTokenIsMissing() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(daeguChainAccountService.createAccount(any()))
                .thenThrow(new BadRequestApiException("token is required"));
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.WalletResponse response = service.connectWallet(request, null);

        assertThat(response.getWalletAddress()).startsWith("0x");
        assertThat(response.getWalletAddress()).hasSize(42);
    }

    private OralExerciseContent content() {
        OralExerciseContent content = OralExerciseContent.builder()
                .contentSort(1)
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
}
