package com.together.mapper;

import com.together.dto.TeamMessage;
import com.together.dto.TeamMessageView;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * TEAM_MESSAGE 테이블 접근(FR-044).
 * {@code resources/mapper/TeamMessageMapper.xml} 과 1:1 대응한다.
 */
public interface TeamMessageMapper {

    TeamMessage findById(@Param("id") Long id);

    /**
     * 팀 응원 메시지 목록. 커서 방식이다 — 새 메시지가 계속 앞에 끼어드는 목록이라
     * 오프셋으로는 중복·누락이 생긴다(알림함·피드와 같은 이유).
     *
     * @param cursor 이 ID 보다 작은 것만. 첫 페이지는 {@code null}
     * @param limit  다음 페이지 존재 여부를 알려면 서비스가 필요한 개수보다 1 크게 넘긴다
     */
    List<TeamMessageView> findViewsByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /** 도배 방지용. 한 멤버가 {@code from} 이후로 보낸 건수를 센다. */
    int countBySenderSince(@Param("senderMemberId") Long senderMemberId,
                           @Param("from") LocalDateTime from);

    /**
     * 본문을 고친다. 이미 지워진 메시지는 건드리지 않는다 — 갱신 건수 0 이 그 신호다.
     *
     * <p>알림(NOTIFICATION.CONTENT)은 여기서 바뀌지 않는다. 발송 시점 스냅샷이라 서비스가
     * 같은 트랜잭션에서 따로 갱신한다.
     */
    int updateContent(@Param("id") Long id, @Param("content") String content);

    /** soft delete. 이미 지워진 메시지면 0 을 돌려준다. */
    int softDelete(@Param("id") Long id);

    int insert(TeamMessage message);
}
