package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * MISSION_MODE_VOTE 테이블 행. 미션 운영 방식을 팀원 투표로 정하는 과정(FR-008 확장)이다.
 *
 * <p>확정된 결과는 CHALLENGE.MISSION_MODE 에 반영되므로 이 행은 "어떻게 정해졌는가"만 남긴다.
 * 챌린지당 열린 투표는 하나뿐이고(UX_MMV_OPEN_PER_CHALLENGE), 닫힌 투표는 이력으로 쌓인다.
 */
@Getter
@Setter
public class MissionModeVote {

    private Long id;
    private Long challengeId;
    /** OPEN / CLOSED */
    private String status;
    /** 이 시각이 지나면 조회·투표 시점에 자동으로 마감된다. */
    private LocalDateTime deadline;
    /** DECIDED / TIE / NOT_ENOUGH_VOTES. 열려 있는 동안에는 null */
    private String outcome;
    /** DECIDED 일 때만. FIXED / AI */
    private String resultMode;
    private Long openedByMemberId;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
