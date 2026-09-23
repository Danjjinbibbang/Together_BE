package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 팀 응원 메시지 목록 한 줄. 보낸 사람 닉네임을 조인해 붙인다.
 *
 * <p>닉네임은 Member 에 있어 챌린지 밖 신원과 엮이지 않는다. 탈퇴자가 보낸 메시지도 그대로 남는다
 * (과거 기록 보존 원칙).
 */
@Getter
@Setter
public class TeamMessageView {

    private Long messageId;
    private Long senderMemberId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    /** 수정된 적이 있으면 그 시각. 프론트는 이 값으로 "수정됨" 표기를 그리면 된다. */
    private LocalDateTime updatedAt;
}
