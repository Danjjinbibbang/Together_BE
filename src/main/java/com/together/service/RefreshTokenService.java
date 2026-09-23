package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.config.JwtProperties;
import com.together.dto.RefreshToken;
import com.together.mapper.RefreshTokenMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-035 리프레시 토큰 발급·회전·폐기.
 *
 * <p>액세스 토큰(JWT)은 stateless 라 서버가 회수할 수 없다. 그래서 "로그아웃"과 "탈취 대응"을
 * 리프레시 토큰 쪽에 둔다 — 이쪽은 DB 에 있으니 언제든 끊을 수 있다.
 *
 * <p>토큰 원문은 발급 순간 응답으로만 나가고 어디에도 남기지 않는다. DB 에는 SHA-256 해시만 둔다.
 */
@Service
public class RefreshTokenService {

    /** 토큰 엔트로피. 256비트면 추측이 불가능하다. */
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenMapper refreshTokenMapper;
    private final Clock clock;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper,
                               JwtProperties jwtProperties,
                               Clock clock) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.clock = clock;
        this.ttl = Duration.ofDays(jwtProperties.refreshExpirationDays());
    }

    /** 회전 결과. 새 토큰과 그 주인을 함께 돌려준다. */
    public record Rotated(Long userId, String refreshToken) {
    }

    /** 새 토큰을 발급하고 <b>원문</b>을 돌려준다. 이 값은 응답으로 나간 뒤 서버에 남지 않는다. */
    @Transactional
    public String issue(Long userId) {
        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken row = new RefreshToken();
        row.setUserId(userId);
        row.setTokenHash(hash(token));
        row.setExpiresAt(LocalDateTime.now(clock).plus(ttl));
        refreshTokenMapper.insert(row);
        return token;
    }

    /**
     * 회전. 받은 토큰을 폐기하고 새 토큰을 발급한다.
     *
     * <p>이미 폐기된 토큰이 다시 오면 <b>그 사용자의 토큰을 전부 내린다.</b> 정상 흐름에서는 한 번
     * 쓴 토큰이 다시 올 수 없으므로, 다시 왔다는 것은 어딘가에서 새어 나갔다는 신호로 본다.
     */
    @Transactional
    public Rotated rotate(String presented) {
        RefreshToken row = refreshTokenMapper.findByTokenHash(hash(presented));
        if (row == null) {
            throw unauthorized();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (row.getRevokedAt() != null) {
            refreshTokenMapper.revokeAllByUserId(row.getUserId(), now);
            throw new BusinessException(HttpStatus.UNAUTHORIZED,
                    "이미 사용된 리프레시 토큰입니다. 보안을 위해 모든 세션을 종료했습니다. 다시 로그인해주세요.");
        }
        if (row.getExpiresAt().isBefore(now)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED,
                    "리프레시 토큰이 만료됐습니다. 다시 로그인해주세요.");
        }

        refreshTokenMapper.revokeById(row.getId(), now);
        return new Rotated(row.getUserId(), issue(row.getUserId()));
    }

    /**
     * 로그아웃. 본인 토큰일 때만 내린다.
     *
     * <p>남의 토큰이거나 없는 토큰이어도 조용히 넘어간다 — 여기서 404 를 주면 "그 토큰이 존재하는가"를
     * 떠볼 수 있게 된다. 어차피 로그아웃의 결과는 "이제 못 쓴다"로 같다.
     */
    @Transactional
    public void revoke(Long userId, String presented) {
        RefreshToken row = refreshTokenMapper.findByTokenHash(hash(presented));
        if (row != null && row.getUserId().equals(userId)) {
            refreshTokenMapper.revokeById(row.getId(), LocalDateTime.now(clock));
        }
    }

    /** 계정 탈퇴·연동 해제처럼 세션을 통째로 끊어야 할 때. */
    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenMapper.revokeAllByUserId(userId, LocalDateTime.now(clock));
    }

    private static BusinessException unauthorized() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
    }

    /** 원문을 DB 에 두지 않으려고 해시로 대조한다. 토큰은 고엔트로피라 솔트 없이 SHA-256 이면 충분하다. */
    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 JDK 표준이라 실제로는 오지 않는다.
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다.", e);
        }
    }
}
