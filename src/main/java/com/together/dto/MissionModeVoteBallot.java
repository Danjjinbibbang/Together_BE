package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * MISSION_MODE_VOTE_BALLOT 테이블 행. 한 사람이 한 투표에 한 표다.
 *
 * <p>마음을 바꾸면 행이 늘지 않고 {@code choice} 가 바뀐다 — PK(voteId, memberId)가 중복 투표를 막는다.
 */
@Getter
@Setter
public class MissionModeVoteBallot {

    private Long voteId;
    private Long memberId;
    /** FIXED / AI */
    private String choice;
    private LocalDateTime votedAt;
}
