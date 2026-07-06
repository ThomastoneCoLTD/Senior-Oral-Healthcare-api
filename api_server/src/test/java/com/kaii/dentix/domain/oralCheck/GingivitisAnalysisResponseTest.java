package com.kaii.dentix.domain.oralCheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.oralCheck.dto.resoponse.GingivitisAnalysisResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GingivitisAnalysisResponseTest {

    @Test
    void mapsSnakeCaseAiResponseFields() throws Exception {
        String responseJson = """
                {
                  "id": 1,
                  "image_name": "gum.jpg",
                  "result_code": 200,
                  "ging_check": {
                    "up_check": 1,
                    "down_check": 0,
                    "all_teeth_check": 12.3
                  }
                }
                """;

        GingivitisAnalysisResponse response = new ObjectMapper()
                .readValue(responseJson, GingivitisAnalysisResponse.class);

        assertThat(response.getResultCode()).isEqualTo(200);
        assertThat(response.getImageName()).isEqualTo("gum.jpg");
        assertThat(response.getGingCheck().getUpCheck()).isEqualTo(1);
        assertThat(response.getGingCheck().getDownCheck()).isZero();
        assertThat(response.getGingCheck().getAllTeethCheck()).isEqualTo(12.3F);
    }
}
