package com.together.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * DAILY_MISSION_ASSIGNMENT 테이블 행. 오늘의 미션 배정(FR-010).
 *
 * <p>배정은 챌린지당 하루 1건이다. 멤버별로 다른 미션이 뽑히지 않으므로 Member 가 아니라
 * Challenge 를 참조한다.
 */
@Getter
@Setter
public class DailyMissionAssignment {

    private Long id;
    private Long challengeId;
    private Long missionId;
    private LocalDate assignedDate;
    /**
     * 배정 시점에 미션의 보상 범위에서 뽑아 고정한 금액. 같은 날 같은 챌린지의 멤버는 모두 이 금액을 받는다.
     *
     * <p>오늘의 미션 조회 응답에는 담지 않는다 — 정확한 금액은 미션 완료 후에 처음 공개된다.
     */
    private Integer rewardAmount;
    private LocalDateTime createdAt;
}
