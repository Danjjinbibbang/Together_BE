package com.together.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /auth/kakao/unlink-callback} — 카카오가 우리 서버로 보내는 "연결 끊기" 통보다.
 *
 * <p>필드명이 snake_case 인 것은 카카오가 그렇게 보내기 때문이다. 우리 API 의 요청 바디 규칙
 * (camelCase)과 다르지만, 남의 스펙이라 맞추는 쪽이 맞다.
 *
 * @param appId        카카오 앱 ID
 * @param userId       카카오 회원번호. 우리 USERS.KAKAO_ID 와 대조한다
 * @param referrerType UNLINK_FROM_APPS(카카오 계정 설정에서 해제) 등. 기록용으로만 받는다
 */
public record KakaoUnlinkCallbackRequest(
        @JsonProperty("app_id") Long appId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("referrer_type") String referrerType
) {
}
