package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * USERS 테이블 행. 카카오 로그인으로 생성되는 전역 계정(FR-030).
 *
 * <p>닉네임은 여기가 아니라 MEMBER 에 있다. 챌린지마다 다른 닉네임을 쓸 수 있기 때문이다.
 */
@Getter
@Setter
public class User {

    private Long id;
    private String kakaoId;
    private String email;
    private LocalDateTime termsAgreedAt;
    private String termsVersion;
    /**
     * FR-043 계정 탈퇴 시각. null 이면 활성 계정이다.
     *
     * <p>행을 지우지 않는 것은 챌린지 탈퇴(FR-038)와 같은 이유다 — MEMBER 를 참조하는
     * 과거 기록(미션 로그·거래·댓글)이 남아야 팀 기록이 깨지지 않는다.
     */
    private LocalDateTime withdrawnAt;

    private LocalDateTime createdAt;
}
