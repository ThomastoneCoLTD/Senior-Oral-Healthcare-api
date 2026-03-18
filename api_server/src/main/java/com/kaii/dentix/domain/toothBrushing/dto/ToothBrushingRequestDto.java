package com.kaii.dentix.domain.toothBrushing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToothBrushingRequestDto {
    @NotBlank(message = "양치 시간은 필수입니다.")
    @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$",
            message = "양치 시간은 HH:mm 또는 HH:mm:ss 형식이어야 합니다."
    )
    private String brushingTime;
}
