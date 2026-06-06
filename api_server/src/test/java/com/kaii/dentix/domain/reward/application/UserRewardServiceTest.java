package com.kaii.dentix.domain.reward.application;

import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainPointService;
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
        jwtTokenUtil = mock(JwtTokenUtil.class);
        environment = mock(Environment.class);
        request = mock(HttpServletRequest.class);

        UserRewardProperties properties = new UserRewardProperties();
        properties.setOralExerciseCoinAmount(3L);
        service = new UserRewardService(
                walletRepository,
                transactionRepository,
                contentRepository,
                daeguChainAccountService,
                daeguChainPointService,
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
    void rewardOralExerciseCoinCreatesWalletAndTransaction() {
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(7L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(UserRewardWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(UserRewardTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseCoin(
                request,
                new UserRewardDto.CoinClickRequest(11L, "session-1", "coin-1")
        );

        assertThat(response.getAmount()).isEqualTo(3L);
        assertThat(response.getPointBalance()).isEqualTo(3L);
        assertThat(response.isDuplicated()).isFalse();
        assertThat(response.getStatus()).isEqualTo(UserRewardTransactionStatus.LOCAL_RECORDED);
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_COIN
                        && transaction.getIdempotencyKey().equals("ORAL_EXERCISE_COIN:7:11:session-1:coin-1")
        ));
    }

    @Test
    void rewardOralExerciseCoinReturnsExistingTransactionWhenDuplicated() {
        UserRewardTransaction transaction = UserRewardTransaction.builder()
                .userId(7L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                .amount(3L)
                .balanceAfter(9L)
                .idempotencyKey("ORAL_EXERCISE_COIN:7:11:session-1:coin-1")
                .build();
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.of(transaction));

        UserRewardDto.RewardResponse response = service.rewardOralExerciseCoin(
                request,
                new UserRewardDto.CoinClickRequest(11L, "session-1", "coin-1")
        );

        assertThat(response.isDuplicated()).isTrue();
        assertThat(response.getPointBalance()).isEqualTo(9L);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rewardOralExerciseCoinRequiresCoinId() {
        assertThatThrownBy(() -> service.rewardOralExerciseCoin(
                request,
                new UserRewardDto.CoinClickRequest(11L, "session-1", "")
        ))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("coinId is required");
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
        return OralExerciseContent.builder()
                .contentSort(1)
                .title("입 체조")
                .description("description")
                .learningPoint("learning point")
                .durationSeconds(60)
                .level("easy")
                .active(true)
                .build();
    }
}
