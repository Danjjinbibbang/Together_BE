package com.together.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * {@code POST /mission-logs/{missionLogId}/review} (FR-012, 방장 재판정).
 *
 * <p>{@code approved=false} 일 때만 {@code rejectReasonCode} 가 필요하다. 이 조합 검사는
 * 필드 두 개를 함께 봐야 해서 서비스에서 한다.
 */
public record ReviewRequest(
        @NotNull(message = "승인 여부는 필수입니다.")
        Boolean approved,

        @Pattern(regexp = "LOW_QUALITY|LOW_LIGHT|IRRELEVANT|ETC",
                message = "반려 사유 코드가 올바르지 않습니다.")
        String rejectReasonCode
) {
}
