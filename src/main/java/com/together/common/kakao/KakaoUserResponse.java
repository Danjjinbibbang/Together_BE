package com.together.common.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 사용자 정보 응답 중 우리가 쓰는 부분만 받는다.
 *
 * <p>{@code id} 가 카카오 회원번호로 {@code USERS.KAKAO_ID} 가 된다. 이메일은 동의 항목이라
 * 사용자가 거부하면 내려오지 않으므로 없을 수 있다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(String email) {
    }

    /** 이메일 동의를 안 했으면 {@code null}. */
    public String email() {
        return kakaoAccount == null ? null : kakaoAccount.email();
    }
}
