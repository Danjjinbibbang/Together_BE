package com.together.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-044 팀 응원 메시지 설정.
 *
 * @param dailyLimitPerMember 한 사람이 하루에 보낼 수 있는 건수. 도배를 막는 유일한 장치다
 */
@ConfigurationProperties(prefix = "app.message")
public record MessageProperties(
        int dailyLimitPerMember
) {
}
