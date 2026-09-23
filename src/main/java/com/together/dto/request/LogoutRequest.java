package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /auth/logout} (FR-035).
 *
 * <p>액세스 토큰은 stateless 라 서버가 회수할 수 없다. 대신 리프레시 토큰을 폐기해 더는 갱신되지
 * 않게 한다 — 남은 액세스 토큰은 만료까지만 유효하다.
 */
public record LogoutRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
