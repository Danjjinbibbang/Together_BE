package com.together.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.together.dto.Challenge;
import com.together.dto.Member;
import com.together.dto.Notification;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * FR-044 팀 응원 메시지. 보내기·수정·삭제가 <b>팀원 알림함까지</b> 따라가는지가 핵심이다.
 *
 * <p>알림 본문은 발송 시점의 스냅샷 복사본이라, 원본만 고쳐서는 팀원 화면에 반영되지 않는다.
 * 그 동기화가 실제 요청에서 되는지를 여기서 본다.
 */
class TeamMessageAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("보내면 나를 뺀 팀원에게만 알림이 간다")
    void sendNotifiesEveryoneButSender() throws Exception {
        Long senderUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        Member sender = newMember(senderUserId, challenge.getId(), "OWNER");
        Member mate = newMember(newUser(), challenge.getId(), "MEMBER");

        mockMvc.perform(post("/challenges/{id}/messages", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("content", "이번 주도 다들 화이팅!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderMemberId").value(sender.getId()))
                .andExpect(jsonPath("$.notifiedMemberCount").value(1));

        // 내 응원을 내 알림함에서 다시 보는 것은 의미가 없다
        assertThat(notificationCount(sender.getId(), "TEAM_CHEER")).isZero();
        assertThat(notificationCount(mate.getId(), "TEAM_CHEER")).isEqualTo(1);

        Notification noti = cheerOf(mate.getId()).orElseThrow();
        assertThat(noti.getContent()).contains(sender.getNickname(), "이번 주도 다들 화이팅!");
        assertThat(noti.getTargetType()).isEqualTo("TEAM_MESSAGE");
    }

    @Test
    @DisplayName("수정하면 팀원 알림함의 문구도 바뀌고, 읽음 상태는 그대로다")
    void editSyncsNotificationButKeepsReadFlag() throws Exception {
        Long senderUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(senderUserId, challenge.getId(), "OWNER");
        Member mate = newMember(newUser(), challenge.getId(), "MEMBER");

        long messageId = sendMessage(senderUserId, challenge.getId(), "오늘도 조금씩!");

        // 팀원이 이미 읽은 상태를 만든다
        Notification before = cheerOf(mate.getId()).orElseThrow();
        notificationMapper.markAsRead(before.getId());

        mockMvc.perform(patch("/challenges/{cid}/messages/{mid}", challenge.getId(), messageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("content", "역시 우리 팀!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("역시 우리 팀!"))
                .andExpect(jsonPath("$.updatedAt").exists());

        Notification after = notificationMapper.findById(before.getId());
        assertThat(after.getContent()).contains("역시 우리 팀!");
        // 오타 하나 고쳤다고 팀 전체 뱃지가 다시 켜지면 안 된다
        assertThat(after.getIsRead()).isEqualTo("Y");
    }

    @Test
    @DisplayName("지우면 팀원 알림함에서도 흔적 없이 사라진다")
    void deleteRemovesNotifications() throws Exception {
        Long senderUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(senderUserId, challenge.getId(), "OWNER");
        Member mate = newMember(newUser(), challenge.getId(), "MEMBER");

        long messageId = sendMessage(senderUserId, challenge.getId(), "잘못 보낸 메시지");
        assertThat(cheerOf(mate.getId())).isPresent();

        mockMvc.perform(delete("/challenges/{cid}/messages/{mid}", challenge.getId(), messageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isNoContent());

        assertThat(cheerOf(mate.getId())).isEmpty();
        mockMvc.perform(get("/challenges/{id}/messages", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isEmpty());

        // 이미 사라진 메시지는 404 다 — "있는데 못 지운다"로 보이면 안 된다
        mockMvc.perform(delete("/challenges/{cid}/messages/{mid}", challenge.getId(), messageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("남이 보낸 메시지는 고치거나 지울 수 없다")
    void othersMessageIsForbidden() throws Exception {
        Long senderUserId = newUser();
        Long mateUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(senderUserId, challenge.getId(), "OWNER");
        newMember(mateUserId, challenge.getId(), "MEMBER");

        long messageId = sendMessage(senderUserId, challenge.getId(), "내가 보낸 응원");

        mockMvc.perform(patch("/challenges/{cid}/messages/{mid}", challenge.getId(), messageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("content", "남의 메시지 고치기")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/challenges/{cid}/messages/{mid}", challenge.getId(), messageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(mateUserId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("하루 발송 한도를 넘기면 429 이고, 지워도 한도는 회복되지 않는다")
    void dailyLimitBlocksSpam() throws Exception {
        Long senderUserId = newUser();
        Challenge challenge = newChallenge(100_000L);
        newMember(senderUserId, challenge.getId(), "OWNER");
        newMember(newUser(), challenge.getId(), "MEMBER");

        long firstMessageId = sendMessage(senderUserId, challenge.getId(), "응원 1");
        for (int i = 2; i <= 5; i++) {
            sendMessage(senderUserId, challenge.getId(), "응원 " + i);
        }

        mockMvc.perform(post("/challenges/{id}/messages", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("content", "여섯 번째 응원")))
                .andExpect(status().isTooManyRequests());

        // 지우고 다시 보내는 우회를 막는다 — 지운 메시지도 그날 한도에는 계속 포함된다
        mockMvc.perform(delete("/challenges/{cid}/messages/{mid}", challenge.getId(), firstMessageId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/challenges/{id}/messages", challenge.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("content", "지웠으니 한 번 더")))
                .andExpect(status().isTooManyRequests());
    }

    // ------------------------------------------------------------------ 도우미

    private long sendMessage(Long userId, Long challengeId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/challenges/{id}/messages", challengeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("content", content)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String marker = "\"messageId\":";
        int from = body.indexOf(marker) + marker.length();
        int to = body.indexOf(',', from);
        return Long.parseLong(body.substring(from, to).trim());
    }

    private Optional<Notification> cheerOf(Long memberId) {
        List<Notification> all = notificationMapper.findByReceiverMemberId(memberId);
        return all.stream().filter(n -> "TEAM_CHEER".equals(n.getNotiType())).findFirst();
    }
}
