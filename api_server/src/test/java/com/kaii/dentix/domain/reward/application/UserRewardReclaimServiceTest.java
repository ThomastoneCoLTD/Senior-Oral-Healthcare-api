package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.reward.config.UserRewardProperties;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserRewardReclaimServiceTest {

    private UserRewardTransactionRepository transactionRepository;
    private UserRewardWalletRepository walletRepository;
    private ExternalTokenClient externalTokenClient;
    private DaeguChainProperties daeguChainProperties;
    private JwtTokenUtil jwtTokenUtil;
    private UserRewardReclaimService service;
    private HttpServletRequest request;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        transactionRepository = mock(UserRewardTransactionRepository.class);
        walletRepository = mock(UserRewardWalletRepository.class);
        externalTokenClient = mock(ExternalTokenClient.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        request = mock(HttpServletRequest.class);

        daeguChainProperties = new DaeguChainProperties();
        daeguChainProperties.setTokenOwnerAddress("0x-token-owner");
        UserRewardProperties userRewardProperties = new UserRewardProperties();
        userRewardProperties.setTokenTransferEnabled(true);

        service = new UserRewardReclaimService(
                transactionRepository,
                walletRepository,
                externalTokenClient,
                daeguChainProperties,
                userRewardProperties,
                jwtTokenUtil
        );

        when(jwtTokenUtil.getAccessToken(request)).thenReturn("access-token");
        when(jwtTokenUtil.getUserId("access-token", TokenType.AccessToken)).thenReturn(7L);
        when(walletRepository.findByUserId(7L)).thenReturn(Optional.of(UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(5L)
                .daeguDid("did:mitum:minic:0x-user-wallet")
                .walletAddress("0x-user-wallet")
                .build()));
        when(transactionRepository.save(any(UserRewardTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void reclaimOralExerciseTokensTransfersRewardsBackToTokenOwner() throws Exception {
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(essentialRewards());
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(externalTokenClient.transferToken(anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "tx_hash": "0x-reclaim-tx",
                          "fact_hash": "reclaim-fact"
                        }
                        """));

        UserRewardDto.ReclaimResponse response = service.reclaimOralExerciseTokens(request);

        assertThat(response.getReclaimedCount()).isEqualTo(5);
        assertThat(response.getSkippedCount()).isZero();
        assertThat(response.getFailedCount()).isZero();
        assertThat(response.getReclaimedAmount()).isEqualTo(5L);
        verify(externalTokenClient, times(5)).transferToken(
                eq("did:mitum:minic:0x-user-wallet"),
                startsWith("ESSENTIAL_VIDEO_"),
                startsWith("0x-token-contract-"),
                eq("0x-token-owner"),
                eq(1L)
        );
    }

    @Test
    void reclaimOralExerciseTokensSkipsAlreadyTransferredReclaims() {
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(essentialRewards());
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(UserRewardTransaction.builder()
                .userId(7L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_RECLAIM)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                .amount(1L)
                .balanceAfter(5L)
                .idempotencyKey("ORAL_EXERCISE_RECLAIM:7:1")
                .coinId("essential_video_1")
                .tokenContractAddress("0x-token-contract-1")
                .build()));

        UserRewardDto.ReclaimResponse response = service.reclaimOralExerciseTokens(request);

        assertThat(response.getReclaimedCount()).isZero();
        assertThat(response.getSkippedCount()).isEqualTo(5);
        assertThat(response.getFailedCount()).isZero();
        verify(externalTokenClient, never()).transferToken(anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void reclaimOralExerciseTokensResolvesLocalRecordedRewardContracts() throws Exception {
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(localRecordedEssentialRewards());
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(externalTokenClient.getTokenList()).thenReturn(essentialRewardTokenList());
        when(externalTokenClient.transferToken(anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "tx_hash": "0x-reclaim-tx",
                          "fact_hash": "reclaim-fact"
                        }
                        """));

        UserRewardDto.ReclaimResponse response = service.reclaimOralExerciseTokens(request);

        assertThat(response.getReclaimedCount()).isEqualTo(5);
        assertThat(response.getFailedCount()).isZero();
        assertThat(response.getTransactions())
                .extracting(UserRewardDto.TransactionResponse::getTokenContractAddress)
                .containsExactlyInAnyOrder(
                        "0x-token-contract-1",
                        "0x-token-contract-2",
                        "0x-token-contract-3",
                        "0x-token-contract-4",
                        "0x-token-contract-5"
                );
        verify(externalTokenClient).getTokenList();
        verify(externalTokenClient).transferToken(
                eq("did:mitum:minic:0x-user-wallet"),
                eq("ESSENTIAL_VIDEO_1"),
                eq("0x-token-contract-1"),
                eq("0x-token-owner"),
                eq(1L)
        );
    }

    @Test
    void reclaimOralExerciseTokensUsesConfiguredContractsBeforeTokenServerList() throws Exception {
        java.util.stream.IntStream.rangeClosed(1, 5)
                .forEach(index -> daeguChainProperties.getRewardTokenContracts()
                        .put("ESSENTIAL_VIDEO_" + index, "0x-allowed-contract-" + index));
        when(transactionRepository.findByUserIdOrderByCreatedDesc(7L)).thenReturn(localRecordedEssentialRewards());
        when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(externalTokenClient.transferToken(anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "tx_hash": "0x-reclaim-tx"
                        }
                        """));

        UserRewardDto.ReclaimResponse response = service.reclaimOralExerciseTokens(request);

        assertThat(response.getReclaimedCount()).isEqualTo(5);
        verify(externalTokenClient, never()).getTokenList();
        verify(externalTokenClient).transferToken(
                eq("did:mitum:minic:0x-user-wallet"),
                eq("ESSENTIAL_VIDEO_1"),
                eq("0x-allowed-contract-1"),
                eq("0x-token-owner"),
                eq(1L)
        );
    }

    private List<UserRewardTransaction> essentialRewards() {
        return java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(index -> {
                    UserRewardTransaction transaction = UserRewardTransaction.builder()
                            .userId(7L)
                            .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                            .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                            .amount(1L)
                            .balanceAfter(index)
                            .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_" + index)
                            .coinId("essential_video_" + index)
                            .tokenContractAddress("0x-token-contract-" + index)
                            .build();
                    ReflectionTestUtils.setField(transaction, "userRewardTransactionId", (long) index);
                    return transaction;
                })
                .toList();
    }

    private List<UserRewardTransaction> localRecordedEssentialRewards() {
        return java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(index -> {
                    UserRewardTransaction transaction = UserRewardTransaction.builder()
                            .userId(7L)
                            .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                            .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                            .amount(1L)
                            .balanceAfter(index)
                            .idempotencyKey("ORAL_EXERCISE_BUTTON:7:essential_video_" + index)
                            .coinId("essential_video_" + index)
                            .build();
                    ReflectionTestUtils.setField(transaction, "userRewardTransactionId", (long) index);
                    return transaction;
                })
                .toList();
    }

    private com.fasterxml.jackson.databind.JsonNode essentialRewardTokenList() throws Exception {
        return objectMapper.readTree("""
                {
                  "data": [
                    {
                      "contract": "0x-token-contract-1",
                      "data": { "name": "essential_video_1" }
                    },
                    {
                      "contract": "0x-token-contract-2",
                      "data": { "name": "essential_video_2" }
                    },
                    {
                      "contract": "0x-token-contract-3",
                      "data": { "name": "essential_video_3" }
                    },
                    {
                      "contract": "0x-token-contract-4",
                      "data": { "name": "essential_video_4" }
                    },
                    {
                      "contract": "0x-token-contract-5",
                      "data": { "name": "essential_video_5" }
                    }
                  ]
                }
                """);
    }
}
