package com.kaii.dentix.domain.toothBrushing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToothBrushingRequestDto {
    private int brushingRound;
    private LocalDate brushingDate;
}