package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * REACTION 테이블 행. 이모지 리액션(FR-022).
 *
 * <p>멤버당 미션기록 1개만 존재한다. 같은 타입을 다시 누르면 삭제, 다른 타입을 누르면 교체다.
 */
@Getter
@Setter
public class Reaction {

    private Long id;
    private Long missionLogId;
    private Long senderMemberId;
    /** 예: CLAP / FIRE */
    private String reactionType;
    private LocalDateTime createdAt;
}
