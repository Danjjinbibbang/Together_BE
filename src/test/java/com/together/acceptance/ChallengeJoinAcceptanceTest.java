package com.together.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.together.dto.Challenge;
import com.together.dto.Member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 초대코드로 참여하는 흐름(화면 07). 프론트 §12 의 "완전히 막힌 화면"을 푼 경로다.
 *
 * <p>참여자가 가진 것은 초대코드뿐이라는 전제를 테스트가 그대로 지킨다 — 어느 요청에도
 * challengeId 를 넣지 않는다.
 */
class ChallengeJoinAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("초대코드만으로 미리보기부터 참여까지 된다")
    void previewThenJoin() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(300_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");

        Long guestUserId = newUser();

        mockMvc.perform(get("/challenges/by-invite-code/{code}", challenge.getInviteCode())
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeId").value(challenge.getId()))
                .andExpect(jsonPath("$.goalAmount").value(300_000))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.alreadyJoined").value(false))
                // 참여 전에는 팀 내부가 드러나면 안 된다
                .andExpect(jsonPath("$.members").doesNotExist())
                .andExpect(jsonPath("$.totalBalance").doesNotExist());

        mockMvc.perform(post("/challenges/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("inviteCode", challenge.getInviteCode(), "nickname", "새로온사람")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challengeId").value(challenge.getId()))
                .andExpect(jsonPath("$.nickname").value("새로온사람"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        Member joined = memberMapper.findByUserIdAndChallengeId(guestUserId, challenge.getId());
        assertThat(joined).isNotNull();
        // 참여하면 계좌도 함께 생긴다 — 첫 보상이 들어올 자리가 있어야 한다
        assertThat(accountMapper.findByMemberId(joined.getId())).isNotNull();
    }

    @Test
    @DisplayName("초대코드는 대소문자를 가리지 않는다")
    void inviteCodeIsCaseInsensitive() throws Exception {
        Long ownerUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(ownerUserId, challenge.getId(), "OWNER");
        Long guestUserId = newUser();

        // 사람이 불러주고 받아 적는 값이라 소문자로 칠 수 있다
        String lowercased = challenge.getInviteCode().toLowerCase();

        mockMvc.perform(get("/challenges/by-invite-code/{code}", lowercased)
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestUserId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/challenges/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("inviteCode", lowercased, "nickname", "소문자로친사람")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("없는 초대코드는 404 다")
    void unknownInviteCodeIsNotFound() throws Exception {
        Long guestUserId = newUser();

        mockMvc.perform(get("/challenges/by-invite-code/{code}", "NOSUCHCODE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestUserId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/challenges/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(guestUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("inviteCode", "NOSUCHCODE", "nickname", "아무개")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미 참여한 챌린지에 다시 참여하면 409 이고, 미리보기는 그 사실을 알려준다")
    void rejoinConflicts() throws Exception {
        Long userId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(userId, challenge.getId(), "MEMBER");

        mockMvc.perform(get("/challenges/by-invite-code/{code}", challenge.getInviteCode())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyJoined").value(true));

        mockMvc.perform(post("/challenges/join")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("inviteCode", challenge.getInviteCode(), "nickname", "다른닉네임")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("인증 없이는 미리보기도 볼 수 없다")
    void requiresAuthentication() throws Exception {
        Challenge challenge = newChallenge(100_000L);

        mockMvc.perform(get("/challenges/by-invite-code/{code}", challenge.getInviteCode()))
                .andExpect(status().isUnauthorized());
    }
}
