package com.together.mapper;

import com.together.dto.RefreshToken;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

/**
 * REFRESH_TOKEN 테이블 접근(FR-035).
 * {@code resources/mapper/RefreshTokenMapper.xml} 과 1:1 대응한다.
 */
public interface RefreshTokenMapper {

    /**
     * 해시로 찾는다. 만료·폐기 여부는 걸러내지 않고 그대로 돌려준다 —
     * "폐기된 토큰이 다시 쓰였다"는 것을 서비스가 알아채야 하기 때문이다(재사용 감지).
     */
    RefreshToken findByTokenHash(@Param("tokenHash") String tokenHash);

    int insert(RefreshToken token);

    /** 회전(rotation)으로 이전 토큰을 내릴 때. 이미 폐기된 행은 건드리지 않는다. */
    int revokeById(@Param("id") Long id, @Param("revokedAt") LocalDateTime revokedAt);

    /** 로그아웃·계정 탈퇴·재사용 감지에서 한 사용자의 살아 있는 토큰을 통째로 내린다. */
    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);
}
