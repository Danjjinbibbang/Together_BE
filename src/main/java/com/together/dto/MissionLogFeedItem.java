package com.together.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code GET /challenges/{id}/mission-logs} 한 줄. 캘린더(화면 16)와 팀 활동 피드(FR-039)가 쓴다.
 *
 * <p>본문 전체가 아니라 미리보기만 담는다. 비공개 기록이면 서비스가 미리보기를 지우고 내려준다.
 */
@Getter
@Setter
public class MissionLogFeedItem {

    private Long missionLogId;
    private Long assignmentId;
    private String missionTitle;
    private LocalDate assignedDate;
    private Long memberId;
    private String nickname;
    private String status;
    /** AI / OWNER */
    private String reviewedBy;
    private Integer rewardAmount;
    /** 'Y' / 'N' */
    private String isPublic;
    private String submitContentPreview;
}
