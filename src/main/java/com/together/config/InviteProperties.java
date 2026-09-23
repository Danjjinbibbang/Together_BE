package com.together.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 초대 링크 생성 설정 (FR-002, 화면 6).
 *
 * @param baseUrl 초대 링크의 앞부분. 뒤에 {@code ?code=...} 가 붙는다.
 */
@ConfigurationProperties(prefix = "app.invite")
public record InviteProperties(String baseUrl) {
}
