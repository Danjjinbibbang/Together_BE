package com.together.dto.request;

import jakarta.validation.constraints.NotNull;

/** {@code PATCH /challenges/{challengeId}/owner} (FR-037). 이전 즉시 본인은 일반 멤버가 되며 취소 불가. */
public record OwnerTransferRequest(
        @NotNull(message = "새 방장의 memberId 는 필수입니다.")
        Long newOwnerMemberId
) {
}
