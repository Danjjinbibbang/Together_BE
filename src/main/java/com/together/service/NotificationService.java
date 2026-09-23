package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.Member;
import com.together.dto.Notification;
import com.together.dto.response.NotificationResponses;
import com.together.mapper.NotificationMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-020, FR-021. 알림함과 미확인 배지.
 *
 * <p>알림은 챌린지별 Member 에 달리지만 화면은 전역이다. 그래서 조회는 챌린지가 아니라
 * 사용자 기준으로 모은다.
 */
@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final MemberAccessService memberAccess;

    public NotificationService(NotificationMapper notificationMapper, MemberAccessService memberAccess) {
        this.notificationMapper = notificationMapper;
        this.memberAccess = memberAccess;
    }

    /**
     * 커서 방식. 다음 페이지가 있는지 알려고 요청한 개수보다 1건 더 읽고, 남으면 잘라낸 뒤
     * 마지막으로 남긴 항목의 ID 를 커서로 준다. COUNT 쿼리를 따로 돌리지 않아도 된다.
     */
    public NotificationResponses.NotificationList list(Long userId, Long cursor, Integer size) {
        int limit = Paging.size(size);
        List<Notification> rows = notificationMapper.findByUserId(userId, cursor, limit + 1);

        boolean hasNext = rows.size() > limit;
        List<Notification> page = hasNext ? rows.subList(0, limit) : rows;
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;

        return new NotificationResponses.NotificationList(
                page.stream()
                        .map(n -> new NotificationResponses.NotificationList.Item(
                                n.getId(), n.getNotiType(), n.getContent(),
                                n.getTargetType(), n.getTargetId(),
                                "Y".equals(n.getIsRead()), n.getCreatedAt()))
                        .toList(),
                nextCursor);
    }

    public NotificationResponses.UnreadCount unreadCount(Long userId) {
        return new NotificationResponses.UnreadCount(notificationMapper.countUnreadByUserId(userId));
    }

    /** 남의 알림을 읽음 처리하지 못하도록 수신자를 확인한다. */
    @Transactional
    public NotificationResponses.Read markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.findById(notificationId);
        if (notification == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");
        }
        Member receiver = memberAccess.requireMemberById(notification.getReceiverMemberId());
        if (!receiver.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인의 알림만 읽음 처리할 수 있습니다.");
        }

        notificationMapper.markAsRead(notificationId);
        return new NotificationResponses.Read(notificationId, true);
    }
}
