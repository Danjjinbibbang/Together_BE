package com.together.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Notification 태그 응답 모음. */
public final class NotificationResponses {

    private NotificationResponses() {
    }

    /**
     * {@code GET /notifications} (화면 15, 폴링).
     *
     * <p>특정 챌린지가 아니라 전역이다. 프론트는 {@code targetType} 으로 이동 화면을 분기한다.
     *
     * <p>커서 페이지네이션. {@code nextCursor} 를 다음 요청의 {@code cursor} 로 넘기면 이어진다.
     * 마지막 페이지면 null 이라 응답에서 빠진다 — 키가 없으면 더 불러올 게 없다는 뜻이다.
     */
    public record NotificationList(List<Item> notifications, Long nextCursor) {

        public record Item(
                Long notificationId,
                String type,
                String content,
                String targetType,
                Long targetId,
                Boolean isRead,
                LocalDateTime createdAt
        ) {
        }
    }

    /** {@code GET /notifications/unread-count} */
    public record UnreadCount(int unreadCount) {
    }

    /** {@code PATCH /notifications/{notificationId}/read} */
    public record Read(Long notificationId, Boolean isRead) {
    }
}
