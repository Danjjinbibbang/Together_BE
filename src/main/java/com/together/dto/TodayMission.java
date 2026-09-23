package com.together.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code GET /challenges/{id}/missions/today} 용. 배정건과 미션 마스터를 조인한 결과다.
 *
 * <p>확정된 보상 금액(배정의 {@code rewardAmount})은 일부러 담지 않는다. 화면 10 카드에는
 * "100~2,000원"처럼 범위만 보여주고, 정확한 금액은 미션 완료 후 화면 12에서 처음 공개한다.
 *
 * <p>{@code mySubmissionStatus} 는 요청자가 이 배정에 남긴 기록의 상태이고, 미제출이면 null 이다.
 */
@Getter
@Setter
public class TodayMission {

    private Long assignmentId;
    private Long missionId;
    private String title;
    /** TEXT / PHOTO */
    private String submitType;
    private Integer rewardMin;
    private Integer rewardMax;
    private Integer minTextLength;
    private LocalDate assignedDate;
    private String mySubmissionStatus;
}
