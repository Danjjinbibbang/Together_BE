package com.together.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Me 태그 응답 모음 — 챌린지를 가로지르는 "내 기록" 화면(16)용이다.
 *
 * <p>다른 응답들이 모두 {@code /challenges/{id}} 하위인 것과 달리, 캘린더는 참여 중인 모든
 * 챌린지를 한 화면에 모아 보여준다(FR-027, FR-040). 챌린지별로 나눠 받으면 프론트가 참여
 * 챌린지 수만큼 호출해 직접 합쳐야 해서 서버가 합쳐 내려준다.
 */
public final class MeResponses {

    private MeResponses() {
    }

    /**
     * {@code GET /me/calendar} — 월 단위 점 데이터 + 전체 활동 연속일.
     *
     * <p>활동이 없는 날은 {@code days} 에 아예 담기지 않는다. 빈 날까지 채우면 한 달치 응답이
     * 두 배가 되는데, 프론트는 날짜 격자를 어차피 직접 그리기 때문이다.
     */
    public record Calendar(
            LocalDate from,
            LocalDate to,
            List<Day> days,
            int totalStreak
    ) {

        public record Day(
                LocalDate date,
                Long totalReward,
                List<ChallengeEntry> challenges
        ) {

            public record ChallengeEntry(
                    Long challengeId,
                    String challengeTitle,
                    String themeColor,
                    Integer missionCount,
                    Long rewardAmount
            ) {
            }
        }
    }

    /**
     * {@code GET /me/mission-logs?date=} — 캘린더에서 날짜를 눌렀을 때의 목록(FR-040).
     *
     * <p>하루치라 페이지네이션이 없다. 참여 챌린지가 아무리 많아도 하루 최대 챌린지 수만큼이다
     * (FR-010 으로 챌린지당 하루 1미션이 확정돼 있다).
     */
    public record MyMissionLogList(LocalDate date, List<Item> missionLogs) {

        public record Item(
                Long missionLogId,
                Long challengeId,
                String challengeTitle,
                String themeColor,
                String missionTitle,
                LocalDate assignedDate,
                String status,
                String reviewedBy,
                Integer rewardAmount,
                Boolean isPublic
        ) {
        }
    }
}
