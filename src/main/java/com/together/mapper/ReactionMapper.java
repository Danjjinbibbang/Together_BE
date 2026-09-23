package com.together.mapper;

import com.together.dto.Reaction;
import com.together.dto.ReactionCount;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * REACTION 테이블 접근. {@code resources/mapper/ReactionMapper.xml} 과 1:1 대응한다.
 */
public interface ReactionMapper {

    /**
     * 요청자가 이 기록에 남긴 반응(UK_REACTION_LOG_SENDER). 없으면 {@code null}.
     *
     * <p>멤버당 1개라 이모지 타입은 조건에 넣지 않는다. 토글 API 가 등록/해제/교체를
     * 판단하는 근거가 이 조회다.
     */
    Reaction findByMissionLogIdAndSenderMemberId(
            @Param("missionLogId") Long missionLogId,
            @Param("senderMemberId") Long senderMemberId);

    List<Reaction> findByMissionLogId(@Param("missionLogId") Long missionLogId);

    /** 이모지별 개수 집계. 맵으로 접는 것은 서비스가 한다. */
    List<ReactionCount> countByMissionLogId(@Param("missionLogId") Long missionLogId);

    int insert(Reaction reaction);

    int deleteById(@Param("id") Long id);
}
