package com.together.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code PATCH /challenges/{challengeId}} (FR-036). 필드가 모두 선택적이며 보낸 값만 수정된다.
 *
 * <p>null 을 "안 보냄"으로 해석하므로 값을 지우는 용도로는 쓸 수 없다 — 셋 다 필수 컬럼이라 문제없다.
 */
public record ChallengeUpdateRequest(
        @Size(max = 100, message = "챌린지명은 100자 이하여야 합니다.")
        String title,

        @Min(value = 1, message = "목표 금액은 1원 이상이어야 합니다.")
        Long goalAmount,

        LocalDate startDate,

        LocalDate endDate
) {
}
