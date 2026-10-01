package com.kaii.dentix.domain.daeguChain.client;

import com.kaii.dentix.global.common.error.exception.BadRequestApiException;

/** Never infer non-submission from an HTTP error, read timeout or missing receipt. */
public class TokenTransferException extends BadRequestApiException {
    private final boolean notSubmitted;
    private final String factHash;

    public TokenTransferException(boolean notSubmitted, String factHash) {
        super("토큰 전송 결과를 확인하고 있습니다.");
        this.notSubmitted = notSubmitted;
        this.factHash = factHash;
    }
    public boolean isNotSubmitted() { return notSubmitted; }
    public String getFactHash() { return factHash; }
}
