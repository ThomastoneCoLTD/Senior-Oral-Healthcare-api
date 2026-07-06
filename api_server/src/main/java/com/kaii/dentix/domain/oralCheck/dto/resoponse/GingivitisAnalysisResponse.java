package com.kaii.dentix.domain.oralCheck.dto.resoponse;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GingivitisAnalysisResponse {

    private Long id;

    @JsonProperty("image_name")
    private String imageName;

    @JsonAlias({"result_code", "status_code", "statusCode"})
    private Integer resultCode;

    private Long oralCheckId;

    @JsonProperty("ging_check")
    private GingivitisCheck gingCheck;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GingivitisCheck {
        @JsonProperty("up_check")
        private Integer upCheck;

        @JsonProperty("down_check")
        private Integer downCheck;

        @JsonProperty("all_teeth_check")
        private Float allTeethCheck;
    }
}
