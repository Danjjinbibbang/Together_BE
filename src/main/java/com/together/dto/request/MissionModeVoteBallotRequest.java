package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** {@code POST /challenges/{challengeId}/mission-mode/vote/ballots} — 같은 사람이 다시 보내면 표가 바뀐다. */
public record MissionModeVoteBallotRequest(
        @NotBlank(message = "선택은 필수입니다.")
        @Pattern(regexp = "FIXED|AI", message = "선택은 FIXED 또는 AI 여야 합니다.")
        String choice
) {
}
