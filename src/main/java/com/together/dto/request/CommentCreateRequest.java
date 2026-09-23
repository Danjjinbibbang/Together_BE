package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code POST /mission-logs/{missionLogId}/comments} (FR-023, 화면 14) */
public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 1000, message = "댓글은 1,000자 이하여야 합니다.")
        String content
) {
}
