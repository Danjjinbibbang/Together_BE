package com.together.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Notion API 명세서(54개)와 실제 등록된 라우팅이 일치하는지 본다.
 *
 * <p>경로 오타나 메서드 착오는 컴파일도 테스트도 통과해 버리고 프론트가 404를 받고 나서야 드러난다.
 * 계약이 레포 밖(Notion)에 있어 더 그렇다. 그래서 목록을 코드에 박아 두고 대조한다.
 * 명세에 엔드포인트가 추가되면 이 목록도 함께 고쳐야 한다.
 */
@SpringBootTest
class ApiSurfaceTest {

    /** API 명세서의 Method + Path 를 그대로 옮긴 것이다. */
    private static final List<String> SPEC_ENDPOINTS = List.of(
            // Auth
            "POST /auth/kakao",
            "POST /auth/terms-agreement",
            "POST /auth/refresh",
            "POST /auth/logout",
            "POST /auth/kakao/unlink-callback",
            // Challenge
            "GET /challenges",
            "POST /challenges",
            "GET /challenges/{challengeId}",
            "PATCH /challenges/{challengeId}",
            "GET /challenges/{challengeId}/invite-code",
            "GET /challenges/by-invite-code/{inviteCode}",
            "POST /challenges/join",
            "PATCH /challenges/{challengeId}/mission-mode",
            "POST /challenges/{challengeId}/mission-mode/vote",
            "GET /challenges/{challengeId}/mission-mode/vote",
            "POST /challenges/{challengeId}/mission-mode/vote/ballots",
            "POST /challenges/{challengeId}/mission-mode/vote/close",
            "PATCH /challenges/{challengeId}/owner",
            // Member
            "GET /challenges/{challengeId}/members",
            "GET /challenges/{challengeId}/members/me",
            "PATCH /challenges/{challengeId}/members/me",
            "DELETE /challenges/{challengeId}/members/me",
            // Account
            "GET /challenges/{challengeId}/accounts",
            "GET /challenges/{challengeId}/accounts/me",
            "GET /challenges/{challengeId}/accounts/me/transactions",
            // Mission
            "GET /challenges/{challengeId}/missions",
            "POST /challenges/{challengeId}/missions",
            "POST /challenges/{challengeId}/missions/ai-generate",
            "GET /challenges/{challengeId}/missions/today",
            "PATCH /missions/{missionId}",
            "DELETE /missions/{missionId}",
            "POST /missions/{missionId}/submit",
            "GET /challenges/{challengeId}/mission-logs",
            "GET /mission-logs/{missionLogId}",
            "POST /mission-logs/{missionLogId}/dispute",
            "PATCH /mission-logs/{missionLogId}/dispute/recheck",
            "PATCH /mission-logs/{missionLogId}/dispute/resubmit",
            "POST /mission-logs/{missionLogId}/review",
            "POST /mission-logs/{missionLogId}/review/auto-approve",
            // Me (내 기록 캘린더, 화면 16)
            "GET /me/calendar",
            "GET /me/mission-logs",
            // Notification
            "GET /notifications",
            "GET /notifications/unread-count",
            "PATCH /notifications/{notificationId}/read",
            // Social
            "GET /mission-logs/{missionLogId}/comments",
            "POST /mission-logs/{missionLogId}/comments",
            "DELETE /comments/{commentId}",
            "GET /mission-logs/{missionLogId}/reactions",
            "POST /mission-logs/{missionLogId}/reactions",
            "POST /challenges/{challengeId}/messages",
            "GET /challenges/{challengeId}/messages",
            "PATCH /challenges/{challengeId}/messages/{messageId}",
            "DELETE /challenges/{challengeId}/messages/{messageId}",
            // User
            "DELETE /users/me");

    // springdoc 이 자기 매핑용 빈을 하나 더 등록하므로 MVC 표준 빈을 이름으로 집는다.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("API 명세서의 54개 엔드포인트가 모두 등록돼 있다")
    void allSpecEndpointsAreMapped() {
        Set<String> registered = registeredEndpoints();

        assertThat(SPEC_ENDPOINTS).hasSize(54);
        assertThat(registered).containsAll(SPEC_ENDPOINTS);
    }

    @Test
    @DisplayName("명세에 없는 엔드포인트를 임의로 열어두지 않았다")
    void noUndocumentedEndpoints() {
        Set<String> registered = registeredEndpoints().stream()
                // 프레임워크가 제공하는 경로(문서·헬스체크·기본 에러 핸들러)는 업무 API 가 아니다
                .filter(e -> !e.contains("/v3/api-docs")
                        && !e.contains("/swagger-ui")
                        && !e.contains("/actuator")
                        && !e.endsWith(" /error"))
                .collect(Collectors.toSet());

        assertThat(registered).containsExactlyInAnyOrderElementsOf(SPEC_ENDPOINTS);
    }

    private Set<String> registeredEndpoints() {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(ApiSurfaceTest::describe)
                .collect(Collectors.toSet());
    }

    private static java.util.stream.Stream<String> describe(RequestMappingInfo info) {
        var patterns = info.getPathPatternsCondition() == null
                ? Set.<String>of()
                : info.getPathPatternsCondition().getPatternValues();
        var methods = info.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            return patterns.stream().map(p -> "ANY " + p);
        }
        return methods.stream().flatMap(m -> patterns.stream().map(p -> m.name() + " " + p));
    }
}
