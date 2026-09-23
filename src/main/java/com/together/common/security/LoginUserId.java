package com.together.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 인증된 요청의 {@code User.id} 를 컨트롤러 파라미터로 주입한다.
 *
 * <p>{@link JwtAuthenticationFilter} 가 principal 에 User.id 를 담으므로 그대로 꺼내 쓴다.
 * Member.id 는 챌린지마다 달라 토큰에 없다 — 필요한 곳에서 challengeId 로 조회해야 한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface LoginUserId {
}
