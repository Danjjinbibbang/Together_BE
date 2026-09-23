package com.together.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code GET /me/calendar} 한 줄. 하루 × 챌린지 하나가 한 행이다(화면 16).
 *
 * <p>캘린더의 날짜 점은 챌린지 색으로 찍히므로(FR-041) 날짜만이 아니라 챌린지까지 쪼개서 집계한다.
 * 하루에 두 챌린지를 수행했다면 두 행이 나오고, 서비스가 날짜로 묶어 내려준다.
 */
@Getter
@Setter
public class CalendarEntry {

    private LocalDate activityDate;
    private Long challengeId;
    private String challengeTitle;
    /** 그 챌린지에서 본인이 지정한 색. 미지정이면 null — 기본색은 프론트가 정한다. */
    private String themeColor;
    private Integer missionCount;
    /** 그날 그 챌린지에서 실제 지급된 금액 합계. 미지급이면 0. */
    private Long rewardAmount;
}
