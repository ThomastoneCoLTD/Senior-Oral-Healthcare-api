package com.kaii.dentix.domain.toothBrushing.application;

import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingRepository;
import com.kaii.dentix.domain.toothBrushing.domain.ToothBrushing;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingRegisterDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingRequestDto;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToothBrushingService {
    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter BRUSHING_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter BRUSHING_TIME_WITH_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final UserService userService;

    private final ToothBrushingRepository toothBrushingRepository;

    /**
     *  양치질 기록
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = "dashboard",
            key = "@userService.getTokenUser(#httpServletRequest).getUserId() + '_' + T(java.time.LocalDate).now()"
    )
    public ToothBrushingRegisterDto toothBrushing(HttpServletRequest httpServletRequest){
        return saveToothBrushing(httpServletRequest, null);
    }

    /**
     * 시간 입력 양치질 기록
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = "dashboard",
            key = "@userService.getTokenUser(#httpServletRequest).getUserId() + '_' + T(java.time.LocalDate).now()"
    )
    public ToothBrushingRegisterDto recordToothBrushing(HttpServletRequest httpServletRequest, ToothBrushingRequestDto requestDto) {
        return saveToothBrushing(httpServletRequest, requestDto);
    }

    private ToothBrushingRegisterDto saveToothBrushing(HttpServletRequest httpServletRequest, ToothBrushingRequestDto requestDto) {
        User user = userService.getTokenUser(httpServletRequest);

        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        Date brushingCreated = resolveBrushingCreated(requestDto, now);

        if (brushingCreated.after(now)) {
            throw new BadRequestApiException("미래 시간으로는 양치 기록을 저장할 수 없어요.");
        }

        List<ToothBrushing> toothBrushingList = toothBrushingRepository.findByUserIdAndCreatedOrderByCreated(
                user.getUserId(),
                DateFormatUtil.dateToString("yyyy-MM-dd", brushingCreated)
        );

        if (toothBrushingList.size() >= 3) {
            throw new BadRequestApiException("오늘의 양치는 이미 완료했어요.");
        }

        if (toothBrushingList.size() > 0) {
            // 입력 시각이 마지막 기록보다 이르더라도, 같은 날 다른 기록과 1시간 이상 차이 나면 저장 허용
            long nearestSecondsGap = toothBrushingList.stream()
                    .mapToLong(toothBrushing ->
                            Math.abs(brushingCreated.getTime() - toothBrushing.getCreated().getTime()) / 1000
                    )
                    .min()
                    .orElse(Long.MAX_VALUE);

            if (nearestSecondsGap < 3600) {
                return ToothBrushingRegisterDto.builder()
                        .toothBrushingList(toothBrushingList.stream()
                                .map(t -> new ToothBrushingDto(t.getToothBrushingId(), t.getCreated()))
                                .toList())
                        .timeInterval(3600 - nearestSecondsGap)
                        .build();
            }
        }

        ToothBrushing toothBrushing = toothBrushingRepository.save(ToothBrushing.builder()
                .userId(user.getUserId())
                .build());

        if (requestDto != null && requestDto.getBrushingTime() != null && !requestDto.getBrushingTime().isBlank()) {
            toothBrushingRepository.updateCreated(toothBrushing.getToothBrushingId(), brushingCreated, brushingCreated);
        }

        toothBrushingList = toothBrushingRepository.findByUserIdAndCreatedOrderByCreated(
                user.getUserId(),
                DateFormatUtil.dateToString("yyyy-MM-dd", brushingCreated)
        );

        return ToothBrushingRegisterDto.builder()
                .toothBrushingList(toothBrushingList.stream()
                        .map(t -> new ToothBrushingDto(t.getToothBrushingId(), t.getCreated()))
                        .toList())
                .build();
    }

    private Date resolveBrushingCreated(ToothBrushingRequestDto requestDto, Date now) {
        if (requestDto == null || requestDto.getBrushingTime() == null || requestDto.getBrushingTime().isBlank()) {
            return now;
        }

        LocalTime brushingTime = parseBrushingTime(requestDto.getBrushingTime());
        LocalDate today = LocalDate.now(ASIA_SEOUL);
        LocalDateTime brushingDateTime = LocalDateTime.of(today, brushingTime);
        return Date.from(brushingDateTime.atZone(ASIA_SEOUL).toInstant());
    }

    private LocalTime parseBrushingTime(String brushingTime) {
        try {
            if (brushingTime.length() == 5) {
                return LocalTime.parse(brushingTime, BRUSHING_TIME_FORMATTER);
            }
            return LocalTime.parse(brushingTime, BRUSHING_TIME_WITH_SECONDS_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BadRequestApiException("양치 시간은 HH:mm 또는 HH:mm:ss 형식이어야 합니다.");
        }
    }

    @Transactional(readOnly = true)
    public ToothBrushingRegisterDto getToothBrushingByDate(HttpServletRequest request, String dateString) {
        User user = userService.getTokenUser(request);

        Calendar calendar = Calendar.getInstance();
        Date date;
        try {
            if (dateString == null || dateString.isEmpty()) {
                date = calendar.getTime();
            } else {
                date = DateFormatUtil.stringToDate("yyyy-MM-dd", dateString);
            }
        } catch (Exception e) {
            date = calendar.getTime();
        }

        // dto가 아니라 date 사용
        List<ToothBrushing> toothBrushingList =
                toothBrushingRepository.findByUserIdAndCreatedStartingWithOrderByCreated(
                        user.getUserId(),
                        DateFormatUtil.dateToString("yyyy-MM-dd", date)
                );

        Long timeInterval = 0L;
        if (!toothBrushingList.isEmpty()) {
            Date lastCreated = toothBrushingList.get(toothBrushingList.size() - 1).getCreated();
            long diff = (calendar.getTime().getTime() - lastCreated.getTime()) / 1000;
            timeInterval = diff;
        }

        return ToothBrushingRegisterDto.builder()
                .toothBrushingList(
                        toothBrushingList.stream()
                                .map(t -> new ToothBrushingDto(t.getToothBrushingId(), t.getCreated()))
                                .toList()
                )
                .timeInterval(timeInterval)
                .build();
    }

}
