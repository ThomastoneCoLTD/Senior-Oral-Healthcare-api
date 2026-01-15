package com.kaii.dentix.domain.oralCheck.dao;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;

public interface OralCheckCustomRepository {

    OralCheckResultTypeCount userOralCheckList(AdminStatisticDto.SearchRequest request);

    Integer allUserOralCheckCount(AdminStatisticDto.SearchRequest request);

}
