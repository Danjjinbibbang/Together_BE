package com.together.service;

import com.together.dto.response.MissionResponses;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 임시 구현. 실제 LLM 연동 대신 테마를 끼운 템플릿을 돌려준다.
 *
 * <p><b>아직 AI 가 아니다.</b> 어떤 모델을 쓸지 정해지지 않아 연동을 만들지 않았고, 대신 API 계약이
 * 살아 있어야 프론트(화면 24)가 붙을 수 있으므로 같은 모양의 응답을 내는 자리를 채워 뒀다.
 * 모델이 정해지면 {@link MissionSuggestionGenerator} 를 구현한 빈으로 갈아끼우면 되고,
 * 호출하는 서비스는 바뀌지 않는다.
 */
@Component
public class TemplateMissionSuggestionGenerator implements MissionSuggestionGenerator {

    @Override
    public List<MissionResponses.AiSuggestions.Suggestion> generate(String theme) {
        return List.of(
                new MissionResponses.AiSuggestions.Suggestion(
                        "오늘 '%s' 관련해서 한 일을 사진으로 인증하기".formatted(theme),
                        "PHOTO", 500, 3000),
                new MissionResponses.AiSuggestions.Suggestion(
                        "'%s'에 대해 오늘 느낀 점 한 줄 남기기".formatted(theme),
                        "TEXT", 100, 1000),
                new MissionResponses.AiSuggestions.Suggestion(
                        "내일 '%s'을(를) 위해 할 일 3가지 적기".formatted(theme),
                        "TEXT", 100, 1500));
    }
}
