package com.together.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 멤버 정보와 계좌 잔액을 함께 읽는 조회 전용 객체.
 *
 * <p>{@code GET /challenges/{id}/members} 와 {@code GET /challenges/{id}/accounts} 가 같이 쓴다.
 * 계좌 목록 응답에는 {@code role}/{@code status} 를 내리지 않는다 — 서비스에서 걸러 담는다.
 */
@Getter
@Setter
public class MemberBalance {

    private Long memberId;
    private String nickname;
    /** OWNER / MEMBER */
    private String role;
    /** ACTIVE / LEFT */
    private String status;
    private Long balance;
}
