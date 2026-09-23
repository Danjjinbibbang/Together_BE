package com.together.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Challenge 태그 응답 모음. */
public final class ChallengeResponses {

    private ChallengeResponses() {
    }

    /** {@code GET /challenges} (화면 4) */
    public record ChallengeList(List<Item> challenges) {

        public record Item(
                Long challengeId,
                String title,
                Long goalAmount,
                Long currentBalance,
                LocalDate startDate,
                LocalDate endDate,
                Integer memberCount,
                String themeColor
        ) {
        }
    }

    /** {@code POST /challenges} (화면 5·6). 생성 직후라 {@code missionMode} 는 아직 null 이다. */
    public record Created(
            Long challengeId,
            String title,
            Long goalAmount,
            LocalDate startDate,
            LocalDate endDate,
            String inviteCode,
            String missionMode,
            Long memberId,
            String role
    ) {
    }

    /** {@code GET /challenges/{challengeId}} · {@code PATCH /challenges/{challengeId}} (화면 8) */
    public record Detail(
            Long challengeId,
            String title,
            Long goalAmount,
            LocalDate startDate,
            LocalDate endDate,
            String missionMode,
            String inviteCode,
            Long totalBalance,
            Integer progressRate,
            String myRole,
            Long myMemberId,
            /** FR-025① 이 챌린지에서의 내 연속 성공일. 성공 기록이 없으면 0. */
            Integer myStreak,
            /** FR-045 내 계좌 잔액. goalAmount 는 1인당 목표라 이 값과 직접 비교한다. */
            Long myBalance,
            /** FR-045 내가 목표를 채웠는가. 채웠으면 미션 수행이 막힌다(FR-046). */
            Boolean isGoalAchieved
    ) {
    }

    /**
     * {@code GET /challenges/by-invite-code/{inviteCode}} (화면 7 참여 전 미리보기)
     *
     * <p>참여 전에도 볼 수 있는 정보만 담는다 — 팀원 닉네임·잔액은 넣지 않는다.
     *
     * @param alreadyJoined 이미 참여 중이면 true. 프론트는 참여 버튼 대신 "들어가기"를 보이면 된다
     */
    public record InvitePreview(
            Long challengeId,
            String title,
            Long goalAmount,
            LocalDate startDate,
            LocalDate endDate,
            Integer memberCount,
            boolean alreadyJoined
    ) {
    }

    /** {@code GET /challenges/{challengeId}/invite-code} (화면 6) */
    public record InviteCode(String inviteCode, String inviteUrl) {
    }

    /** {@code POST /challenges/{challengeId}/join} (화면 7) */
    public record Joined(
            Long memberId,
            Long challengeId,
            String nickname,
            String role,
            String status
    ) {
    }

    /** {@code PATCH /challenges/{challengeId}/mission-mode} (화면 9) */
    public record MissionMode(Long challengeId, String missionMode) {
    }

    /** {@code PATCH /challenges/{challengeId}/owner} (FR-037) */
    public record OwnerTransferred(
            Long challengeId,
            Long previousOwnerMemberId,
            Long newOwnerMemberId
    ) {
    }

    /**
     * {@code GET · POST /challenges/{challengeId}/mission-mode/vote} 계열 공통 응답 (FR-008 확장).
     *
     * <p>투표를 열든 표를 던지든 마감하든 항상 같은 모양의 현황을 돌려준다 — 프론트가 한 화면을
     * 한 가지 방식으로만 그리면 되도록.
     *
     * @param status             OPEN / CLOSED
     * @param outcome            DECIDED / TIE / NOT_ENOUGH_VOTES. 열려 있는 동안에는 없다
     * @param resultMode         DECIDED 일 때 확정된 방식. 이 값이 챌린지에 반영된다
     * @param currentMissionMode 지금 챌린지에 적용돼 있는 방식. 아직 안 정했으면 없다
     * @param eligibleVoters     투표할 수 있는 사람 수(ACTIVE 멤버). 탈퇴자는 빠진다
     * @param requiredVotes      결과를 확정하려면 필요한 최소 표 수(정족수)
     * @param counts             선택지별 표 수. 0표인 선택지도 키가 있다
     * @param myChoice           내가 던진 표. 아직 안 했으면 없다
     */
    public record MissionModeVoteStatus(
            Long voteId,
            Long challengeId,
            String status,
            LocalDateTime deadline,
            String outcome,
            String resultMode,
            String currentMissionMode,
            Integer eligibleVoters,
            Integer votedCount,
            Integer requiredVotes,
            Map<String, Integer> counts,
            String myChoice
    ) {
    }
}
