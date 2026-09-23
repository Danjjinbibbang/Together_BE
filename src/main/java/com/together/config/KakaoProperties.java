package com.together.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-030 카카오 인가코드 → 액세스 토큰 교환 설정.
 *
 * <p>NFR-005에 따라 카카오 액세스 토큰과 개인정보는 백엔드에서만 취급한다.
 */
@ConfigurationProperties(prefix = "app.kakao")
public record KakaoProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String tokenUri,
        String userInfoUri,
        /** 카카오 연결 끊기 콜백 검증용 Admin 키. 비워두면 콜백 엔드포인트가 항상 401 이다. */
        String adminKey
) {
}
