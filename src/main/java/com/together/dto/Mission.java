package com.together.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * MISSION 테이블 행. 미션 마스터(FR-007 ~ FR-010).
 *
 * <p>{@code challengeId} 가 NULL 이면 전역 공용 고정 풀, 값이 있으면 해당 챌린지 전용 미션이다.
 * 챌린지 전용 미션은 방장이 직접 만들거나(OWNER) AI 제안을 골라 등록한다(AI).
 */
@Getter
@Setter
public class Mission {

    private Long id;
    /** NULL = 전역 고정 풀 */
    private Long challengeId;
    private String title;
    /** TEXT / PHOTO */
    private String submitType;
    /**
     * 보상 하한(FR-013, 100원 이상). 실제 지급액은 배정 시점에 하한~상한 사이에서 한 번만 뽑혀
     * {@link DailyMissionAssignment#getRewardAmount()} 에 저장된다.
     */
    private Integer rewardMin;
    /** 보상 상한(10,000원 이하) */
    private Integer rewardMax;
    /** TEXT 미션 자동완료 기준 글자수. PHOTO 면 NULL */
    private Integer minTextLength;
    /** OWNER / AI. 전역 고정 풀은 NULL */
    private String createdByType;
    private Long createdByMemberId;
    /** 'Y' / 'N' */
    private String isActive;
}
