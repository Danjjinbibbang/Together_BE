package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * TEAM_MESSAGE 테이블 행. 팀원이 챌린지 팀 전체에 보내는 한 줄 응원(FR-044).
 *
 * <p>알림은 받는 사람 수만큼 복제되지만 <b>메시지 원본은 이 행 하나</b>다. 목록 조회와 도배 카운트가
 * 모두 이 행을 기준으로 한다.
 */
@Getter
@Setter
public class TeamMessage {

    private Long id;
    private Long challengeId;
    private Long senderMemberId;
    private String content;
    private LocalDateTime createdAt;

    /** 마지막 수정 시각. 한 번도 고치지 않았으면 null. */
    private LocalDateTime updatedAt;

    /**
     * 삭제 시각(soft delete). null 이면 살아 있는 메시지다.
     *
     * <p>알림함에서는 흔적 없이 사라지지만(알림 행은 실제로 지운다) 원본은 남긴다 —
     * 지우고 다시 보내는 식으로 하루 발송 한도를 우회하지 못하게 하려는 것이다.
     */
    private LocalDateTime deletedAt;
}
