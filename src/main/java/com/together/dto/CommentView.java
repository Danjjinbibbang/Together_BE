package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * {@code GET /mission-logs/{missionLogId}/comments} 한 줄. 댓글에 작성자 닉네임을 조인한 결과다.
 */
@Getter
@Setter
public class CommentView {

    private Long commentId;
    private Long memberId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
}
