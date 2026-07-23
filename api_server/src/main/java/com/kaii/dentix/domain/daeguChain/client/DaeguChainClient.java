package com.kaii.dentix.domain.daeguChain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainApiAuditService;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Component
public class DaeguChainClient {

    private final DaeguChainProperties properties;
    private final RestTemplate restTemplate;
    private final DaeguChainApiAuditService auditService;

    public DaeguChainClient(DaeguChainProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this(properties, restTemplateBuilder, null);
    }

    @Autowired
    public DaeguChainClient(
            DaeguChainProperties properties,
            RestTemplateBuilder restTemplateBuilder,
            DaeguChainApiAuditService auditService
    ) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
        this.auditService = auditService;
    }

    public DaeguChainDto.ApiResponse<DaeguChainDto.RpcNodeData> getRpcNode(DaeguChainDto.ChainRequest request) {
        return post(
                "/mitum/com/rpc_node",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<DaeguChainDto.ChainIdData> getChainName(DaeguChainDto.ChainRequest request) {
        return post(
                "/mitum/com/chain_id",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<DaeguChainDto.NodeInfoData> getNodeInfo(DaeguChainDto.TokenChainRequest request) {
        return post(
                "/mitum/com/node_info",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> createAccount(DaeguChainDto.TokenChainRequest request) {
        return post(
                "/mitum/com/acc_create",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> faucet(DaeguChainDto.AccountAddressApiRequest request) {
        return post(
                "/mitum/com/acc_faucet",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getBalance(DaeguChainDto.AccountAddressApiRequest request) {
        return post(
                "/mitum/com/acc_balance",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getInformation(DaeguChainDto.AccountAddressApiRequest request) {
        return post(
                "/mitum/com/acc_info",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> transfer(DaeguChainDto.AccountTransferApiRequest request) {
        return post(
                "/mitum/com/cur_transfer",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getBlockNumber(DaeguChainDto.BlockNumberApiRequest request) {
        return post(
                "/mitum/com/block_number",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getBlockByNumber(DaeguChainDto.BlockByNumberApiRequest request) {
        return post(
                "/mitum/com/block_by_num",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getBlockByHash(DaeguChainDto.BlockByHashApiRequest request) {
        return post(
                "/mitum/com/block_by_hash",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTransactionCount(DaeguChainDto.BlockByNumberApiRequest request) {
        return post(
                "/mitum/com/trx_count",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTransaction(DaeguChainDto.TransactionApiRequest request) {
        return post(
                "/mitum/com/trx_info",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> createToken(DaeguChainDto.TokenCreateApiRequest request) {
        return post(
                "/mitum/token/create",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> uploadToken(String token, String contAddr, MultipartFile tokenFile) {
        try {
            MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
            params.add("token", token);
            params.add("cont_addr", contAddr);
            params.add("token_file", toResource(tokenFile));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<DaeguChainDto.ApiResponse<JsonNode>> response = restTemplate.exchange(
                    getApiUrl("/mitum/upload/upload_token"),
                    HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {}
            );

            return Objects.requireNonNull(response.getBody(), "DaeguChain response body is empty");
        } catch (IOException | RestClientException | NullPointerException exception) {
            throw new BadRequestApiException("DaeguChain API call failed: " + exception.getMessage());
        }
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenList(DaeguChainDto.TokenListApiRequest request) {
        return post(
                "/mitum/token/tokens",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenSupply(DaeguChainDto.TokenContractApiRequest request) {
        return post(
                "/mitum/token/supply",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenBalance(DaeguChainDto.TokenBalanceApiRequest request) {
        return post(
                "/mitum/token/balance",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> mintToken(DaeguChainDto.TokenMintApiRequest request) {
        return post(
                "/mitum/token/mint",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> burnToken(DaeguChainDto.TokenBurnApiRequest request) {
        return post(
                "/mitum/token/burn",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> approveToken(DaeguChainDto.TokenApproveApiRequest request) {
        return post(
                "/mitum/token/approve",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenAllowance(DaeguChainDto.TokenAllowanceApiRequest request) {
        return post(
                "/mitum/token/allowance",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> transferToken(DaeguChainDto.TokenTransferApiRequest request) {
        return post(
                "/mitum/token/transfer",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> transferTokenFrom(DaeguChainDto.TokenTransferFromApiRequest request) {
        return post(
                "/mitum/token/transfer_from",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> createPoint(DaeguChainDto.TokenCreateApiRequest request) {
        return post(
                "/mitum/point/create",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getPointList(DaeguChainDto.TokenListApiRequest request) {
        return post(
                "/mitum/point/points",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getPointBalance(DaeguChainDto.TokenBalanceApiRequest request) {
        return post(
                "/mitum/point/balance",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> mintPoint(DaeguChainDto.TokenMintApiRequest request) {
        return post(
                "/mitum/point/mint",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> transferPoint(DaeguChainDto.TokenTransferApiRequest request) {
        return post(
                "/mitum/point/transfer",
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> postNft(String path, Map<String, Object> request) {
        return post(
                path,
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> uploadNft(String token, String description, MultipartFile nftFile) {
        try {
            MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
            params.add("token", token);
            params.add("description", description);
            params.add("nft_file", toResource(nftFile));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<DaeguChainDto.ApiResponse<JsonNode>> response = restTemplate.exchange(
                    getApiUrl("/mitum/upload/upload_nft"),
                    HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {}
            );

            return Objects.requireNonNull(response.getBody(), "DaeguChain response body is empty");
        } catch (IOException | RestClientException | NullPointerException exception) {
            throw new BadRequestApiException("DaeguChain API call failed: " + exception.getMessage());
        }
    }

    public DaeguChainDto.ApiResponse<JsonNode> postDid(String path, Map<String, Object> request) {
        return post(
                path,
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> postTimestamp(String path, Map<String, Object> request) {
        return post(
                path,
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    private <T> DaeguChainDto.ApiResponse<T> post(
            String path,
            Object request,
            ParameterizedTypeReference<DaeguChainDto.ApiResponse<T>> responseType
    ) {
        String api = getApiUrl(path);
        try {
            ResponseEntity<DaeguChainDto.ApiResponse<T>> response = restTemplate.exchange(
                    api,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    responseType
            );

            DaeguChainDto.ApiResponse<T> responseBody =
                    Objects.requireNonNull(response.getBody(), "DaeguChain response body is empty");
            record(api, request, responseBody, true);
            return responseBody;
        } catch (RestClientException | NullPointerException exception) {
            BadRequestApiException apiException =
                    new BadRequestApiException("DaeguChain API call failed: " + exception.getMessage());
            recordFailure(api, request, apiException);
            throw apiException;
        }
    }

    private void record(String api, Object request, Object response, boolean success) {
        if (auditService != null) {
            auditService.record(api, request, response, success);
        }
    }

    private void recordFailure(String api, Object request, RuntimeException exception) {
        if (auditService != null) {
            auditService.recordFailure(api, request, exception);
        }
    }

    private String getApiUrl(String path) {
        return UriComponentsBuilder
                .fromHttpUrl(trimTrailingSlash(resolveApiBaseUrl()))
                .path(normalizePath(path))
                .toUriString();
    }

    private String resolveApiBaseUrl() {
        if (hasText(properties.getApiBaseUrl())) {
            return properties.getApiBaseUrl();
        }
        return trimTrailingSlash(properties.getBaseUrl()) + "/" + trimSlashes(properties.getApiVersion());
    }

    private String normalizePath(String path) {
        String normalizedPath = path == null || path.isBlank() ? "" : path;
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (apiBaseUrlEndsWithMitum() && normalizedPath.startsWith("/mitum/")) {
            return normalizedPath.substring("/mitum".length());
        }
        return normalizedPath;
    }

    private boolean apiBaseUrlEndsWithMitum() {
        String apiBaseUrl = trimTrailingSlash(resolveApiBaseUrl()).toLowerCase();
        return apiBaseUrl.endsWith("/mitum");
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ByteArrayResource toResource(MultipartFile file) throws IOException {
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
    }
}
