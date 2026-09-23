package com.together.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Social 태그(댓글·리액션·팀 응원 메시지) 응답 모음. */
public final class SocialResponses {

    private SocialResponses() {
    }

    /** {@code GET /mission-logs/{missionLogId}/comments} (화면 14) */
    public record CommentList(List<Item> comments) {

        public record Item(
                Long commentId,
                Long memberId,
                String nickname,
                String content,
                LocalDateTime createdAt
        ) {
        }
    }

    /** {@code POST /mission-logs/{missionLogId}/comments} */
    public record CommentCreated(
            Long commentId,
            Long missionLogId,
            Long memberId,
            String content,
            LocalDateTime createdAt
    ) {
    }

    /**
     * {@code GET /mission-logs/{missionLogId}/reactions} (화면 14).
     *
     * <p>{@code myReaction} 은 요청자 본인이 남긴 타입이고 없으면 null 이다 — 멤버당 최대 1개다.
     */
    public record ReactionSummary(Map<String, Integer> reactionCounts, String myReaction) {
    }

    /**
     * {@code POST /mission-logs/{missionLogId}/reactions} 토글 결과.
     *
     * <p>해제된 경우 {@code reactionType} 이 null 이다.
     */
    public record ReactionToggled(Long missionLogId, Long memberId, String reactionType) {
    }

    /**
     * {@code POST /challenges/{challengeId}/messages} (FR-044)
     *
     * @param notifiedMemberCount 실제로 알림을 받은 사람 수. 보낸 사람과 수신을 꺼둔 사람은 빠진다
     */
    public record TeamMessageSent(
            Long messageId,
            Long challengeId,
            Long senderMemberId,
            String content,
            LocalDateTime createdAt,
            int notifiedMemberCount
    ) {
    }

    /**
     * {@code PATCH /challenges/{challengeId}/messages/{messageId}} (FR-044)
     *
     * <p>{@code updatedAt} 이 있으면 고쳐진 메시지다 — 목록에서도 같은 필드로 "수정됨"을 그릴 수 있다.
     */
    public record TeamMessageUpdated(
            Long messageId,
            Long challengeId,
            Long senderMemberId,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /** {@code GET /challenges/{challengeId}/messages} — 커서 페이지네이션. */
    public record TeamMessageList(List<Item> messages, Long nextCursor) {

        public record Item(
                Long messageId,
                Long senderMemberId,
                String nickname,
                String content,
                LocalDateTime createdAt,
                LocalDateTime updatedAt,
                boolean mine
        ) {
        }
    }
}
