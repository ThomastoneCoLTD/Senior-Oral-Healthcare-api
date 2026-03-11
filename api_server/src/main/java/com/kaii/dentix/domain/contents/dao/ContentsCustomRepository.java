package com.kaii.dentix.domain.contents.dao;

import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.type.OralSectionType;

import java.util.List;

public interface ContentsCustomRepository {
    List<ContentsDto.Summary> getContents();

    List<Long> getCustomizedContentsIdList(Long questionnaireId);

    List<ContentsDto.Summary> getCustomizedContents(Long questionnaireId);

    List<Long> getCustomizedContentsIdListByOralCheck(Long oralCheckId);
    List<ContentsDto.Summary> getCustomizedContents(OralSectionType sectionType, Long sectionId);
}

