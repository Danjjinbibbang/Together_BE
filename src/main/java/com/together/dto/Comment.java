package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * COMMENTS 테이블 행. 댓글(FR-023). 입금 건이 아니라 공개된 미션 상세에 작성된다.
 */
@Getter
@Setter
public class Comment {

    private Long id;
    private Long missionLogId;
    private Long senderMemberId;
    private String content;
    private LocalDateTime createdAt;
}
