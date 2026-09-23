package com.together.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Mission 태그 응답 모음. */
public final class MissionResponses {

    private MissionResponses() {
    }

    /**
     * {@code GET /challenges/{challengeId}/missions} (화면 23, 방장 전용). 비활성 미션도 포함한다.
     *
     * <p>오프셋 페이지네이션. {@code totalCount} 로 전체 페이지 수를 계산할 수 있다.
     */
    public record MissionList(List<Item> missions, int page, int size, int totalCount) {

        public record Item(
                Long missionId,
                String title,
                String submitType,
                Integer rewardMin,
                Integer rewardMax,
                /** TEXT 미션의 자동완료 기준. PHOTO 미션이면 없다(화면 25 수정 폼이 현재값을 채울 때 쓴다). */
                Integer minTextLength,
                Boolean isActive,
                String createdByType
        ) {
        }
    }

    /** {@code POST /challenges/{challengeId}/missions} */
    public record MissionCreated(
            Long missionId,
            Long challengeId,
            String title,
            String submitType,
            Integer rewardMin,
            Integer rewardMax,
            Integer minTextLength,
            Boolean isActive,
            String createdByType,
            Long createdByMemberId
    ) {
    }

    /** {@code PATCH /missions/{missionId}} */
    public record MissionUpdated(
            Long missionId,
            String title,
            Integer rewardMin,
            Integer rewardMax,
            Integer minTextLength,
            Boolean isActive
    ) {
    }

    /**
     * {@code POST /challenges/{challengeId}/missions/ai-generate} (화면 24).
     *
     * <p>저장하지 않고 제안만 돌려준다. 방장이 골라서 미션 생성 API 로 등록하는 2단계 흐름이다.
     */
    public record AiSuggestions(List<Suggestion> suggestions) {

        public record Suggestion(
                String title,
                String submitType,
                Integer suggestedRewardMin,
                Integer suggestedRewardMax
        ) {
        }
    }

    /**
     * {@code GET /challenges/{challengeId}/missions/today} (화면 10).
     *
     * <p>확정된 보상 금액은 담지 않는다 — 카드에는 범위만 보여주고 정확한 금액은 완료 후에 공개한다.
     *
     * <p>활성 미션이 하나도 없으면(방장이 전부 비활성화한 경우) 404 가 아니라 200 +
     * {@code hasActiveMission=false} 로 내려준다. 미션이 없는 것은 오류가 아니라 정상 상태이고,
     * 404 로 내리면 프론트가 빈 상태 화면을 예외 처리 경로에서 그려야 한다.
     * null 필드는 직렬화에서 빠지므로 이때 응답은 {@code {"hasActiveMission": false}} 하나다.
     */
    public record Today(
            Boolean hasActiveMission,
            Long assignmentId,
            Long missionId,
            String title,
            String submitType,
            Integer rewardMin,
            Integer rewardMax,
            Integer minTextLength,
            LocalDate assignedDate,
            String mySubmissionStatus
    ) {

        /** 뽑을 미션이 없을 때. 나머지 필드는 전부 비어 응답에서 빠진다. */
        public static Today none() {
            return new Today(false, null, null, null, null, null, null, null, null, null);
        }
    }

    /** {@code POST /missions/{missionId}/submit} (화면 11) */
    public record Submitted(Long missionLogId, String status) {
    }

    /** 이의제기·재검토·재제출 공통 응답. */
    public record StatusChanged(Long missionLogId, String status) {
    }

    /**
     * {@code POST /mission-logs/{missionLogId}/review} (FR-012 재판정).
     *
     * <p>승인이면 지급 거래 ID 가, 이미 지급된 건을 반려하면 취소 거래 ID 가 담긴다.
     * 해당 없는 쪽은 null 이라 응답에서 빠진다(전역 non-null 직렬화 설정).
     */
    public record Reviewed(
            Long missionLogId,
            String status,
            String reviewedBy,
            Long rewardTransactionId,
            Long reversalTransactionId
    ) {
    }

    /**
     * {@code GET /challenges/{challengeId}/mission-logs} (화면 16, 팀 피드).
     *
     * <p>커서 페이지네이션. {@code nextCursor} 가 없으면 더 불러올 게 없다.
     * 캘린더처럼 {@code date} 로 하루치만 받는 경우엔 대개 한 번에 다 온다.
     */
    public record MissionLogList(List<Item> missionLogs, Long nextCursor) {

        public record Item(
                Long missionLogId,
                Long assignmentId,
                String missionTitle,
                LocalDate assignedDate,
                Long memberId,
                String nickname,
                String status,
                String reviewedBy,
                Integer rewardAmount,
                Boolean isPublic,
                String submitContentPreview
        ) {
        }
    }

    /** {@code GET /mission-logs/{missionLogId}} (화면 12·14) */
    public record MissionLogDetailResponse(
            Long missionLogId,
            Long assignmentId,
            String missionTitle,
            Long challengeId,
            Long memberId,
            String nickname,
            String submitContent,
            String attachmentUrl,
            Boolean isPublic,
            String status,
            String reviewedBy,
            String rejectReasonCode,
            Integer rewardAmount,
            LocalDateTime createdAt
    ) {
    }
}
