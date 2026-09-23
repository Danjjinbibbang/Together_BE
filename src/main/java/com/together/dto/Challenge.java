package com.together.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * CHALLENGE 테이블 행. 챌린지(FR-001, FR-007).
 */
@Getter
@Setter
public class Challenge {

    private Long id;
    private String title;
    private Long targetAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String inviteCode;
    /** FIXED(고정 풀) / AI(AI 추천) */
    private String missionMode;
    private LocalDateTime createdAt;
}
