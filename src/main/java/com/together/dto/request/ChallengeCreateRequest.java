package com.together.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code POST /challenges} (화면 5·6).
 *
 * <p>{@code nickname} 은 방장 본인의 이 챌린지 전용 닉네임이다. 로그인 시점이 아니라 챌린지를
 * 만드는 시점에 받는다(FR-003 개정).
 */
public record ChallengeCreateRequest(
        @NotBlank(message = "챌린지명은 필수입니다.")
        @Size(max = 100, message = "챌린지명은 100자 이하여야 합니다.")
        String title,

        @NotNull(message = "목표 금액은 필수입니다.")
        @Min(value = 1, message = "목표 금액은 1원 이상이어야 합니다.")
        Long goalAmount,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname
) {
}
