package com.kaii.dentix.domain.user.scheduler;

import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.toothBrushing.dao.ToothBrushingRepository;
import com.kaii.dentix.domain.toothBrushing.domain.ToothBrushing;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.userOralStatus.dao.UserOralStatusRepository;
import com.kaii.dentix.domain.userOralStatus.domain.UserOralStatus;
import com.kaii.dentix.global.common.util.DateFormatUtil;
import com.kaii.dentix.global.common.util.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserScheduler {

    @Value("${spring.profiles.active}")
    private String active;

    private final UserRepository userRepository;
    private final OralCheckRepository oralCheckRepository;
    private final UserOralStatusRepository userOralStatusRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final ToothBrushingRepository toothBrushingRepository;

    private final OralCheckService oralCheckService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul") // 매일 자정
    public void setTestDataForAppStoreReview() throws ParseException {
        String loginIdentifier = active.equals("prod") ? "dtx1234" : "dentix123";

        User user = userRepository.findByUserLoginIdentifier(loginIdentifier).orElse(null);
        if (user == null) return;

        List<OralCheck> oralCheckList = oralCheckRepository.findAllByUser_UserIdOrderByCreatedDesc(user.getUserId());
        List<ToothBrushing> toothBrushingList = toothBrushingRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
        List<Questionnaire> questionnaireList = questionnaireRepository.findAllByUserIdOrderByCreatedDesc(user.getUserId());
        List<UserOralStatus> userOralStatusList = userOralStatusRepository.findAllByQuestionnaireIn(questionnaireList);

        toothBrushingRepository.deleteAllInBatch(toothBrushingList);
        userOralStatusRepository.deleteAllInBatch(userOralStatusList);
        questionnaireRepository.deleteAllInBatch(questionnaireList);
        oralCheckRepository.deleteAllInBatch(oralCheckList);

        final String datePattern = "yyyy-MM-dd";

        Calendar calendar = Calendar.getInstance();

        Date today = calendar.getTime();
        String todayString = DateFormatUtil.dateToString(datePattern, today);

        calendar.setTime(DateFormatUtil.stringToDate("yyyy-MM-dd", todayString)); // 시각 삭제
        calendar.add(Calendar.DATE, -30); // 30일 전 기준
        calendar.add(Calendar.DATE, 1 - calendar.get(Calendar.DAY_OF_WEEK)); // 30일 전날이 포함된 일요일부터 시작

        Date startDate = calendar.getTime();

        Random random = new Random();

        for (int i = 0;; i++) {
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, i);

            Date targetDate = calendar.getTime();
            if (DateFormatUtil.dateToString(datePattern, targetDate).compareTo(todayString) >= 0) break;

            for (int j = 0; j < random.nextInt(4); j++) { // 0 ~ 3
                calendar.setTime(targetDate);
                calendar.add(Calendar.SECOND, random.nextInt(24 * 60 * 60));

                toothBrushingRepository.nativeInsert(user.getUserId(), calendar.getTime());
            }

            for (int j = 0; j < random.nextInt(4); j++) { // 0 ~ 3
                calendar.setTime(targetDate);
                calendar.add(Calendar.SECOND, random.nextInt(24 * 60 * 60));

                Questionnaire questionnaire = questionnaireRepository.save(Questionnaire.builder()
                    .userId(user.getUserId())
                    .questionnaireVersion("v1")
                    .form("{}")
                    .build());

                questionnaireRepository.nativeUpdate(questionnaire.getQuestionnaireId(), calendar.getTime());

                String[] chars = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};
                List<String> typeList = new ArrayList<>();
                for (int k = 0; k < random.nextInt(2) + 1; k++) { // 1 ~ 2
                    int randomIndex = random.nextInt(chars.length);
                    if (typeList.stream().anyMatch(type -> type.equals(chars[randomIndex]))) {
                        k--; continue;
                    }
                    typeList.add(chars[randomIndex]);
                }

                typeList.forEach(type -> userOralStatusRepository.nativeInsert(questionnaire.getQuestionnaireId(), type, calendar.getTime()));
            }

            for (int j = 0; j < random.nextInt(4); j++) { // 0 ~ 3
                calendar.setTime(targetDate);
                calendar.add(Calendar.SECOND, random.nextInt(24 * 60 * 60));

                Float upRightRange;
                Float upLeftRange;
                Float downRightRange;
                Float downLeftRange;

                int v = random.nextInt(4);
                switch (v) {
                    case 0:
                        upRightRange = 0F;
                        upLeftRange = 0F;
                        downRightRange = 0F;
                        downLeftRange = 0F;
                        break;
                    case 1: // 1 ~ 9
                        upRightRange = random.nextInt(90) / 10F + 1;
                        upLeftRange = random.nextInt(90) / 10F + 1;
                        downRightRange = random.nextInt(90) / 10F + 1;
                        downLeftRange = random.nextInt(90) / 10F + 1;
                        break;
                    case 2: // 10 ~ 29
                        upRightRange = random.nextInt(200) / 10F + 10;
                        upLeftRange = random.nextInt(200) / 10F + 10;
                        downRightRange = random.nextInt(200) / 10F + 10;
                        downLeftRange = random.nextInt(200) / 10F + 10;
                        break;
                    case 3: // 30 ~ 100
                        upRightRange = random.nextInt(700) / 10F + 30;
                        upLeftRange = random.nextInt(700) / 10F + 30;
                        downRightRange = random.nextInt(700) / 10F + 30;
                        downLeftRange = random.nextInt(700) / 10F + 30;
                        break;
                    default:
                        return;
                }

                Float totalGroupRatio = (upRightRange + upLeftRange + downRightRange + downLeftRange) / 4;
                Float totalRange = Utils.getDeleteDecimalValue(totalGroupRatio, 1); // 전체 비율

                // 점수 유형
                OralCheckResultType upRightScoreType = oralCheckService.calcDivisionScoreType(upRightRange);
                OralCheckResultType upLeftScoreType = oralCheckService.calcDivisionScoreType(upLeftRange);
                OralCheckResultType downRightScoreType = oralCheckService.calcDivisionScoreType(downRightRange);
                OralCheckResultType downLeftScoreType = oralCheckService.calcDivisionScoreType(downLeftRange);
                OralCheckResultType resultTotalType = oralCheckService.calcDivisionScoreType(totalRange);

                // insert 데이터 set
                OralCheck oralCheck = OralCheck.builder()
                    .user(user)
                    .oralCheckPicturePath("testData")
                    .oralCheckAnalysisState(OralCheckAnalysisState.SUCCESS)
                    .oralCheckTotalRange(totalRange)
                    .oralCheckUpRightRange(upRightRange)
                    .oralCheckUpLeftRange(upLeftRange)
                    .oralCheckDownRightRange(downRightRange)
                    .oralCheckDownLeftRange(downLeftRange)
                    .oralCheckResultJsonData("{}")
                    .oralCheckResultTotalType(resultTotalType)
                    .oralCheckUpRightScoreType(upRightScoreType)
                    .oralCheckUpLeftScoreType(upLeftScoreType)
                    .oralCheckDownRightScoreType(downRightScoreType)
                    .oralCheckDownLeftScoreType(downLeftScoreType)
                    .build();

                oralCheckRepository.nativeInsert(oralCheck, calendar.getTime());
            }
        }
    }
}
