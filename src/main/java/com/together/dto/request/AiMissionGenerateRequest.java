package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /challenges/{challengeId}/missions/ai-generate} (FR-009).
 *
 * <p>이 호출만으로는 미션이 저장되지 않는다. 방장이 제안 중 하나를 골라
 * {@code POST /challenges/{challengeId}/missions} 로 등록하는 2단계 흐름이다.
 */
public record AiMissionGenerateRequest(
        @NotBlank(message = "테마는 필수입니다.")
        @Size(max = 50, message = "테마는 50자 이하여야 합니다.")
        String theme
) {
}
