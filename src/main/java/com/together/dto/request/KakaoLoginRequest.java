package com.together.dto.request;

import jakarta.validation.constraints.NotBlank;

/** {@code POST /auth/kakao} */
public record KakaoLoginRequest(
        @NotBlank(message = "인가코드는 필수입니다.")
        String authorizationCode
) {
}
