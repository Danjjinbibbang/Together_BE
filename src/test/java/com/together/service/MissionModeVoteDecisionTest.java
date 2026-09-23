package com.together.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FR-008 투표 마감 판정 규칙. 요구사항정의서 v0.4 §4 가 "정족수 등 세부 룰 설계 필요"로 남겨둔
 * 자리를 채운 결정이라, 나중에 룰이 바뀌면 여기가 먼저 깨지도록 고정해 둔다.
 */
class MissionModeVoteDecisionTest {

    @Test
    @DisplayName("정족수를 채우고 표가 갈리면 다수 쪽으로 확정된다")
    void decidesByMajority() {
        MissionModeVoteService.Decision decision =
                MissionModeVoteService.decide(Map.of("FIXED", 1, "AI", 2), 2);

        assertThat(decision.outcome()).isEqualTo("DECIDED");
        assertThat(decision.resultMode()).isEqualTo("AI");
    }

    @Test
    @DisplayName("동률이면 확정하지 않는다 — 기존 미션 방식이 그대로 남는다")
    void tieChangesNothing() {
        MissionModeVoteService.Decision decision =
                MissionModeVoteService.decide(Map.of("FIXED", 2, "AI", 2), 2);

        assertThat(decision.outcome()).isEqualTo("TIE");
        assertThat(decision.resultMode()).isNull();
    }

    @Test
    @DisplayName("정족수를 못 채우면 다수결을 따지지 않는다")
    void quorumComesFirst() {
        // 표가 갈려 있어도(AI 우세) 참여가 모자라면 결정하지 않는다
        MissionModeVoteService.Decision decision =
                MissionModeVoteService.decide(Map.of("FIXED", 0, "AI", 1), 3);

        assertThat(decision.outcome()).isEqualTo("NOT_ENOUGH_VOTES");
        assertThat(decision.resultMode()).isNull();
    }

    @Test
    @DisplayName("아무도 던지지 않은 투표는 결정되지 않는다")
    void emptyVoteIsNeverDecided() {
        // 정족수가 최소 1이라(requiredVotes) 0표는 항상 여기로 떨어진다
        MissionModeVoteService.Decision decision =
                MissionModeVoteService.decide(Map.of("FIXED", 0, "AI", 0), 1);

        assertThat(decision.outcome()).isEqualTo("NOT_ENOUGH_VOTES");
    }
}
