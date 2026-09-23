package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** {@code PATCH /challenges/{challengeId}/mission-mode} (화면 9, 방장 전용) */
public record MissionModeUpdateRequest(
        @NotBlank(message = "미션 운영 방식은 필수입니다.")
        @Pattern(regexp = "FIXED|AI", message = "미션 운영 방식은 FIXED 또는 AI 여야 합니다.")
        String missionMode
) {
}
