package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.Member;
import com.together.mapper.MemberMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * "이 사용자가 이 챌린지에서 무엇을 할 수 있는가"를 한 곳에서 판단한다.
 *
 * <p>JWT 에는 User.id 만 들어 있고 Member.id 는 챌린지마다 다르므로, 거의 모든 엔드포인트가
 * 먼저 이 변환을 거쳐야 한다. 탈퇴자(LEFT)는 참여자로 보지 않는다.
 */
@Service
public class MemberAccessService {

    private final MemberMapper memberMapper;

    public MemberAccessService(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    /** 참여 중인 멤버를 돌려준다. 미참여이거나 탈퇴했으면 403. */
    public Member requireActiveMember(Long userId, Long challengeId) {
        Member member = memberMapper.findByUserIdAndChallengeId(userId, challengeId);
        if (member == null || "LEFT".equals(member.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "이 챌린지에 참여하고 있지 않습니다.");
        }
        return member;
    }

    /** 방장만 할 수 있는 작업에 쓴다. 방장이 아니면 403. */
    public Member requireOwner(Long userId, Long challengeId) {
        Member member = requireActiveMember(userId, challengeId);
        if (!"OWNER".equals(member.getRole())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "방장만 할 수 있는 작업입니다.");
        }
        return member;
    }

    /**
     * Member.id 로 바로 찾는다. 이미 다른 경로로 소속이 확인된 대상(예: 미션 기록의 작성자)을
     * 집을 때 쓴다 — 권한 검사는 하지 않는다.
     */
    public Member requireMemberById(Long memberId) {
        Member member = memberMapper.findById(memberId);
        if (member == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다.");
        }
        return member;
    }

    /** 알림 발송 대상처럼 탈퇴자를 빼고 돌려야 하는 곳에서 쓴다. */
    public List<Member> activeMembersOf(Long challengeId) {
        return memberMapper.findByChallengeId(challengeId).stream()
                .filter(m -> !"LEFT".equals(m.getStatus()))
                .toList();
    }
}
