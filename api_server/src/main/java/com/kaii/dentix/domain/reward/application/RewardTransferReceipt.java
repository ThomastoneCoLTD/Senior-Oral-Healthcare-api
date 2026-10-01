package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/** Fail closed: a successful lookup or an empty balance alone is not a transfer receipt. */
public final class RewardTransferReceipt {
    private RewardTransferReceipt() {}
    public enum Result { RECEIVED, REJECTED, UNKNOWN }

    public static Result verify(JsonNode data, String factHash, String owner, String receiver, String contract, long amount) {
        if (data == null || factHash == null || owner == null || receiver == null || contract == null) return Result.UNKNOWN;
        // The production token proxy wraps the Mitum receipt under data.receipt.
        if (data.has("receipt")) data = data.path("receipt");
        else if (data.has("trx_info")) data = data.path("trx_info");
        JsonNode operation = data.has("operation") ? data.path("operation") : data;
        JsonNode fact = operation.path("fact");
        // Bind the receipt to this exact contract, wallet, sender, amount and submitted fact.
        if (!factHash.equals(fact.path("hash").asText()) || !owner.equals(fact.path("sender").asText())) return Result.UNKNOWN;
        JsonNode items = fact.path("items");
        if (items.isArray() && items.size() != 1) return Result.UNKNOWN;
        // Mitum contract token transfers store receiver/contract/amount directly on the fact.
        JsonNode item = items.isArray() ? items.get(0) : fact;
        if (!receiver.equals(item.path("receiver").asText()) || !contract.equals(item.path("contract").asText())) return Result.UNKNOWN;
        try {
            if (new BigDecimal(item.path("amount").asText()).compareTo(BigDecimal.valueOf(amount)) != 0) return Result.UNKNOWN;
        } catch (NumberFormatException ignored) { return Result.UNKNOWN; }
        if (!data.path("height").isIntegralNumber() || data.path("height").asLong() < 0 || !data.path("in_state").isBoolean()) return Result.UNKNOWN;
        return data.path("in_state").asBoolean() ? Result.RECEIVED : Result.REJECTED;
    }
}
