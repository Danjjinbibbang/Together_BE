package com.together.mapper;

import com.together.dto.Challenge;
import com.together.dto.ChallengeSummary;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * CHALLENGE 테이블 접근. {@code resources/mapper/ChallengeMapper.xml} 과 1:1 대응한다.
 */
public interface ChallengeMapper {

    Challenge findById(@Param("id") Long id);

    /** 초대코드로 챌린지를 찾는다(UK_CHALLENGE_INVITE_CODE). 없으면 {@code null}. */
    Challenge findByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * {@code GET /challenges} 용. 내가 참여 중인 챌린지에 잔액 합계·멤버 수·내 테마색을 얹어 돌려준다.
     */
    List<ChallengeSummary> findSummariesByUserId(@Param("userId") Long userId);

    int insert(Challenge challenge);

    /**
     * FR-036 챌린지 정보 수정(제목/목표금액/기간). 방장 권한 확인은 서비스가 한다.
     *
     * <p>부분 갱신이 아니라 전체 덮어쓰기다. PATCH 의 선택적 필드 병합은 서비스가 처리한다.
     */
    int update(Challenge challenge);

    /** PATCH /challenges/{challengeId}/mission-mode */
    int updateMissionMode(@Param("id") Long id, @Param("missionMode") String missionMode);
}
