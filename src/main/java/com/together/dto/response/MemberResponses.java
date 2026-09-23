package com.together.dto.response;

import com.together.dto.NotificationType;
import java.util.List;
import java.util.Map;

/** Member 태그 응답 모음. */
public final class MemberResponses {

    private MemberResponses() {
    }

    /**
     * {@code GET /challenges/{challengeId}/members}
     *
     * <p>{@code themeColor} 는 본인만 보는 개인 설정이라 이 목록에 넣지 않는다.
     */
    public record MemberList(List<Item> members) {

        public record Item(
                Long memberId,
                String nickname,
                String role,
                String status,
                Long balance,
                /** FR-025① 이 챌린지에서의 연속 성공일. 팀원이 함께 보는 값이다. */
                Integer streak
        ) {
        }
    }

    /**
     * {@code GET /challenges/{challengeId}/members/me}
     *
     * <p>{@code notificationSettings} 는 저장된 적 없는 종류까지 채워 <b>항상 전체 종류</b>를 담는다.
     * 프론트가 종류 목록을 하드코딩하지 않고 이 키들로 토글을 그리면 된다.
     */
    public record Me(
            Long memberId,
            Long userId,
            Long challengeId,
            String nickname,
            String role,
            String status,
            String themeColor,
            Map<NotificationType, Boolean> notificationSettings
    ) {
    }

    /** {@code PATCH /challenges/{challengeId}/members/me} */
    public record Updated(
            Long memberId,
            String nickname,
            String themeColor,
            Map<NotificationType, Boolean> notificationSettings
    ) {
    }
}
