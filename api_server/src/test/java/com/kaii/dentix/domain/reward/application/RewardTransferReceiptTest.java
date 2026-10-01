package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RewardTransferReceiptTest {
    ObjectMapper mapper = new ObjectMapper();
    String hash = "a".repeat(64);
    ObjectNode receipt() throws Exception {
        return (ObjectNode) mapper.readTree("""
          {"height":10,"in_state":true,"operation":{"fact":{"hash":"%s","sender":"owner",
           "items":[{"receiver":"wallet","contract":"contract","amount":"1"}]}}}
          """.formatted(hash));
    }
    RewardTransferReceipt.Result verify(ObjectNode node) {
        return RewardTransferReceipt.verify(node,hash,"owner","wallet","contract",1);
    }
    @Test void onlyAnExactCommittedOperationIsAccepted() throws Exception {
        var node=receipt();assertThat(verify(node)).isEqualTo(RewardTransferReceipt.Result.RECEIVED);
        node.put("in_state",false);assertThat(verify(node)).isEqualTo(RewardTransferReceipt.Result.REJECTED);
    }
    @Test void lookupSuccessAndBalanceAreNotReceipts() throws Exception {
        for(String json : new String[]{"{\"balance\":0}","{\"balance\":1}","{\"state\":\"OK\"}","{}"})
            assertThat(verify((ObjectNode)mapper.readTree(json))).isEqualTo(RewardTransferReceipt.Result.UNKNOWN);
    }
    @Test void productionProxyReceiptWithFlatContractTokenFactIsVerified() throws Exception {
        var node=(ObjectNode)mapper.readTree("""
                {"receipt":{"height":100,"in_state":true,"operation":{"fact":{"hash":"%s","sender":"owner",
                "receiver":"wallet","contract":"contract","amount":"1"}}}}
                """.formatted(hash));
        assertThat(verify(node)).isEqualTo(RewardTransferReceipt.Result.RECEIVED);
        ((ObjectNode)node.path("receipt")).put("in_state",false);
        assertThat(verify(node)).isEqualTo(RewardTransferReceipt.Result.REJECTED);
        var lookup=mapper.createObjectNode().set("trx_info",node.path("receipt"));
        ((ObjectNode)lookup.path("trx_info")).put("in_state",true);
        assertThat(verify((ObjectNode)lookup)).isEqualTo(RewardTransferReceipt.Result.RECEIVED);
    }
    @Test void wrongHashSenderReceiverContractAmountOrMissingHeightCannotConfirm() throws Exception {
        for(String key : new String[]{"hash","sender","receiver","contract","amount"}) {
            var node=receipt();var fact=(ObjectNode)node.path("operation").path("fact");
            var target=key.equals("hash")||key.equals("sender")?fact:(ObjectNode)fact.path("items").get(0);
            target.put(key,"wrong");assertThat(verify(node)).isEqualTo(RewardTransferReceipt.Result.UNKNOWN);
        }
        var node=receipt();node.remove("height");assertThat(verify(node)).isEqualTo(RewardTransferReceipt.Result.UNKNOWN);
    }
}
