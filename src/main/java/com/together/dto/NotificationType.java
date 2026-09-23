package com.together.dto;

/**
 * 알림 종류. 사용자가 챌린지별로 종류마다 켜고 끌 수 있다.
 *
 * <p>여기가 유일한 정의 지점이다. 알림을 만드는 쪽과 수신 설정 화면이 같은 목록을 봐야 하므로,
 * 새 종류를 추가하면 이 enum 에만 넣으면 설정 API 응답에도 자동으로 따라온다.
 *
 * <p>API 명세서는 예시로 {@code MISSION_REWARD} 하나만 들고 있어 나머지는 FR-020 · FR-012a 를
 * 구현하며 정한 이름이다 — 목록 확정 후 조정될 수 있다.
 */
public enum NotificationType {

    /** 팀원이 미션을 완료해 입금됐을 때. 팀원 전원이 받는다(FR-020). */
    MISSION_REWARD("팀원 미션 완료 · 입금"),

    /** 내가 낸 미션이 반려됐을 때. 작성자 본인만 받는다(FR-012a). */
    MISSION_REJECTED("내 미션 반려"),

    /** 이의제기가 접수됐을 때. 방장만 받는다. */
    MISSION_DISPUTED("이의제기 접수 (방장)"),

    /** 팀원이 팀 전체에 응원 메시지를 보냈을 때. 보낸 사람을 뺀 나머지가 받는다(FR-044). */
    TEAM_CHEER("팀원 응원 메시지"),

    /** 활성 멤버 전원이 각자 목표를 채운 순간. 팀원 전원이 받는다(FR-045). */
    TEAM_GOAL_ACHIEVED("팀 목표 달성");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    /** 설정 화면에 그대로 쓸 수 있는 한글 라벨. */
    public String label() {
        return label;
    }
}
