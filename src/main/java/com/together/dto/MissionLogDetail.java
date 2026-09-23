package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code GET /mission-logs/{missionLogId}} 용. 기록에 미션 제목·작성자 닉네임·챌린지를 조인한 결과다.
 *
 * <p>비공개 기록을 작성자 본인이 아닌 사람이 볼 때 본문/첨부를 가리는 판단은 서비스가 한다.
 */
@Getter
@Setter
public class MissionLogDetail {

    private Long missionLogId;
    private Long assignmentId;
    private String missionTitle;
    private Long challengeId;
    private Long memberId;
    private String nickname;
    private String submitContent;
    private String attachmentUrl;
    /** 'Y' / 'N' */
    private String isPublic;
    private String status;
    /** AI / OWNER */
    private String reviewedBy;
    private String rejectReasonCode;
    private Integer rewardAmount;
    private LocalDateTime createdAt;
}
