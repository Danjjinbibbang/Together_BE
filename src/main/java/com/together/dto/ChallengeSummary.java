package com.together.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code GET /challenges} 한 줄. 챌린지 기본 정보에 집계값과 본인 설정을 얹은 조회 전용 객체다.
 *
 * <p>{@code currentBalance} 는 이 챌린지 소속 계좌 잔액 합, {@code themeColor} 는 요청자 본인 것이다.
 */
@Getter
@Setter
public class ChallengeSummary {

    private Long challengeId;
    private String title;
    /** CHALLENGE.TARGET_AMOUNT */
    private Long goalAmount;
    private Long currentBalance;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer memberCount;
    private String themeColor;
}
