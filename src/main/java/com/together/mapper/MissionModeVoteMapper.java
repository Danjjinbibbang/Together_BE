package com.together.mapper;

import com.together.dto.MissionModeVote;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

/**
 * MISSION_MODE_VOTE 테이블 접근. {@code resources/mapper/MissionModeVoteMapper.xml} 과 1:1 대응한다.
 */
public interface MissionModeVoteMapper {

    MissionModeVote findById(@Param("id") Long id);

    /** 챌린지에 열려 있는 투표. 없으면 {@code null} — 열린 투표는 챌린지당 최대 하나다. */
    MissionModeVote findOpenByChallengeId(@Param("challengeId") Long challengeId);

    /** 열린 것이 없을 때 직전 결과를 보여주기 위한 조회. 한 번도 없었으면 {@code null}. */
    MissionModeVote findLatestByChallengeId(@Param("challengeId") Long challengeId);

    /** 채번된 ID 는 {@code vote.id} 에 채워진다. */
    int insert(MissionModeVote vote);

    /**
     * 투표를 닫는다. 무엇으로 닫을지(다수결·동률·정족수 미달)는 서비스가 판단해 넘긴다.
     *
     * <p>{@code STATUS='OPEN'} 인 행만 건드리므로, 동시에 두 요청이 들어와도 한 번만 닫힌다.
     * 갱신 건수 0 은 "이미 누가 닫았다"는 뜻이다.
     */
    int close(@Param("id") Long id,
              @Param("outcome") String outcome,
              @Param("resultMode") String resultMode,
              @Param("closedAt") LocalDateTime closedAt);
}
