package com.together.mapper;

import com.together.dto.DailyMissionAssignment;
import com.together.dto.TodayMission;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Param;

/**
 * DAILY_MISSION_ASSIGNMENT 테이블 접근.
 * {@code resources/mapper/DailyMissionAssignmentMapper.xml} 과 1:1 대응한다.
 */
public interface DailyMissionAssignmentMapper {

    DailyMissionAssignment findById(@Param("id") Long id);

    /**
     * 특정 챌린지의 특정 날짜 배정(UK_DMA_CHALLENGE_DATE). 아직 안 뽑았으면 {@code null}.
     *
     * <p>배정을 새로 만들지 판단하기 위한 존재 확인용이다.
     */
    /**
     * 이 미션이 배정된 적이 있는지 센다. 미션 삭제(FR-047) 가능 여부를 가르는 기준이다 —
     * 한 번이라도 배정됐으면 MISSION_LOG·TRANSACTIONS 가 그 배정을 참조하고 있어 지울 수 없다.
     */
    int countByMissionId(@Param("missionId") Long missionId);

    DailyMissionAssignment findByChallengeIdAndAssignedDate(
            @Param("challengeId") Long challengeId, @Param("assignedDate") LocalDate assignedDate);

    /**
     * {@code GET /challenges/{id}/missions/today} 용. 배정에 미션 정보와 요청자 본인의 제출 상태를
     * 조인해 돌려준다. 배정이 없으면 {@code null} 이므로 서비스가 먼저 배정을 만들어야 한다.
     */
    TodayMission findTodayMission(
            @Param("challengeId") Long challengeId,
            @Param("assignedDate") LocalDate assignedDate,
            @Param("memberId") Long memberId);

    int insert(DailyMissionAssignment assignment);
}
