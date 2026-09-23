package com.together.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code GET /me/mission-logs} 한 줄(FR-040, 화면 16에서 날짜를 눌렀을 때).
 *
 * <p>{@link MissionLogFeedItem} 과 달리 챌린지를 가로지르므로 소속 챌린지를 함께 담는다.
 * 대신 작성자는 항상 본인이라 닉네임이 없고, 댓글·리액션 수도 넣지 않는다(FR-040 정보 과다 방지).
 */
@Getter
@Setter
public class MyMissionLogItem {

    private Long missionLogId;
    private Long challengeId;
    private String challengeTitle;
    private String themeColor;
    private String missionTitle;
    private LocalDate assignedDate;
    private String status;
    /** AI / OWNER */
    private String reviewedBy;
    private Integer rewardAmount;
    /** 'Y' / 'N' */
    private String isPublic;
}
