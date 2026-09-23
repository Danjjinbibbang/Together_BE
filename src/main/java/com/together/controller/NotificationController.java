package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.response.NotificationResponses;
import com.together.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 알림은 챌린지별이 아니라 전역이다(화면 15, 상단바 배지). */
@Tag(name = "Notification")
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "내 알림 목록 (폴링, 화면 15). 커서 페이지네이션")
    @GetMapping
    public NotificationResponses.NotificationList list(
            @LoginUserId Long userId,
            @Parameter(description = "직전 응답의 nextCursor. 첫 페이지는 생략")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "기본 20, 최대 100")
            @RequestParam(required = false) Integer size) {
        return notificationService.list(userId, cursor, size);
    }

    @Operation(summary = "상단바 뱃지용 미확인 개수 (전역)")
    @GetMapping("/unread-count")
    public NotificationResponses.UnreadCount unreadCount(@LoginUserId Long userId) {
        return notificationService.unreadCount(userId);
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public NotificationResponses.Read markAsRead(@LoginUserId Long userId,
                                                 @PathVariable Long notificationId) {
        return notificationService.markAsRead(userId, notificationId);
    }
}
