package com.together.common.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 카카오 토큰 교환 응답 중 우리가 쓰는 부분만 받는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(
        @JsonProperty("access_token") String accessToken
) {
}
