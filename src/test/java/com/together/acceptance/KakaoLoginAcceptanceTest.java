package com.together.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.together.common.exception.BusinessException;
import com.together.common.kakao.KakaoClient;
import com.together.common.kakao.KakaoUserResponse;
import com.together.dto.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

/**
 * FR-029 ~ FR-032. 카카오 로그인 <b>이후</b> 우리가 하는 일 — 가입 분기, 자체 JWT 발급,
 * 리프레시 토큰 발급, 약관 동의 진입.
 *
 * <p>카카오 서버는 {@link KakaoClient} 를 갈아끼워 대신한다. 인가코드는 일회용이라 자동화된
 * 테스트에서 진짜 카카오를 부를 수 없고, 부를 수 있더라도 남의 서버 상태에 테스트가 매달리게 된다.
 * 카카오와 주고받는 <b>요청 모양</b>은 {@code KakaoClientTest} 가 따로 본다.
 */
class KakaoLoginAcceptanceTest extends AcceptanceTestSupport {

    @MockitoBean private KakaoClient kakaoClient;

    private static KakaoUserResponse kakaoUser(long kakaoId, String email) {
        return new KakaoUserResponse(kakaoId, new KakaoUserResponse.KakaoAccount(email));
    }

    @Test
    @DisplayName("처음 보는 카카오 계정이면 가입까지 하고 isNewUser=true 로 알려준다")
    void firstLoginCreatesUser() throws Exception {
        long kakaoId = 3_800_000_000L + System.nanoTime() % 1_000_000L;
        given(kakaoClient.exchangeAccessToken(anyString())).willReturn("kakao-access-token");
        given(kakaoClient.fetchUser("kakao-access-token"))
                .willReturn(kakaoUser(kakaoId, "tester@example.com"));

        MvcResult result = mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("authorizationCode", "code-from-kakao")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNewUser").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                // FR-035 로 리프레시 토큰이 함께 나간다
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.accessTokenExpiresIn").isNumber())
                .andExpect(jsonPath("$.user.kakaoId").value(String.valueOf(kakaoId)))
                .andReturn();

        User saved = userMapper.findByKakaoId(String.valueOf(kakaoId));
        assertThat(saved).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("tester@example.com");
        // 약관 동의는 아직이다 — 프론트는 isNewUser 로 화면 1 로 보낸다(FR-032)
        assertThat(saved.getTermsAgreedAt()).isNull();

        // 받은 토큰이 실제로 보호된 API 를 연다
        String accessToken = extract(result, "accessToken");
        mockMvc.perform(post("/auth/terms-agreement")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("termsVersion", "1.0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termsVersion").value("1.0"));

        assertThat(userMapper.findById(saved.getId()).getTermsAgreedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 카카오 계정으로 다시 로그인하면 기존 사용자로 붙고 isNewUser=false 다")
    void secondLoginReusesUser() throws Exception {
        long kakaoId = 3_900_000_000L + System.nanoTime() % 1_000_000L;
        given(kakaoClient.exchangeAccessToken(anyString())).willReturn("kakao-access-token");
        given(kakaoClient.fetchUser(anyString())).willReturn(kakaoUser(kakaoId, "tester@example.com"));

        mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("authorizationCode", "first-code")))
                .andExpect(jsonPath("$.isNewUser").value(true));

        Long userId = userMapper.findByKakaoId(String.valueOf(kakaoId)).getId();

        mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("authorizationCode", "second-code")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNewUser").value(false))
                .andExpect(jsonPath("$.user.userId").value(userId));
    }

    @Test
    @DisplayName("로그인으로 받은 리프레시 토큰이 그대로 갱신에 쓰인다")
    void issuedRefreshTokenWorks() throws Exception {
        long kakaoId = 3_700_000_000L + System.nanoTime() % 1_000_000L;
        given(kakaoClient.exchangeAccessToken(anyString())).willReturn("kakao-access-token");
        given(kakaoClient.fetchUser(anyString())).willReturn(kakaoUser(kakaoId, null));

        MvcResult login = mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("authorizationCode", "code-from-kakao")))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", extract(login, "refreshToken"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("이메일 동의를 안 한 계정도 가입된다")
    void emailIsOptional() throws Exception {
        long kakaoId = 3_600_000_000L + System.nanoTime() % 1_000_000L;
        given(kakaoClient.exchangeAccessToken(anyString())).willReturn("kakao-access-token");
        given(kakaoClient.fetchUser(anyString())).willReturn(kakaoUser(kakaoId, null));

        mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("authorizationCode", "code-from-kakao")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNewUser").value(true));

        assertThat(userMapper.findByKakaoId(String.valueOf(kakaoId)).getEmail()).isNull();
    }

    @Test
    @DisplayName("카카오 연동이 실패하면 502 다 — 우리 잘못과 구분되게")
    void kakaoFailureIsBadGateway() throws Exception {
        willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "카카오 로그인에 실패했습니다."))
                .given(kakaoClient).exchangeAccessToken(any());

        mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("authorizationCode", "used-or-expired-code")))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("인가코드가 없으면 400 이고 카카오를 부르지 않는다")
    void missingAuthorizationCodeIsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String extract(MvcResult result, String field) throws Exception {
        String body = result.getResponse().getContentAsString();
        String marker = "\"" + field + "\":\"";
        int from = body.indexOf(marker) + marker.length();
        return body.substring(from, body.indexOf('"', from));
    }
}
