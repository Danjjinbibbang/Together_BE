package com.together.dto.request;

import com.together.dto.NotificationType;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * {@code PATCH /challenges/{challengeId}/members/me}. 필드가 모두 선택적이다.
 *
 * <p>{@code themeColor} 와 {@code notificationSettings} 는 캘린더·알림 설정 화면에서 호출하는
 * 개인 설정으로 팀원에게 노출되지 않는다(FR-041, FR-042).
 */
public record MemberUpdateRequest(
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        @Size(max = 20, message = "테마 색상 값이 너무 깁니다.")
        String themeColor,

        /**
         * 알림 종류별 on/off. 보낸 종류만 반영되고 나머지는 그대로 둔다.
         *
         * <p>예: {@code {"MISSION_REWARD": false}} 를 보내면 입금 알림만 끄고 나머지는 유지된다.
         * 모르는 종류 이름을 보내면 역직렬화 단계에서 400 이 난다.
         */
        Map<NotificationType, Boolean> notificationSettings
) {
}
