package com.together.mapper;

import com.together.dto.Member;
import com.together.dto.MemberBalance;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MEMBER 테이블 접근. {@code resources/mapper/MemberMapper.xml} 과 1:1 대응한다.
 */
public interface MemberMapper {

    Member findById(@Param("id") Long id);

    /** 한 사람의 특정 챌린지 참여 정보(UK_MEMBER_USER_CHALLENGE). 미참여면 {@code null}. */
    Member findByUserIdAndChallengeId(
            @Param("userId") Long userId, @Param("challengeId") Long challengeId);

    /** 한 사람이 참여한 모든 챌린지의 멤버 행. 계정 탈퇴(FR-043)가 방장 여부를 훑을 때 쓴다. */
    List<Member> findByUserId(@Param("userId") Long userId);

    /** 챌린지의 전체 참여자. 탈퇴자(LEFT)까지 포함하므로 걸러내는 것은 서비스 몫이다. */
    List<Member> findByChallengeId(@Param("challengeId") Long challengeId);

    /**
     * 멤버 목록에 계좌 잔액을 붙여 돌려준다.
     * {@code GET /challenges/{id}/members} 와 {@code /accounts} 가 함께 쓴다.
     */
    List<MemberBalance> findWithBalanceByChallengeId(@Param("challengeId") Long challengeId);

    int insert(Member member);

    /** PATCH /challenges/{challengeId}/members/me */
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    /** FR-041/042 캘린더 색상 변경. 본인에게만 보이는 설정이다. */
    int updateThemeColor(@Param("id") Long id, @Param("themeColor") String themeColor);


    /** FR-037 방장 양도. 넘기는 쪽과 받는 쪽 각각 호출한다 — 순서와 트랜잭션은 서비스가 잡는다. */
    int updateRole(@Param("id") Long id, @Param("role") String role);

    /** FR-038 탈퇴. 행을 지우지 않고 STATUS 만 LEFT 로 바꾼다. */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** FR-043 계정 탈퇴. 참여 중인 모든 챌린지에서 한 번에 나간다. */
    int updateStatusByUserId(@Param("userId") Long userId, @Param("status") String status);
}
