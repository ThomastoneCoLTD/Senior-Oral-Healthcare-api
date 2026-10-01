package com.kaii.dentix.domain.reward.application;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;

public class DeferredRewardClientRequiredException extends BadRequestApiException {
    public DeferredRewardClientRequiredException() {
        super("토큰 지급 화면이 업데이트되었습니다. 페이지를 새로고침한 뒤 다시 눌러주세요.");
    }
}
