package com.together.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * {@code PATCH /missions/{missionId}} (방장 전용). 필드가 모두 선택적이며 보낸 값만 수정된다.
 *
 * <p>{@code isActive=false} 로 바꿔도 기존 수행 기록과 배정은 유지된다(과거 기록 보존).
 */
public record MissionUpdateRequest(
        @Size(max = 200, message = "미션 제목은 200자 이하여야 합니다.")
        String title,

        @Min(value = 100, message = "보상 하한은 100원 이상이어야 합니다.")
        @Max(value = 10000, message = "보상 하한은 10,000원 이하여야 합니다.")
        Integer rewardMin,

        @Min(value = 100, message = "보상 상한은 100원 이상이어야 합니다.")
        @Max(value = 10000, message = "보상 상한은 10,000원 이하여야 합니다.")
        Integer rewardMax,

        @Min(value = 1, message = "최소 글자수는 1자 이상이어야 합니다.")
        Integer minTextLength,

        Boolean isActive
) {
}
