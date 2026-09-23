package com.together.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-012c 재판정 설정. "방장이 없거나 응답이 없을 때"를 얼마나 기다릴지가 v0.4 §4 미확정 항목이라
 * 설정으로 뺐다.
 *
 * @param ownerResponseHours 방장 판정을 기다리는 시간. 넘기면 작성자가 자동 승인을 요청할 수 있다
 */
@ConfigurationProperties(prefix = "app.review")
public record ReviewProperties(
        int ownerResponseHours
) {
}
