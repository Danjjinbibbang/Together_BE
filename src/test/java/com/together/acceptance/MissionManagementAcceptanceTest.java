package com.together.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.together.dto.Challenge;
import com.together.dto.Member;
import com.together.dto.Mission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 미션 관리(화면 23·24·25)에서 프론트가 오래 기다리던 세 가지 — 목록의 {@code minTextLength},
 * 생성 시 {@code createdByType}, 그리고 미션 삭제(FR-047).
 */
class MissionManagementAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("미션 목록에 minTextLength 가 실려 수정 화면이 현재값을 채울 수 있다")
    void listExposesMinTextLength() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        Member owner = newMember(ownerUserId, challenge.getId(), "OWNER");
        newMission(challenge.getId(), owner.getId(), 30);

        mockMvc.perform(get("/challenges/{id}/missions", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missions[0].minTextLength").value(30))
                .andExpect(jsonPath("$.missions[0].isActive").value(true));
    }

    @Test
    @DisplayName("createdByType=AI 로 등록하면 AI 로 기록된다")
    void createdByTypeIsHonored() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");

        mockMvc.perform(post("/challenges/{id}/missions", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(
                                "title", "AI 가 제안한 미션",
                                "submitType", "TEXT",
                                "rewardMin", "100",
                                "rewardMax", "2000",
                                "minTextLength", "30",
                                "createdByType", "AI")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByType").value("AI"));

        // 생략하면 방장이 직접 만든 것으로 본다
        mockMvc.perform(post("/challenges/{id}/missions", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(
                                "title", "방장이 직접 쓴 미션",
                                "submitType", "TEXT",
                                "rewardMin", "100",
                                "rewardMax", "2000",
                                "minTextLength", "30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByType").value("OWNER"));
    }

    @Test
    @DisplayName("아직 배정된 적 없는 미션은 지워진다")
    void unusedMissionIsDeletable() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        Member owner = newMember(ownerUserId, challenge.getId(), "OWNER");
        Mission mission = newMission(challenge.getId(), owner.getId(), 30);

        mockMvc.perform(delete("/missions/{id}", mission.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId)))
                .andExpect(status().isNoContent());

        assertThat(missionMapper.findById(mission.getId())).isNull();
    }

    @Test
    @DisplayName("한 번이라도 배정된 미션은 409 — 비활성화로 안내한다")
    void assignedMissionCannotBeDeleted() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        Member owner = newMember(ownerUserId, challenge.getId(), "OWNER");
        Mission mission = newMission(challenge.getId(), owner.getId(), 30);
        assignToday(challenge.getId(), mission.getId(), 500);

        mockMvc.perform(delete("/missions/{id}", mission.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId)))
                .andExpect(status().isConflict());

        // 지워지지 않았고, 비활성화는 여전히 가능하다
        assertThat(missionMapper.findById(mission.getId())).isNotNull();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/missions/{id}", mission.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isActive", "false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("팀원은 미션을 지울 수 없다")
    void memberCannotDeleteMission() throws Exception {
        Long ownerUserId = newUser();
        Long mateUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        Member owner = newMember(ownerUserId, challenge.getId(), "OWNER");
        newMember(mateUserId, challenge.getId(), "MEMBER");
        Mission mission = newMission(challenge.getId(), owner.getId(), 30);

        mockMvc.perform(delete("/missions/{id}", mission.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateUserId)))
                .andExpect(status().isForbidden());
    }
}
