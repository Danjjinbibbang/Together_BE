package com.together.dto.response;

import java.time.LocalDateTime;

/** Auth 태그 응답 모음. */
public final class AuthResponses {

    private AuthResponses() {
    }

    /**
     * {@code POST /auth/kakao} 응답.
     *
     * <p>{@code isNewUser=true} 면 프론트는 약관동의(화면 1)로, false 면 홈(화면 3)으로 분기한다(FR-032).
     *
     * <p>FR-035 로 리프레시 토큰이 함께 나간다. 액세스 토큰이 만료되면 재로그인 대신
     * {@code POST /auth/refresh} 로 갱신하면 된다.
     */
    public record KakaoLogin(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn,
            boolean isNewUser,
            UserSummary user
    ) {
    }

    public record UserSummary(Long userId, String kakaoId, LocalDateTime createdAt) {
    }

    /**
     * {@code POST /auth/refresh} 응답 (FR-035).
     *
     * <p>회전(rotation) 방식이라 <b>리프레시 토큰도 매번 새로 나간다.</b> 프론트는 둘 다 갈아끼워야
     * 하며, 이전 리프레시 토큰은 그 즉시 못 쓰게 된다.
     *
     * @param accessTokenExpiresIn 액세스 토큰 유효기간(초)
     */
    public record TokenPair(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn
    ) {
    }

    /** {@code POST /auth/terms-agreement} 응답. */
    public record TermsAgreement(Long userId, LocalDateTime termsAgreedAt, String termsVersion) {
    }
}
