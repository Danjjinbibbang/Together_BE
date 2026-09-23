package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /mission-logs/{missionLogId}/reactions} (FR-022, 화면 14).
 *
 * <p>토글이다. 같은 타입을 다시 보내면 해제, 다른 타입을 보내면 교체된다 — 멤버당 반응은 최대 1개.
 */
public record ReactionToggleRequest(
        @NotBlank(message = "리액션 타입은 필수입니다.")
        @Size(max = 20, message = "리액션 타입이 너무 깁니다.")
        String reactionType
) {
}
