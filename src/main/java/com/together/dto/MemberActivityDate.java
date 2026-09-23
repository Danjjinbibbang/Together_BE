package com.together.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * 스트릭(FR-025) 계산용 원자료. "누가 어느 날 미션을 성공했는가" 한 줄이다.
 *
 * <p>연속일 계산 자체는 서비스({@code StreakCalculator})가 한다 — 매퍼에 조건 분기를 넣지 않는
 * 규칙 때문이기도 하고, 날짜 연속 판정을 SQL 로 쓰면 읽기 어려워지기 때문이다.
 */
@Getter
@Setter
public class MemberActivityDate {

    private Long memberId;
    private LocalDate activityDate;
}
