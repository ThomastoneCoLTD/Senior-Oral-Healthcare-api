package com.kaii.dentix.domain.toothBrushing;

import com.kaii.dentix.domain.toothBrushing.application.ToothBrushingService;
import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingRepository;
import com.kaii.dentix.domain.toothBrushing.domain.ToothBrushing;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingRegisterDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingRequestDto;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ToothBrushingServiceTest {

    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");

    @Mock private UserService userService;
    @Mock private ToothBrushingRepository toothBrushingRepository;

    @InjectMocks
    private ToothBrushingService toothBrushingService;

    @Test
    void recordToothBrushing_allowsEarlierTimeWhenItDoesNotOverlapAnotherRecord() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = User.builder().userId(1L).build();
        ToothBrushingRequestDto requestDto = ToothBrushingRequestDto.builder()
                .brushingTime("09:30")
                .build();

        ToothBrushing noonBrushing = brushing(1L, todayAt(12, 0));
        ToothBrushing afternoonBrushing = brushing(2L, todayAt(15, 0));
        ToothBrushing insertedBrushing = brushing(3L, todayAt(9, 30));

        given(userService.getTokenUser(request)).willReturn(user);
        given(toothBrushingRepository.findByUserIdAndCreatedOrderByCreated(1L, LocalDate.now(ASIA_SEOUL).toString()))
                .willReturn(List.of(noonBrushing, afternoonBrushing))
                .willReturn(List.of(insertedBrushing, noonBrushing, afternoonBrushing));
        given(toothBrushingRepository.save(any(ToothBrushing.class))).willReturn(insertedBrushing);

        ToothBrushingRegisterDto response = toothBrushingService.recordToothBrushing(request, requestDto);

        assertThat(response.getTimeInterval()).isNull();
        assertThat(response.getToothBrushingList()).hasSize(3);
        verify(toothBrushingRepository).updateCreated(3L, todayAt(9, 30), todayAt(9, 30));
    }

    @Test
    void recordToothBrushing_returnsTimeIntervalWhenEarlierTimeOverlapsAnotherRecord() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = User.builder().userId(1L).build();
        ToothBrushingRequestDto requestDto = ToothBrushingRequestDto.builder()
                .brushingTime("09:30")
                .build();

        ToothBrushing existingBrushing = brushing(1L, todayAt(10, 0));

        given(userService.getTokenUser(request)).willReturn(user);
        given(toothBrushingRepository.findByUserIdAndCreatedOrderByCreated(1L, LocalDate.now(ASIA_SEOUL).toString()))
                .willReturn(List.of(existingBrushing));

        ToothBrushingRegisterDto response = toothBrushingService.recordToothBrushing(request, requestDto);

        assertThat(response.getToothBrushingList()).hasSize(1);
        assertThat(response.getTimeInterval()).isEqualTo(1800L);
    }

    private ToothBrushing brushing(Long id, Date created) {
        ToothBrushing toothBrushing = ToothBrushing.builder()
                .toothBrushingId(id)
                .userId(1L)
                .build();
        toothBrushing.setCreated(created);
        return toothBrushing;
    }

    private Date todayAt(int hour, int minute) {
        LocalDateTime dateTime = LocalDateTime.of(LocalDate.now(ASIA_SEOUL), LocalTime.of(hour, minute));
        return Date.from(dateTime.atZone(ASIA_SEOUL).toInstant());
    }
}
