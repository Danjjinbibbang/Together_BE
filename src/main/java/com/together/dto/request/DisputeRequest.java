package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code POST /mission-logs/{missionLogId}/dispute} (FR-012, 화면 12). AI_REJECTED 상태에서만 호출 가능. */
public record DisputeRequest(
        @NotBlank(message = "이의제기 사유는 필수입니다.")
        @Size(max = 500, message = "이의제기 사유는 500자 이하여야 합니다.")
        String reason
) {
}
