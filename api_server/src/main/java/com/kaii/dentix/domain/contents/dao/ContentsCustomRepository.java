package com.kaii.dentix.domain.contents.dao;

import com.kaii.dentix.domain.contents.dto.ContentsDto;

import java.util.List;

public interface ContentsCustomRepository {
    List<ContentsDto.Summary> getContents();

    List<Long> getCustomizedContentsIdList(Long questionnaireId);

    List<ContentsDto.Summary> getCustomizedContents(Long questionnaireId);
}

