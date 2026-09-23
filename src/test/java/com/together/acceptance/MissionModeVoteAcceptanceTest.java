package com.together.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.together.dto.Challenge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * FR-008 확장 — 미션 운영 방식을 팀원 투표로 정한다.
 *
 * <p>확정 결과가 {@code CHALLENGE.MISSION_MODE} 에 실제로 반영되는지, 정족수를 못 채우면
 * 기존 설정을 건드리지 않는지가 핵심이다.
 */
class MissionModeVoteAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("투표를 열고 다수결로 마감하면 챌린지 미션 방식이 바뀐다")
    void majorityDecidesMissionMode() throws Exception {
        Long ownerUserId = newUser();
        Long mateUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");
        newMember(mateUserId, challenge.getId(), "MEMBER");
        challengeMapper.updateMissionMode(challenge.getId(), "FIXED");

        mockMvc.perform(post("/challenges/{id}/mission-mode/vote", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("durationHours", "24")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.eligibleVoters").value(2))
                .andExpect(jsonPath("$.requiredVotes").value(1))
                .andExpect(jsonPath("$.currentMissionMode").value("FIXED"))
                .andExpect(jsonPath("$.counts.FIXED").value(0))
                .andExpect(jsonPath("$.counts.AI").value(0));

        castBallot(ownerUserId, challenge.getId(), "AI");
        castBallot(mateUserId, challenge.getId(), "AI");

        mockMvc.perform(get("/challenges/{id}/mission-mode/vote", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votedCount").value(2))
                .andExpect(jsonPath("$.myChoice").value("AI"));

        mockMvc.perform(post("/challenges/{id}/mission-mode/vote/close", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.outcome").value("DECIDED"))
                .andExpect(jsonPath("$.resultMode").value("AI"));

        // 확정 결과는 챌린지에 그대로 반영된다 — 미션 뽑기 쪽은 손댈 것이 없다
        assertThat(challengeMapper.findById(challenge.getId()).getMissionMode()).isEqualTo("AI");
    }

    @Test
    @DisplayName("재투표는 표를 늘리지 않고 바꾼다")
    void revotingReplacesTheBallot() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");
        openVote(ownerUserId, challenge.getId());

        castBallot(ownerUserId, challenge.getId(), "FIXED");
        mockMvc.perform(post("/challenges/{id}/mission-mode/vote/ballots", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("choice", "AI")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votedCount").value(1))
                .andExpect(jsonPath("$.counts.FIXED").value(0))
                .andExpect(jsonPath("$.counts.AI").value(1))
                .andExpect(jsonPath("$.myChoice").value("AI"));
    }

    @Test
    @DisplayName("정족수를 못 채우면 기존 미션 방식을 그대로 둔다")
    void quorumFailureKeepsCurrentMode() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");
        // 멤버 4명이면 정족수는 2표다
        newMember(newUser(), challenge.getId(), "MEMBER");
        newMember(newUser(), challenge.getId(), "MEMBER");
        newMember(newUser(), challenge.getId(), "MEMBER");
        challengeMapper.updateMissionMode(challenge.getId(), "FIXED");

        openVote(ownerUserId, challenge.getId());
        castBallot(ownerUserId, challenge.getId(), "AI");

        mockMvc.perform(post("/challenges/{id}/mission-mode/vote/close", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NOT_ENOUGH_VOTES"))
                .andExpect(jsonPath("$.resultMode").doesNotExist());

        assertThat(challengeMapper.findById(challenge.getId()).getMissionMode()).isEqualTo("FIXED");
    }

    @Test
    @DisplayName("투표를 열고 닫는 것은 방장만 할 수 있다")
    void onlyOwnerOpensAndCloses() throws Exception {
        Long ownerUserId = newUser();
        Long mateUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");
        newMember(mateUserId, challenge.getId(), "MEMBER");

        mockMvc.perform(post("/challenges/{id}/mission-mode/vote", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateUserId)))
                .andExpect(status().isForbidden());

        openVote(ownerUserId, challenge.getId());

        // 챌린지당 열린 투표는 하나뿐이다
        mockMvc.perform(post("/challenges/{id}/mission-mode/vote", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/challenges/{id}/mission-mode/vote/close", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateUserId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("한 번도 열린 적 없으면 현황 조회가 404 다")
    void noVoteYet() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");

        mockMvc.perform(get("/challenges/{id}/mission-mode/vote", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ 도우미

    private void openVote(Long userId, Long challengeId) throws Exception {
        mockMvc.perform(post("/challenges/{id}/mission-mode/vote", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isCreated());
    }

    private void castBallot(Long userId, Long challengeId, String choice) throws Exception {
        mockMvc.perform(post("/challenges/{id}/mission-mode/vote/ballots", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("choice", choice)))
                .andExpect(status().isOk());
    }
}
