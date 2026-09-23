package com.together.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 미션 기록의 이모지별 리액션 개수. {@code GET /mission-logs/{id}/reactions} 의
 * {@code reactionCounts} 맵을 만들기 위한 중간 형태다 — 맵으로 접는 것은 서비스가 한다.
 */
@Getter
@Setter
public class ReactionCount {

    private String reactionType;
    private Integer count;
}
