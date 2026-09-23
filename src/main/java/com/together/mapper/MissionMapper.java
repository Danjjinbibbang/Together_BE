package com.together.mapper;

import com.together.dto.Mission;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MISSION 테이블 접근. {@code resources/mapper/MissionMapper.xml} 과 1:1 대응한다.
 */
public interface MissionMapper {

    Mission findById(@Param("id") Long id);

    /** 전역 공용 고정 풀(CHALLENGE_ID IS NULL) 중 사용 가능한 미션. */
    List<Mission> findActiveFixedPool();

    /** 특정 챌린지 전용 미션 중 사용 가능한 것. 오늘의 미션 뽑기 대상 풀이다. */
    List<Mission> findActiveByChallengeId(@Param("challengeId") Long challengeId);

    /** 방장 미션 관리 화면용. 비활성 미션까지 전부 돌려준다. 오프셋 페이지네이션. */
    List<Mission> findAllByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    int countByChallengeId(@Param("challengeId") Long challengeId);

    int insert(Mission mission);

    /**
     * 미션 수정/비활성화(PATCH /missions/{missionId}).
     *
     * <p>부분 갱신이 아니라 전체 덮어쓰기다. 어떤 필드를 남기고 바꿀지는 서비스가 병합해서 넘긴다.
     */
    int update(Mission mission);

    /**
     * 미션을 완전히 지운다(FR-047).
     *
     * <p>배정된 적이 있으면 부르면 안 된다 — DAILY_MISSION_ASSIGNMENT 의 FK 에 걸려 실패한다.
     * 배정 이력이 있는지 확인하는 것은 서비스 몫이고, 그런 미션은 비활성화만 허용한다.
     */
    int deleteById(@Param("id") Long id);
}
