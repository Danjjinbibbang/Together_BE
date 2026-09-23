package com.together.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * MEMBER_NOTIFICATION_SETTING 테이블 행. 챌린지별·종류별 알림 수신 설정.
 *
 * <p>행이 없으면 켜짐으로 본다 — 끈 것만 저장한다.
 */
@Getter
@Setter
public class MemberNotificationSetting {

    private Long memberId;
    /** {@link NotificationType} 의 이름 */
    private String notiType;
    /** 'Y' / 'N' */
    private String enabled;
}
