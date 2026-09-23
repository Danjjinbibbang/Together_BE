package com.together.mapper;

import com.together.dto.Notification;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * NOTIFICATION 테이블 접근. {@code resources/mapper/NotificationMapper.xml} 과 1:1 대응한다.
 */
public interface NotificationMapper {

    Notification findById(@Param("id") Long id);

    /**
     * 알림함(FR-021)과 상단바 배지는 특정 챌린지가 아니라 전역 기준이다. 한 사람이 여러 챌린지에
     * 속하고 알림은 챌린지별 MEMBER 에 달리므로, 사용자에 속한 모든 MEMBER 의 알림을 모은다.
     *
     * <p>커서 방식이다. 알림은 계속 앞에 쌓여 오프셋으로는 중복·누락이 생긴다.
     *
     * @param cursor 이 ID 보다 작은 것만. 첫 페이지는 {@code null}
     * @param limit  다음 페이지 존재 여부를 알려면 서비스가 필요한 개수보다 1 크게 넘긴다
     */
    List<Notification> findByUserId(
            @Param("userId") Long userId, @Param("cursor") Long cursor, @Param("limit") int limit);

    /** 상단바 배지용 미확인 개수(전역). */
    int countUnreadByUserId(@Param("userId") Long userId);

    List<Notification> findByReceiverMemberId(@Param("receiverMemberId") Long receiverMemberId);

    int insert(Notification notification);

    int markAsRead(@Param("id") Long id);

    /**
     * 대상 하나에 매달린 알림들의 본문을 한 번에 바꾼다(FR-044 메시지 수정).
     *
     * <p>알림 본문은 발송 시점 스냅샷이라 원본만 고쳐서는 팀원 알림함에 반영되지 않는다.
     * 읽음 상태는 건드리지 않는다 — 오타 하나 고쳤다고 팀 전체 뱃지가 다시 켜지면 안 된다.
     */
    int updateContentByTarget(@Param("targetType") String targetType,
                              @Param("targetId") Long targetId,
                              @Param("content") String content);

    /** 대상이 사라졌을 때 그 알림들도 함께 지운다(FR-044 메시지 삭제). */
    int deleteByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);
}
