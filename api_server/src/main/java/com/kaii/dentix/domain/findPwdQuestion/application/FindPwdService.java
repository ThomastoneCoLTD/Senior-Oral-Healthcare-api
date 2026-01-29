package com.kaii.dentix.domain.findPwdQuestion.application;

import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.findPwdQuestion.dto.FindPwdQuestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindPwdService {

    private final FindPwdQuestionRepository findPwdQuestionRepository;

    /**
     *  사용자 바밀번호 찾기 질문 리스트
     */
    @Transactional(readOnly = true)
    public FindPwdQuestionDto.Response userFindPwdQuestions() {

        List<FindPwdQuestionDto.Question> questions = findPwdQuestionRepository.findAll(Sort.by(Sort.Direction.ASC, "findPwdQuestionSort"))
                .stream()
                .map(entity -> FindPwdQuestionDto.Question.builder()
                        .id(entity.getFindPwdQuestionId())
                        .sort(entity.getFindPwdQuestionSort())
                        .title(entity.getFindPwdQuestionTitle())
                        .build()
                ).toList();

        return new FindPwdQuestionDto.Response(questions);
    }

}
