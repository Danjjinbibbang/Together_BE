package com.together.common.security;

import com.together.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * FR-031 자체 JWT 발급/검증.
 *
 * <p>subject 에는 User.id 를 담는다. Member.id 는 챌린지별로 달라지므로 토큰에 담지 않는다.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final Duration expiration;
    private final String issuer;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = resolveKey(properties.secret());
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
        this.issuer = properties.issuer();
    }

    private static SecretKey resolveKey(String secret) {
        if (!StringUtils.hasText(secret)) {
            log.warn("app.jwt.secret 이 비어 있어 임시 키를 생성합니다. 재기동하면 기존 토큰이 모두 무효화됩니다. "
                    + "로컬 외 환경에서는 JWT_SECRET 환경변수를 반드시 설정하세요.");
            return Jwts.SIG.HS256.key().build();
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * @return 유효한 토큰이면 User.id, 아니면 null
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return null;
        }
    }

    public long expirationSeconds() {
        return expiration.getSeconds();
    }
}