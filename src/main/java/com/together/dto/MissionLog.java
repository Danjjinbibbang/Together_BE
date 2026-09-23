package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * MISSION_LOG 테이블 행. 미션 수행 기록(FR-011 ~ FR-016).
 *
 * <p>미션을 직접 참조하지 않고 배정건({@link DailyMissionAssignment})을 참조한다.
 * 배정 자체가 챌린지·날짜당 1건이라 하루 1미션 제한도 여기서 따라온다.
 */
@Getter
@Setter
public class MissionLog {

    private Long id;
    private Long memberId;
    private Long assignmentId;
    private String submitContent;
    private String attachmentUrl;
    /** 실제 지급된 금액. 설정값은 {@link Mission#getRewardAmount()} 쪽이다 */
    private Integer rewardAmount;
    /** 'Y' / 'N'. FR-015 기본 공개 */
    private String isPublic;
    /**
     * FR-012 8단계. SUBMITTED → AI_APPROVED / AI_REJECTED → DISPUTE_REQUESTED →
     * RECHECK_REQUESTED / RESUBMITTED → OWNER_APPROVED / OWNER_REJECTED
     */
    private String status;
    /** 마지막으로 판정한 주체. AI / OWNER */
    private String reviewedBy;
    /** LOW_QUALITY / LOW_LIGHT / IRRELEVANT / ETC. 반려가 아니면 NULL */
    private String rejectReasonCode;
    /** FR-012b 재제출 횟수 */
    private Integer resubmitCount;

    /**
     * FR-012c 방장 판정을 기다리기 시작한 시각. 무응답 자동 승인 기한을 여기서 잰다.
     *
     * <p>이의제기·재검토요청·재제출로 대기열에 들어갈 때마다 갱신된다 — 마지막 요청 기준으로
     * 기다려야, 재제출한 건이 이전 대기 시간을 물려받아 곧바로 자동 승인되는 일이 없다.
     */
    private LocalDateTime reviewRequestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
