package com.together.mapper;

import com.together.dto.MissionModeVoteBallot;
import com.together.dto.VoteChoiceCount;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MISSION_MODE_VOTE_BALLOT 테이블 접근.
 * {@code resources/mapper/MissionModeVoteBallotMapper.xml} 과 1:1 대응한다.
 */
public interface MissionModeVoteBallotMapper {

    /** 내가 이미 던진 표. 없으면 {@code null}. */
    MissionModeVoteBallot find(@Param("voteId") Long voteId, @Param("memberId") Long memberId);

    /** 선택지별 표 수. 아무도 고르지 않은 선택지는 행이 없다. */
    List<VoteChoiceCount> countByChoice(@Param("voteId") Long voteId);

    /**
     * 표를 넣거나 바꾼다. 재투표를 새 행으로 쌓지 않으려고 MERGE 를 쓴다 —
     * PK(voteId, memberId)가 있어 INSERT 로는 두 번째 투표가 제약 위반이 된다.
     */
    int upsert(@Param("voteId") Long voteId,
               @Param("memberId") Long memberId,
               @Param("choice") String choice);
}
