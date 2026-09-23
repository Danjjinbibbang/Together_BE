package com.together.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.together.dto.Challenge;
import com.together.dto.Member;
import com.together.dto.Mission;
import com.together.dto.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * FR-045·FR-046. 목표 금액은 1인당이고, 채운 사람은 미션 수행이 끝난다.
 *
 * <p>프론트가 §12 에서 "화면에서만 막혀 우회 가능"하다고 지적한 지점과, 팀 달성을 전달할 길이
 * 없다던 지점을 실제 요청으로 확인한다.
 */
class GoalAchievementAcceptanceTest extends AcceptanceTestSupport {

    /** 보상 한 번으로 목표를 정확히 채우도록 잡은 금액. DB 제약상 보상은 100~10,000원이다. */
    private static final long GOAL = 1_000L;

    @Test
    @DisplayName("진행률 분모가 1인당 목표 × 활성 멤버수다")
    void progressRateUsesPerMemberGoal() throws Exception {
        Long meUserId = newUser();
        Challenge challenge = newChallenge(GOAL);
        Member me = newMember(meUserId, challenge.getId(), "OWNER");
        Member mate = newMember(newUser(), challenge.getId(), "MEMBER");

        // 한 명만 목표를 채운 상태 — 팀 목표(1,000 × 2)의 절반이다
        addBalance(mate.getId(), GOAL);

        mockMvc.perform(get("/challenges/{id}", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(meUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(1_000))
                // 예전 수식(÷ goalAmount)이었다면 100 이 나왔을 자리다
                .andExpect(jsonPath("$.progressRate").value(50))
                .andExpect(jsonPath("$.myBalance").value(0))
                .andExpect(jsonPath("$.isGoalAchieved").value(false));

        assertThat(balanceOf(me.getId())).isZero();
    }

    @Test
    @DisplayName("목표를 채운 멤버는 미션을 제출할 수 없다 (403)")
    void achieverCannotSubmit() throws Exception {
        Long achieverUserId = newUser();
        Challenge challenge = newChallenge(GOAL);
        Member achiever = newMember(achieverUserId, challenge.getId(), "OWNER");
        addBalance(achiever.getId(), GOAL);

        Mission mission = newMission(challenge.getId(), achiever.getId(), 5);
        assignToday(challenge.getId(), mission.getId(), 500);

        mockMvc.perform(post("/missions/{id}/submit", mission.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(achieverUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("submitContent", "오늘도 열심히 했습니다")))
                .andExpect(status().isForbidden());

        // 달성 여부는 서버가 판정해 응답으로 알려준다 — 프론트가 버튼을 잠글 근거다
        mockMvc.perform(get("/challenges/{id}", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(achieverUserId)))
                .andExpect(jsonPath("$.isGoalAchieved").value(true));
    }

    @Test
    @DisplayName("마지막 주자가 채우면 팀 전원에게 TEAM_GOAL_ACHIEVED 가 간다")
    void lastMemberTriggersTeamNotification() throws Exception {
        Long meUserId = newUser();
        Challenge challenge = newChallenge(GOAL);
        Member me = newMember(meUserId, challenge.getId(), "OWNER");
        Member mate = newMember(newUser(), challenge.getId(), "MEMBER");

        // 팀원은 이미 다 채웠고, 내가 마지막 주자다
        addBalance(mate.getId(), GOAL);

        Mission mission = newMission(challenge.getId(), me.getId(), 5);
        assignToday(challenge.getId(), mission.getId(), (int) GOAL);

        mockMvc.perform(post("/missions/{id}/submit", mission.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(meUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("submitContent", "마지막 한 걸음까지 왔습니다")))
                .andExpect(status().isCreated())
                // 텍스트 미션은 최소 글자수를 채우면 그 자리에서 승인되고 보상이 나간다
                .andExpect(jsonPath("$.status").value("AI_APPROVED"));

        assertThat(balanceOf(me.getId())).isEqualTo(GOAL);

        // 팀 달성은 내가 아니라 팀 전체의 사건이라 보낸 사람까지 전원이 받는다
        assertThat(notificationCount(me.getId(), "TEAM_GOAL_ACHIEVED")).isEqualTo(1);
        assertThat(notificationCount(mate.getId(), "TEAM_GOAL_ACHIEVED")).isEqualTo(1);

        Notification teamNoti = notificationMapper.findByReceiverMemberId(mate.getId()).stream()
                .filter(n -> "TEAM_GOAL_ACHIEVED".equals(n.getNotiType()))
                .findFirst()
                .orElseThrow();
        // 대상이 미션이 아니라 챌린지다 — 알림을 누르면 그룹 홈으로 보내면 된다
        assertThat(teamNoti.getTargetType()).isEqualTo("CHALLENGE");
        assertThat(teamNoti.getTargetId()).isEqualTo(challenge.getId());
        assertThat(teamNoti.getIsRead()).isEqualTo("N");

        mockMvc.perform(get("/challenges/{id}", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(meUserId)))
                .andExpect(jsonPath("$.progressRate").value(100))
                .andExpect(jsonPath("$.isGoalAchieved").value(true));
    }

    @Test
    @DisplayName("이미 달성한 팀에서는 축하 알림이 다시 가지 않는다")
    void teamNotificationDoesNotRepeat() throws Exception {
        Long meUserId = newUser();
        Challenge challenge = newChallenge(GOAL);
        Member me = newMember(meUserId, challenge.getId(), "OWNER");
        Member mate = newMember(newUser(), challenge.getId(), "MEMBER");

        // 두 명 다 이미 채운 상태에서 시작한다
        addBalance(me.getId(), GOAL);
        addBalance(mate.getId(), GOAL);

        Mission mission = newMission(challenge.getId(), me.getId(), 5);
        assignToday(challenge.getId(), mission.getId(), 500);

        // 달성자는 제출이 막히므로 새 입금 자체가 생기지 않는다
        mockMvc.perform(post("/missions/{id}/submit", mission.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(meUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("submitContent", "이미 다 모았지만 한 번 더")))
                .andExpect(status().isForbidden());

        assertThat(notificationCount(me.getId(), "TEAM_GOAL_ACHIEVED")).isZero();
        assertThat(notificationCount(mate.getId(), "TEAM_GOAL_ACHIEVED")).isZero();
    }

    @Test
    @DisplayName("달성자도 팀 응원 메시지는 보낼 수 있다")
    void achieverCanStillCheerTeam() throws Exception {
        Long achieverUserId = newUser();
        Challenge challenge = newChallenge(GOAL);
        Member achiever = newMember(achieverUserId, challenge.getId(), "OWNER");
        newMember(newUser(), challenge.getId(), "MEMBER");
        addBalance(achiever.getId(), GOAL);

        // 다 채운 사람이 아직 채우는 중인 팀원을 응원하라는 것이 FR-046 의 의도다
        mockMvc.perform(post("/challenges/{id}/messages", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(achieverUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("content", "먼저 도착했어요, 다들 화이팅!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notifiedMemberCount").value(1));
    }
}
