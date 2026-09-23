package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * NOTIFICATION 테이블 행. 알림(FR-020, FR-021, FR-012a).
 *
 * <p>대상을 {@code targetType} + {@code targetId} 다형 참조로 가리킨다. DB 레벨 FK 가 없으므로
 * 대상이 실재하는지는 서비스가 보장해야 한다. 프론트는 {@code targetType} 으로 이동 화면을 분기한다.
 */
@Getter
@Setter
public class Notification {

    private Long id;
    private Long receiverMemberId;
    /** API 응답 필드명은 type. 예: MISSION_REWARD */
    private String notiType;
    /** 알림 문구 */
    private String content;
    /** 예: MISSION_LOG / CHALLENGE */
    private String targetType;
    private Long targetId;
    /** 'Y' / 'N' */
    private String isRead;
    private LocalDateTime createdAt;
}
