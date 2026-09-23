package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * REFRESH_TOKEN 테이블 행(FR-035).
 *
 * <p>원문은 저장하지 않는다. {@code tokenHash} 는 SHA-256 hex 이고, 프론트가 보낸 토큰을 같은 방식으로
 * 해시해 대조한다 — DB 가 새더라도 토큰을 재현할 수 없어야 하기 때문이다.
 */
@Getter
@Setter
public class RefreshToken {

    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    /** 폐기 시각. null 이면 살아 있는 토큰 */
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
}
