package com.together.dto.request;

import jakarta.validation.constraints.Size;

/**
 * {@code POST /missions/{missionId}/submit} (화면 11).
 *
 * <p>제출 형식에 맞는 필드만 보내면 된다 — 텍스트 미션이면 {@code submitContent},
 * 사진 미션이면 {@code attachmentUrl}. 어느 쪽이 필요한지는 미션의 제출 형식에 달려 있어
 * 서비스에서 검사한다.
 */
public record MissionSubmitRequest(
        String submitContent,

        @Size(max = 500, message = "첨부 URL 이 너무 깁니다.")
        String attachmentUrl,

        /** 생략하면 공개(FR-015 기본값). */
        Boolean isPublic
) {
}
