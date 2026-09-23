package com.together.common.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.together.common.exception.BusinessException;
import com.together.config.KakaoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * FR-030 카카오 연동. <b>실제 카카오 서버를 부르지 않고</b> 우리가 보내는 요청 모양과 실패 변환을 본다.
 *
 * <p>카카오는 우리가 부르는 유일한 외부 서버다. 여기서 틀리면 원인이 KOE 오류 코드로만 돌아와
 * 찾기가 어렵다 — 폼 필드 이름 하나, 헤더 하나가 어긋나는 것이 대표적인 실패다.
 */
class KakaoClientTest {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private MockRestServiceServer server;

    private KakaoClient clientWith(String clientId, String clientSecret) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        KakaoProperties properties = new KakaoProperties(
                clientId, clientSecret, "http://localhost:5173/oauth/kakao/callback",
                TOKEN_URI, USER_INFO_URI, "admin-key");
        return new KakaoClient(properties, builder.build());
    }

    @Test
    @DisplayName("인가코드를 REST API 키와 함께 폼으로 보내고 액세스 토큰을 받아온다")
    void exchangesAuthorizationCode() {
        KakaoClient client = clientWith("rest-api-key", null);

        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("grant_type=authorization_code")))
                // client_id 는 JavaScript 키가 아니라 REST API 키다 — 프론트의 인가 요청과 같은 값
                .andExpect(content().string(containsString("client_id=rest-api-key")))
                .andExpect(content().string(containsString("code=auth-code-123")))
                .andExpect(content().string(containsString("redirect_uri=")))
                // Client Secret 을 켜지 않은 앱에는 보내면 안 된다
                .andExpect(content().string(not(containsString("client_secret"))))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.exchangeAccessToken("auth-code-123")).isEqualTo("kakao-access-token");
        server.verify();
    }

    @Test
    @DisplayName("Client Secret 을 켠 앱이면 함께 보낸다")
    void sendsClientSecretWhenConfigured() {
        KakaoClient client = clientWith("rest-api-key", "secret-value");

        server.expect(requestTo(TOKEN_URI))
                .andExpect(content().string(containsString("client_secret=secret-value")))
                .andRespond(withSuccess("{\"access_token\":\"t\"}", MediaType.APPLICATION_JSON));

        client.exchangeAccessToken("auth-code-123");
        server.verify();
    }

    @Test
    @DisplayName("앱을 등록하지 않았으면 카카오를 부르지도 않고 502 다")
    void failsFastWhenAppIsNotRegistered() {
        KakaoClient client = clientWith("", null);

        assertThatThrownBy(() -> client.exchangeAccessToken("auth-code-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("카카오 앱이 아직 설정되지 않았습니다");

        // 호출 자체가 없어야 한다 — 기대한 요청이 없으므로 verify 가 통과한다
        server.verify();
    }

    @Test
    @DisplayName("카카오가 4xx 로 거절하면 502 로 바꿔 올린다")
    void kakaoRejectionBecomesBadGateway() {
        KakaoClient client = clientWith("rest-api-key", null);

        // 인가코드는 일회용이라 재사용하면 KOE320 이 온다 — 가장 흔한 실패다
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_code\":\"KOE320\"}"));

        assertThatThrownBy(() -> client.exchangeAccessToken("used-code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    @DisplayName("토큰 응답이 비어 있어도 502 다")
    void emptyTokenResponseIsBadGateway() {
        KakaoClient client = clientWith("rest-api-key", null);

        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeAccessToken("auth-code-123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("카카오 토큰 응답이 비어 있습니다");
    }

    @Test
    @DisplayName("사용자 정보는 Bearer 헤더로 조회하고 회원번호·이메일을 읽는다")
    void fetchesUser() {
        KakaoClient client = clientWith("rest-api-key", null);

        server.expect(requestTo(USER_INFO_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer kakao-access-token"))
                .andRespond(withSuccess(
                        "{\"id\":3812345678,\"kakao_account\":{\"email\":\"tester@example.com\"}}",
                        MediaType.APPLICATION_JSON));

        KakaoUserResponse user = client.fetchUser("kakao-access-token");

        assertThat(user.id()).isEqualTo(3_812_345_678L);
        assertThat(user.email()).isEqualTo("tester@example.com");
        server.verify();
    }

    @Test
    @DisplayName("이메일 동의를 안 한 계정은 이메일이 없다")
    void emailIsOptional() {
        KakaoClient client = clientWith("rest-api-key", null);

        server.expect(requestTo(USER_INFO_URI))
                .andRespond(withSuccess("{\"id\":3812345678}", MediaType.APPLICATION_JSON));

        KakaoUserResponse user = client.fetchUser("kakao-access-token");

        assertThat(user.id()).isEqualTo(3_812_345_678L);
        // 동의 항목이라 거부하면 내려오지 않는다 — USERS.EMAIL 이 nullable 인 이유다
        assertThat(user.email()).isNull();
    }

    @Test
    @DisplayName("카카오 장애도 502 로 바꿔 올린다")
    void kakaoOutageBecomesBadGateway() {
        KakaoClient client = clientWith("rest-api-key", null);

        server.expect(requestTo(USER_INFO_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchUser("kakao-access-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
