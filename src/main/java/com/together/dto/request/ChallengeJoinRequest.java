package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code POST /challenges/{challengeId}/join} (화면 7) */
public record ChallengeJoinRequest(
        @NotBlank(message = "초대코드는 필수입니다.")
        String inviteCode,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname
) {
}
