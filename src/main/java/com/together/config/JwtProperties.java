package com.together.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-031 자체 JWT 발급 설정.
 *
 * @param secret            HS256 서명 키(Base64 또는 32자 이상 평문). 비워두면 기동 시 임시 키를 생성한다.
 * @param expirationMinutes      액세스 토큰 만료(분)
 * @param refreshExpirationDays  리프레시 토큰 만료(일). FR-035 회전 방식이라 재발급 때마다 새로 발급된다
 * @param issuer            토큰 발급자
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes,
        long refreshExpirationDays,
        String issuer
) {
}
