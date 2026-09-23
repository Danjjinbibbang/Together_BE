package com.together.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.together.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * FR-035 리프레시 토큰. 회전과 재사용 감지가 실제 요청에서 도는지 본다.
 *
 * <p>로그인({@code POST /auth/kakao})은 카카오 서버가 필요해 여기서 태우지 않는다. 발급 이후의
 * 규칙만 확인하면 되므로 토큰은 서비스로 직접 만든다.
 */
class AuthTokenAcceptanceTest extends AcceptanceTestSupport {

    @Autowired private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("리프레시하면 액세스·리프레시 토큰이 둘 다 새로 나온다")
    void refreshRotatesBothTokens() throws Exception {
        Long userId = newUser();
        String issued = refreshTokenService.issue(userId);

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        // 액세스 토큰이 이미 만료된 상태에서 부르는 API 라 인증 헤더가 없다
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", issued)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.accessTokenExpiresIn").isNumber())
                .andReturn();

        String rotated = extract(result, "refreshToken");
        org.assertj.core.api.Assertions.assertThat(rotated).isNotEqualTo(issued);

        // 새 액세스 토큰으로 보호된 API 가 열린다
        String accessToken = extract(result, "accessToken");
        mockMvc.perform(get("/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("한 번 쓴 리프레시 토큰을 다시 쓰면 401 이고, 그 사용자의 세션이 전부 끊긴다")
    void reusedTokenRevokesEverything() throws Exception {
        Long userId = newUser();
        String first = refreshTokenService.issue(userId);

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", first)))
                .andExpect(status().isOk())
                .andReturn();
        String rotated = extract(result, "refreshToken");

        // 정상 흐름이라면 한 번 쓴 토큰이 다시 올 수 없다 — 탈취 신호로 본다
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", first)))
                .andExpect(status().isUnauthorized());

        // 그래서 방금 회전해 받은 멀쩡한 토큰까지 함께 죽는다
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", rotated)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 그 리프레시 토큰으로는 갱신되지 않는다")
    void logoutRevokesToken() throws Exception {
        Long userId = newUser();
        String issued = refreshTokenService.issue(userId);

        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", issued)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", issued)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("없는 리프레시 토큰은 401 이다")
    void unknownTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", "not-a-real-token")))
                .andExpect(status().isUnauthorized());
    }

    /** 응답 JSON 에서 문자열 필드 하나를 꺼낸다. 테스트가 Jackson 빈에 기대지 않게 하려는 것이다. */
    private String extract(MvcResult result, String field) throws Exception {
        String body = result.getResponse().getContentAsString();
        String marker = "\"" + field + "\":\"";
        int from = body.indexOf(marker) + marker.length();
        return body.substring(from, body.indexOf('"', from));
    }
}
