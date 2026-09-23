package com.together.dto.request;

import jakarta.validation.constraints.Size;

/**
 * {@code PATCH /mission-logs/{missionLogId}/dispute/resubmit} (FR-012b).
 *
 * <p>원래 제출형식에 맞는 필드만 보내면 된다.
 */
public record ResubmitRequest(
        String submitContent,

        @Size(max = 500, message = "첨부 URL 이 너무 깁니다.")
        String attachmentUrl
) {
}
