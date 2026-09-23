package com.together.common.kakao;

import com.together.common.exception.BusinessException;
import com.together.config.KakaoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * FR-030 카카오 인가코드 → 액세스 토큰 교환 및 사용자 정보 조회.
 *
 * <p>NFR-005에 따라 카카오 토큰과 개인정보는 백엔드 밖으로 나가지 않는다. 프론트에는 자체 JWT 만 준다.
 *
 * <p>카카오 쪽 실패는 502로 올린다 — 우리 잘못이 아니라 외부 연동 실패라는 것을 프론트가 구분해야
 * 재시도 안내를 할 수 있기 때문이다(API 명세서의 {@code /auth/kakao} 응답 코드).
 */
@Component
public class KakaoClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoClient.class);

    /** 설정 파일의 자리표시자. 실수로 그대로 두면 카카오가 알아보기 힘든 오류를 돌려주므로 미설정으로 본다. */
    private static final String PLACEHOLDER = "CHANGE_ME";

    private final KakaoProperties properties;
    private final RestClient restClient;

    @Autowired
    public KakaoClient(KakaoProperties properties) {
        this(properties, RestClient.create());
    }

    /**
     * 테스트에서 목 서버에 붙인 RestClient 를 넣기 위한 생성자.
     *
     * <p>카카오는 우리가 부르는 유일한 외부 서버라, 요청을 어떤 모양으로 보내는지(폼 필드·헤더)와
     * 실패를 502 로 바꾸는지를 확인하려면 HTTP 를 가로챌 수 있어야 한다. 운영 경로는 위 생성자다.
     */
    KakaoClient(KakaoProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }
    /**
     * 인가코드를 카카오 액세스 토큰으로 교환한다.
     *
     * <p>{@code client_id} 에는 <b>REST API 키</b>를 쓴다. 프론트가 인가 URL 로 리다이렉트할 때 쓰는
     * 키와 같은 값이다(JavaScript 키가 아니다).
     */
    public String exchangeAccessToken(String authorizationCode) {
        if (isUnset(properties.clientId())) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "카카오 앱이 아직 설정되지 않았습니다. app.kakao.client-id 에 REST API 키를 넣어주세요.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", authorizationCode);
        // 카카오 콘솔에서 Client Secret 을 켠 앱만 필요하다. 켜지 않았으면 보내면 안 된다.
        if (!isUnset(properties.clientSecret())) {
            form.add("client_secret", properties.clientSecret());
        }

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "카카오 토큰 응답이 비어 있습니다.");
            }
            return response.accessToken();
        } catch (HttpStatusCodeException e) {
            // 카카오가 실패 사유를 본문에 담아준다(KOE320 인가코드 만료·재사용, redirect_uri 불일치 등).
            // 앱 등록이 잘못됐을 때 이 본문이 없으면 원인을 찾기가 매우 어려우므로 로그에 남긴다.
            log.warn("카카오 토큰 교환 실패 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "카카오 로그인에 실패했습니다. 다시 시도해 주세요.");
        } catch (RestClientException e) {
            log.warn("카카오 토큰 교환 실패: {}", e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "카카오 로그인에 실패했습니다. 다시 시도해 주세요.");
        }
    }

    /** 액세스 토큰으로 카카오 사용자 정보를 조회한다. */
    public KakaoUserResponse fetchUser(String kakaoAccessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(properties.userInfoUri())
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id() == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보를 받지 못했습니다.");
            }
            return response;
        } catch (HttpStatusCodeException e) {
            log.warn("카카오 사용자 정보 조회 실패 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 조회에 실패했습니다.");
        } catch (RestClientException e) {
            log.warn("카카오 사용자 정보 조회 실패: {}", e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 조회에 실패했습니다.");
        }
    }

    /** 비어 있거나 설정 파일의 자리표시자 그대로면 미설정으로 본다. */
    private static boolean isUnset(String value) {
        return !StringUtils.hasText(value) || PLACEHOLDER.equals(value.trim());
    }
}
