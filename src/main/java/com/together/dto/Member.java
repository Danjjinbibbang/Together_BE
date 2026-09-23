package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * MEMBER 테이블 행. 챌린지별 참여 정보(FR-003, FR-004, FR-006).
 *
 * <p>닉네임이 User 가 아니라 여기 있는 이유는 챌린지마다 다른 닉네임을 쓸 수 있어서다.
 * 방장 정보의 유일한 소스도 이 테이블의 {@code role} 이다 — 챌린지 테이블은 방장을 들고 있지 않다.
 * 탈퇴(FR-038)는 행을 지우지 않고 {@code status} 를 LEFT 로 바꿔 과거 활동 기록을 남긴다.
 */
@Getter
@Setter
public class Member {

    private Long id;
    private Long userId;
    private Long challengeId;
    private String nickname;
    /** OWNER(방장) / MEMBER(팀원) */
    private String role;
    /** ACTIVE / LEFT */
    private String status;
    /** FR-041 캘린더에서 챌린지를 구분하는 색상(예 '#18A8F1'). 팀원에게 보이지 않는 개인 설정 */
    private String themeColor;
    private LocalDateTime joinedAt;
}
