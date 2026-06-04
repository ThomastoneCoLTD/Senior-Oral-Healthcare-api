package com.kaii.dentix.domain.daeguChain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

public class DaeguChainDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChainRequest {
        private String chain;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenChainRequest {
        private String token;
        private String chain;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiResponse<T> {
        private String state;
        private Map<String, Object> rcode;
        private String msg;
        private T data;
        private String cid;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RpcNodeData {
        @JsonProperty("rpc_node")
        private String rpcNode;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChainIdData {
        @JsonProperty("chain_id")
        private String chainId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeInfoData {
        @JsonProperty("node_info")
        private JsonNode nodeInfo;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicRequest {
        private String chain;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeInfoRequest {
        private String chain;
        private String token;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyPairData {
        @JsonProperty("key_pair")
        private KeyPair keyPair;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyPair {
        private String privatekey;
        private String publickey;
        private String address;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountCreateRequest {
        private String chain;
        private String token;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountAddressRequest {
        private String chain;
        private String token;

        @NotBlank(message = "address is required")
        private String address;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountTransferRequest {
        private String chain;
        private String token;

        @NotBlank(message = "privateKey is required")
        @JsonProperty("private_key")
        private String privateKey;

        @NotBlank(message = "sender is required")
        private String sender;

        @NotBlank(message = "receiver is required")
        private String receiver;

        @NotBlank(message = "amount is required")
        private String amount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountAddressApiRequest {
        private String chain;
        private String token;
        private String address;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountTransferApiRequest {
        private String chain;
        private String token;

        @JsonProperty("private_key")
        private String privateKey;

        private String sender;
        private String receiver;
        private String amount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockNumberRequest {
        private String chain;
        private String token;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockNumberApiRequest {
        private String chain;
        private String token;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockByNumberRequest {
        private String chain;
        private String token;

        @NotBlank(message = "blockNum is required")
        @JsonProperty("block_num")
        private String blockNum;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockByNumberApiRequest {
        private String chain;
        private String token;

        @JsonProperty("block_num")
        private String blockNum;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockByHashRequest {
        private String chain;
        private String token;

        @NotBlank(message = "blockHash is required")
        @JsonProperty("block_hash")
        private String blockHash;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockByHashApiRequest {
        private String chain;
        private String token;

        @JsonProperty("block_hash")
        private String blockHash;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionRequest {
        private String chain;
        private String token;

        @NotBlank(message = "factHash is required")
        @JsonProperty("fact_hash")
        private String factHash;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionApiRequest {
        private String chain;
        private String token;

        @JsonProperty("fact_hash")
        private String factHash;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenCreateRequest {
        private String token;
        private String chain;

        @NotBlank(message = "chainName is required")
        @JsonProperty("chain_name")
        private String chainName;

        @NotBlank(message = "ownerAddr is required")
        @JsonProperty("owner_addr")
        private String ownerAddr;

        @NotBlank(message = "ownerPkey is required")
        @JsonProperty("owner_pkey")
        private String ownerPkey;

        @NotBlank(message = "tokenName is required")
        @JsonProperty("token_name")
        private String tokenName;

        @NotBlank(message = "tokenSymbol is required")
        @JsonProperty("token_symbol")
        private String tokenSymbol;

        @NotNull(message = "decimals is required")
        private Integer decimals;

        @NotNull(message = "supply is required")
        private Long supply;

        private Boolean mintable;
        private Boolean lockable;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenCreateApiRequest {
        private String token;
        private String chain;

        @JsonProperty("chain_name")
        private String chainName;

        @JsonProperty("owner_addr")
        private String ownerAddr;

        @JsonProperty("owner_pkey")
        private String ownerPkey;

        @JsonProperty("token_name")
        private String tokenName;

        @JsonProperty("token_symbol")
        private String tokenSymbol;

        private Integer decimals;
        private Long supply;
        private Boolean mintable;
        private Boolean lockable;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenListRequest {
        private String token;
        private String chain;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenListApiRequest {
        private String token;
        private String chain;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenContractRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenContractApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenBalanceRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;

        @NotBlank(message = "addr is required")
        private String addr;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenBalanceApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;

        private String addr;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenMintRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;

        @NotBlank(message = "owner is required")
        private String owner;

        @NotBlank(message = "ownerPkey is required")
        @JsonProperty("owner_pkey")
        private String ownerPkey;

        @NotBlank(message = "receiver is required")
        private String receiver;

        @NotBlank(message = "amount is required")
        private String amount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenMintApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;

        private String owner;

        @JsonProperty("owner_pkey")
        private String ownerPkey;

        private String receiver;
        private String amount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenBurnRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;

        @NotBlank(message = "holder is required")
        private String holder;

        @NotBlank(message = "holderPkey is required")
        @JsonProperty("holder_pkey")
        private String holderPkey;

        @NotBlank(message = "amount is required")
        private String amount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenBurnApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;

        private String holder;

        @JsonProperty("holder_pkey")
        private String holderPkey;

        private String amount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenApproveRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;

        @NotBlank(message = "holder is required")
        private String holder;

        @NotBlank(message = "holderPkey is required")
        @JsonProperty("holder_pkey")
        private String holderPkey;

        @NotBlank(message = "approved is required")
        private String approved;

        @NotBlank(message = "amount is required")
        private String amount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenApproveApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;

        private String holder;

        @JsonProperty("holder_pkey")
        private String holderPkey;

        private String approved;
        private String amount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenAllowanceRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;

        @NotBlank(message = "holder is required")
        private String holder;

        @NotBlank(message = "spender is required")
        private String spender;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenAllowanceApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;

        private String holder;
        private String spender;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenTransferRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;

        @NotBlank(message = "sender is required")
        private String sender;

        @NotBlank(message = "senderPkey is required")
        @JsonProperty("sender_pkey")
        private String senderPkey;

        @NotBlank(message = "receiver is required")
        private String receiver;

        @NotBlank(message = "amount is required")
        private String amount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenTransferApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;

        private String sender;

        @JsonProperty("sender_pkey")
        private String senderPkey;

        private String receiver;
        private String amount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenTransferFromRequest {
        private String token;
        private String chain;

        @NotBlank(message = "contAddr is required")
        @JsonProperty("cont_addr")
        private String contAddr;

        @NotBlank(message = "sender is required")
        private String sender;

        @NotBlank(message = "senderPkey is required")
        @JsonProperty("sender_pkey")
        private String senderPkey;

        @NotBlank(message = "holder is required")
        private String holder;

        @NotBlank(message = "receiver is required")
        private String receiver;

        @NotBlank(message = "amount is required")
        private String amount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenTransferFromApiRequest {
        private String token;
        private String chain;

        @JsonProperty("cont_addr")
        private String contAddr;

        private String sender;

        @JsonProperty("sender_pkey")
        private String senderPkey;

        private String holder;
        private String receiver;
        private String amount;
    }
}
