package com.together.service;

import com.together.dto.response.MissionResponses;
import java.util.List;

/**
 * FR-009 AI 미션 제안. 방장이 준 테마로 미션 후보를 만든다.
 *
 * <p>생성 결과는 저장하지 않는다 — 방장이 골라 미션 생성 API 로 등록하는 2단계 흐름이라,
 * 이 포트는 순수하게 후보만 돌려준다.
 */
public interface MissionSuggestionGenerator {

    List<MissionResponses.AiSuggestions.Suggestion> generate(String theme);
}
