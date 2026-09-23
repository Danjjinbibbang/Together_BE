package com.together.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * {@code POST /challenges/{challengeId}/mission-mode/vote} (FR-008 확장, 방장 전용)
 *
 * @param durationHours 마감까지 몇 시간. 생략하면 서버 기본값(app.vote.default-duration-hours)
 */
public record MissionModeVoteOpenRequest(
        @Min(value = 1, message = "투표 기간은 1시간 이상이어야 합니다.")
        @Max(value = 168, message = "투표 기간은 168시간(7일)을 넘을 수 없습니다.")
        Integer durationHours
) {
}
