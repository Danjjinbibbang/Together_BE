package com.together.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-008 미션 방식 팀원 투표 설정. 요구사항정의서 v0.4 §4 가 "정족수·기간 등 세부 룰 설계 필요"로
 * 남겨둔 값들이라, 코드에 박지 않고 설정으로 뺐다 — 정해지면 값만 바꾸면 된다.
 *
 * @param defaultDurationHours 투표 기본 기간(시간). 방장이 열 때 지정하면 그 값이 우선한다
 * @param quorumPercent        유효 투표로 인정할 최소 참여율(%). 미달이면 결과를 확정하지 않는다
 */
@ConfigurationProperties(prefix = "app.vote")
public record VoteProperties(
        int defaultDurationHours,
        int quorumPercent
) {
}
