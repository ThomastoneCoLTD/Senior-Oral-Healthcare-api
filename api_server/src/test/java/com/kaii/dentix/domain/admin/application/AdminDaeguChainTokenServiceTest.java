package com.kaii.dentix.domain.admin.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDaeguChainTokenServiceTest {

    private ExternalTokenClient externalTokenClient;
    private DaeguChainProperties properties;
    private UserRewardTransactionRepository userRewardTransactionRepository;
    private UserRepository userRepository;
    private AdminDaeguChainTokenService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        externalTokenClient = mock(ExternalTokenClient.class);
        properties = new DaeguChainProperties();
        properties.setTokenSymbol("MYT");
        userRewardTransactionRepository = mock(UserRewardTransactionRepository.class);
        userRepository = mock(UserRepository.class);
        service = new AdminDaeguChainTokenService(
                externalTokenClient,
                properties,
                userRewardTransactionRepository,
                userRepository
        );
        objectMapper = new ObjectMapper();
    }

    @Test
    void getTokenNamesReturnsOnlyTokenDataNames() throws Exception {
        JsonNode data = objectMapper.readTree("""
                [
                  {
                    "contract": "0x-token",
                    "data": {
                      "name": "ESSENTIAL_VIDEO_1",
                      "symbol": "MYT"
                    }
                  },
                  {
                    "contract": "0x-token2",
                    "data": {
                      "name": "ESSENTIAL_VIDEO_2",
                      "symbol": "MYT"
                    }
                  },
                  {
                    "contract": "0x-other",
                    "data": {
                      "name": "UNRELATED_TOKEN",
                      "symbol": "MYT"
                    }
                  }
                ]
                """);
        when(externalTokenClient.getTokenList()).thenReturn(data);

        List<String> tokenNames = service.getTokenNames();

        assertThat(tokenNames).containsExactly(
                "ESSENTIAL_VIDEO_1",
                "ESSENTIAL_VIDEO_2",
                "ESSENTIAL_VIDEO_3",
                "ESSENTIAL_VIDEO_4",
                "ESSENTIAL_VIDEO_5",
                "OPTIONAL_VIDEO_1",
                "OPTIONAL_VIDEO_2",
                "OPTIONAL_VIDEO_3",
                "OPTIONAL_VIDEO_4",
                "OPTIONAL_VIDEO_5",
                "OPTIONAL_VIDEO_6",
                "OPTIONAL_VIDEO_7"
        );
    }

    @Test
    void getTokenOptionsReturnsRewardTokensWithIssueMetadata() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "data": [
                    {
                      "contract": "0x-essential-5",
                      "data": {
                        "name": "ESSENTIAL_VIDEO_5",
                        "symbol": "MYT",
                        "supply": 100,
                        "decimals": 9,
                        "owner": "0x-owner"
                      },
                      "tx": {
                        "hash": "tx-hash",
                        "fact_hash": "fact-hash"
                      },
                      "issued": "2026-07-04T02:56:39.524Z"
                    },
                    {
                      "contract": "0x-optional-1",
                      "data": {
                        "name": "OPTIONAL_VIDEO_1",
                        "symbol": "MYT",
                        "supply": 100
                      }
                    }
                  ]
                }
                """);
        when(externalTokenClient.getTokenList()).thenReturn(data);

        List<AdminDaeguChainTokenDto.TokenOption> tokenOptions = service.getTokenOptions();

        assertThat(tokenOptions).extracting(AdminDaeguChainTokenDto.TokenOption::getTokenName)
                .containsExactly(
                        "ESSENTIAL_VIDEO_1",
                        "ESSENTIAL_VIDEO_2",
                        "ESSENTIAL_VIDEO_3",
                        "ESSENTIAL_VIDEO_4",
                        "ESSENTIAL_VIDEO_5",
                        "OPTIONAL_VIDEO_1",
                        "OPTIONAL_VIDEO_2",
                        "OPTIONAL_VIDEO_3",
                        "OPTIONAL_VIDEO_4",
                        "OPTIONAL_VIDEO_5",
                        "OPTIONAL_VIDEO_6",
                        "OPTIONAL_VIDEO_7"
                );
        AdminDaeguChainTokenDto.TokenOption essential5 = tokenOptions.get(4);
        assertThat(essential5.getContractAddress()).isEqualTo("0x-essential-5");
        assertThat(essential5.getDecimals()).isEqualTo(9);
        assertThat(essential5.getOwner()).isEqualTo("0x-owner");
        assertThat(essential5.getTxHash()).isEqualTo("tx-hash");
        assertThat(essential5.getFactHash()).isEqualTo("fact-hash");
        assertThat(essential5.getIssued()).isEqualTo("2026-07-04T02:56:39.524Z");
    }

    @Test
    void getTokenOptionsKeepsOnlyConfiguredContractWhenTokenNamesAreDuplicated() throws Exception {
        properties.getRewardTokenContracts().put("ESSENTIAL_VIDEO_1", "0x-allowed-contract");
        JsonNode data = objectMapper.readTree("""
                {
                  "data": [
                    {
                      "contract": "0x-wrong-contract",
                      "data": {
                        "name": "ESSENTIAL_VIDEO_1",
                        "symbol": "MYT"
                      }
                    },
                    {
                      "contract": "0x-allowed-contract",
                      "data": {
                        "name": "ESSENTIAL_VIDEO_1",
                        "symbol": "MYT"
                      }
                    }
                  ]
                }
                """);
        when(externalTokenClient.getTokenList()).thenReturn(data);

        List<AdminDaeguChainTokenDto.TokenOption> tokenOptions = service.getTokenOptions();

        assertThat(tokenOptions.get(0).getTokenName()).isEqualTo("ESSENTIAL_VIDEO_1");
        assertThat(tokenOptions.get(0).getContractAddress()).isEqualTo("0x-allowed-contract");
    }

    @Test
    void getTokenOptionsFallsBackToRewardTokenNamesWhenExternalTokenServerFails() {
        when(externalTokenClient.getTokenList()).thenThrow(new BadRequestApiException("token server error"));

        List<AdminDaeguChainTokenDto.TokenOption> tokenOptions = service.getTokenOptions();

        assertThat(tokenOptions).hasSize(12);
        assertThat(tokenOptions.get(0).getTokenName()).isEqualTo("ESSENTIAL_VIDEO_1");
        assertThat(tokenOptions.get(0).getContractAddress()).isEqualTo("ESSENTIAL_VIDEO_1");
        assertThat(tokenOptions.get(11).getTokenName()).isEqualTo("OPTIONAL_VIDEO_7");
    }

    @Test
    void getTokenListDoesNotFallbackWhenExternalTokenServerFails() {
        when(externalTokenClient.getTokenList()).thenThrow(new BadRequestApiException("token server error"));

        assertThatThrownBy(() -> service.getTokenList())
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("token server error");
    }

    @Test
    void getTokenListRejectsResponsesWithoutTokenArray() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "message": "ok"
                }
                """);
        when(externalTokenClient.getTokenList()).thenReturn(data);

        assertThatThrownBy(() -> service.getTokenList())
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("token list response does not include token array");
    }

    @Test
    void createTokenCallsExternalTokenServerWithConfiguredSymbol() throws Exception {
        JsonNode responseBody = objectMapper.readTree("""
                {
                  "contract address": "0x-token-contract"
                }
                """);
        when(externalTokenClient.createToken("ESSENTIAL_VIDEO_1", "MYT", 3L))
                .thenReturn(responseBody);

        var response = service.createToken(new AdminDaeguChainTokenDto.CreateRequest("ESSENTIAL_VIDEO_1", 3L));

        assertThat(response.getData().path("contract address").asText()).isEqualTo("0x-token-contract");
        verify(externalTokenClient).createToken("ESSENTIAL_VIDEO_1", "MYT", 3L);
    }

    @Test
    void createTokenRequiresTokenSymbolConfiguration() {
        properties.setTokenSymbol(null);

        assertThatThrownBy(() -> service.createToken(new AdminDaeguChainTokenDto.CreateRequest("ESSENTIAL_VIDEO_1", 1L)))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("daegu-chain.token-symbol is required");
    }

    @Test
    void createTokenRejectsUnsupportedTokenName() {
        assertThatThrownBy(() -> service.createToken(new AdminDaeguChainTokenDto.CreateRequest("UNRELATED_TOKEN", 1L)))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("unsupported reward token name");
    }

    @Test
    void getRewardTransfersReturnsRecentRewardTokenTransactionsWithUserInfo() {
        OralExerciseContent content = OralExerciseContent.builder()
                .oralExerciseContentId(10L)
                .title("1화 인트로")
                .build();
        UserRewardTransaction transaction = UserRewardTransaction.builder()
                .userRewardTransactionId(100L)
                .userId(1L)
                .oralExerciseContent(content)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                .amount(1L)
                .balanceAfter(3L)
                .idempotencyKey("ORAL_EXERCISE_BUTTON:1:OPTIONAL_VIDEO_1")
                .coinId("OPTIONAL_VIDEO_1")
                .tokenContractAddress("0x-token")
                .sessionId("session-1")
                .build();
        transaction.markTokenTransferred("tx-hash", "fact-hash");
        User user = User.builder()
                .userId(1L)
                .userLoginIdentifier("tester")
                .userName("테스터")
                .build();
        when(userRewardTransactionRepository.findRecentByType(
                org.mockito.ArgumentMatchers.eq(UserRewardTransactionType.ORAL_EXERCISE_COIN),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of(transaction));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));

        AdminDaeguChainTokenDto.RewardTransferListResponse response = service.getRewardTransfers();

        assertThat(response.getTransfers()).hasSize(1);
        AdminDaeguChainTokenDto.RewardTransfer transfer = response.getTransfers().get(0);
        assertThat(transfer.getTransactionId()).isEqualTo(100L);
        assertThat(transfer.getUserLoginIdentifier()).isEqualTo("tester");
        assertThat(transfer.getUserName()).isEqualTo("테스터");
        assertThat(transfer.getContentTitle()).isEqualTo("1화 인트로");
        assertThat(transfer.getTokenName()).isEqualTo("OPTIONAL_VIDEO_1");
        assertThat(transfer.getTokenContractAddress()).isEqualTo("0x-token");
        assertThat(transfer.getTxHash()).isEqualTo("tx-hash");
        assertThat(transfer.getFactHash()).isEqualTo("fact-hash");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRewardTransactionRepository).findRecentByType(
                org.mockito.ArgumentMatchers.eq(UserRewardTransactionType.ORAL_EXERCISE_COIN),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    void getRewardTransfersFillsMissingRewardTokenContractAddress() throws Exception {
        UserRewardTransaction transaction = UserRewardTransaction.builder()
                .userRewardTransactionId(100L)
                .userId(1L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                .amount(1L)
                .balanceAfter(1L)
                .idempotencyKey("ORAL_EXERCISE_BUTTON:1:essential_video_5")
                .coinId("essential_video_5")
                .build();
        when(userRewardTransactionRepository.findRecentByType(
                org.mockito.ArgumentMatchers.eq(UserRewardTransactionType.ORAL_EXERCISE_COIN),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of(transaction));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of());
        when(userRewardTransactionRepository.save(transaction)).thenReturn(transaction);
        when(externalTokenClient.getTokenList()).thenReturn(objectMapper.readTree("""
                {
                  "data": [
                    {
                      "contract": "0x-essential-5",
                      "data": { "name": "essential_video_5" }
                    }
                  ]
                }
                """));

        AdminDaeguChainTokenDto.RewardTransferListResponse response = service.getRewardTransfers();

        assertThat(response.getTransfers()).singleElement()
                .satisfies(transfer -> assertThat(transfer.getTokenContractAddress()).isEqualTo("0x-essential-5"));
        assertThat(transaction.getTokenContractAddress()).isEqualTo("0x-essential-5");
        verify(userRewardTransactionRepository).save(transaction);
    }

    @Test
    void getRewardReclaimsReturnsRecentReclaimTransactions() {
        UserRewardTransaction transaction = UserRewardTransaction.builder()
                .userRewardTransactionId(200L)
                .userId(1L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_RECLAIM)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                .amount(1L)
                .balanceAfter(5L)
                .idempotencyKey("ORAL_EXERCISE_RECLAIM:1:100")
                .coinId("essential_video_1")
                .tokenContractAddress("0x-essential-1")
                .build();
        User user = User.builder()
                .userId(1L)
                .userLoginIdentifier("tester")
                .userName("테스터")
                .build();
        when(userRewardTransactionRepository.findRecentByType(
                org.mockito.ArgumentMatchers.eq(UserRewardTransactionType.ORAL_EXERCISE_RECLAIM),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of(transaction));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));

        AdminDaeguChainTokenDto.RewardTransferListResponse response = service.getRewardReclaims();

        assertThat(response.getTransfers()).singleElement()
                .satisfies(reclaim -> {
                    assertThat(reclaim.getTransactionId()).isEqualTo(200L);
                    assertThat(reclaim.getUserLoginIdentifier()).isEqualTo("tester");
                    assertThat(reclaim.getTokenName()).isEqualTo("essential_video_1");
                    assertThat(reclaim.getTokenContractAddress()).isEqualTo("0x-essential-1");
                });
    }
}
