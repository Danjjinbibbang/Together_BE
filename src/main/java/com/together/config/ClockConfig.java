package com.together.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "오늘"이 언제인지는 미션 배정·제출 판정의 기준이라 테스트에서 고정할 수 있어야 한다.
 * 그래서 {@code LocalDate.now()} 를 직접 부르지 않고 주입받은 시계를 쓴다.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
