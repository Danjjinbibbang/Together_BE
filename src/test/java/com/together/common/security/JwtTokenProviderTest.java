package com.together.common.security;

import com.together.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(
            new JwtProperties("test-secret-key-for-together-be-hs256-at-least-32bytes", 720, 30, "together-be"));

    @Test
    void 발급한_토큰에서_userId를_다시_읽어낸다() {
        String token = provider.createToken(42L);

        assertThat(provider.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void 위조된_토큰은_null을_반환한다() {
        String token = provider.createToken(42L) + "tampered";

        assertThat(provider.parseUserId(token)).isNull();
    }
}