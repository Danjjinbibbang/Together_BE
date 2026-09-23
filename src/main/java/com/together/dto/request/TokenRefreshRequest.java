package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /auth/refresh} (FR-035). 액세스 토큰이 만료됐을 때 부른다 — 인증 헤더는 필요 없다. */
public record TokenRefreshRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
