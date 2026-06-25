package com.kaii.dentix.domain.admin.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainToken20Service;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDaeguChainTokenService {

    private final DaeguChainToken20Service daeguChainToken20Service;
    private final DaeguChainProperties properties;

    public List<String> getTokenNames() {
        DaeguChainDto.ApiResponse<JsonNode> response =
                daeguChainToken20Service.getTokenList(new DaeguChainDto.TokenListRequest(null, null));

        List<String> tokenNames = new ArrayList<>();
        JsonNode data = response == null ? null : response.getData();
        if (data == null || !data.isArray()) {
            return tokenNames;
        }

        for (JsonNode token : data) {
            JsonNode name = token.path("data").path("name");
            if (!name.isMissingNode() && !name.asText().isBlank()) {
                tokenNames.add(name.asText());
            }
        }
        return tokenNames;
    }

    public DaeguChainDto.ApiResponse<JsonNode> createToken(AdminDaeguChainTokenDto.CreateRequest request) {
        validateTokenCreateConfiguration();
        return daeguChainToken20Service.createToken(new DaeguChainDto.TokenCreateRequest(
                null,
                null,
                null,
                properties.getTokenOwnerAddress(),
                properties.getTokenOwnerPrivateKey(),
                request.getTokenName(),
                properties.getTokenSymbol(),
                properties.getTokenDecimals(),
                request.getSupply(),
                properties.getTokenMintable(),
                properties.getTokenLockable()
        ));
    }

    private void validateTokenCreateConfiguration() {
        if (isBlank(properties.getTokenOwnerAddress())) {
            throw new BadRequestApiException("daegu-chain.token-owner-address is required");
        }
        if (isBlank(properties.getTokenOwnerPrivateKey())) {
            throw new BadRequestApiException("daegu-chain.token-owner-private-key is required");
        }
        if (isBlank(properties.getTokenSymbol())) {
            throw new BadRequestApiException("daegu-chain.token-symbol is required");
        }
        if (properties.getTokenDecimals() == null) {
            throw new BadRequestApiException("daegu-chain.token-decimals is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
