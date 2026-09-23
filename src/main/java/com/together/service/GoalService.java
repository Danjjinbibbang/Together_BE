package com.together.service;

import com.together.dto.MemberBalance;
import com.together.mapper.MemberMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 목표 달성 판정(FR-045·FR-046, 2026-08-25 기획 확정).
 *
 * <p><b>{@code goalAmount} 는 팀 합산이 아니라 1인당 목표다.</b> 챌린지 생성 시 정한 금액을 채운
 * 사람이 달성자가 되고, 다른 팀원은 아직 못 채웠을 수 있다. 팀 목표는 {@code goalAmount × 활성 멤버수}
 * 가 된다.
 *
 * <p>판정이 세 곳(챌린지 상세·계좌 합산·미션 제출)에서 필요해 여기 한 곳에 모았다. 흩어 두면
 * "화면에 보이는 진행률"과 "서버가 막는 기준"이 갈라진다 — 프론트가 §12 에서 지적한 위험이 그것이다.
 */
@Service
public class GoalService {

    private final MemberMapper memberMapper;

    public GoalService(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    /**
     * 팀 진행률(%). {@code totalBalance ÷ (goalAmount × 활성 멤버수)}.
     *
     * <p>예전 식({@code ÷ goalAmount})은 팀 합산 목표를 전제했던 것이라, 4명이 각자 목표를 채우면
     * 400% 가 나왔다. 소수점은 버린다 — 목표를 넘기면 100을 넘을 수 있으므로 프로그레스바 폭은
     * 프론트가 잘라 써야 한다.
     */
    public int progressRate(Long challengeId, long totalBalance, long goalAmount) {
        return progressRate(totalBalance, goalAmount, activeMemberCount(challengeId));
    }

    /** 멤버 목록을 이미 읽어 둔 곳에서 쓰는 오버로드. 같은 조회를 두 번 하지 않으려는 것이다. */
    public static int progressRate(long totalBalance, long goalAmount, int activeMemberCount) {
        long teamGoal = goalAmount * activeMemberCount;
        if (teamGoal <= 0) {
            return 0;
        }
        return (int) (totalBalance * 100 / teamGoal);
    }

    /** 이 사람이 자기 목표를 채웠는가. 채운 사람은 미션을 더 수행하지 않는다(FR-046). */
    public boolean achievedByMember(Long memberId, long goalAmount, long myBalance) {
        return goalAmount > 0 && myBalance >= goalAmount;
    }

    /**
     * 활성 멤버 <b>전원</b>이 각자 목표를 채웠는가.
     *
     * <p>합계({@code totalBalance >= goalAmount × 인원})로 보지 않는다. 누군가 초과로 모으면 아직
     * 못 채운 사람이 가려지기 때문이다 — 프론트도 같은 이유로 멤버별 확인을 택했다.
     *
     * <p>활성 멤버가 없으면 false 다. "아무도 없는 팀이 목표를 달성했다"고 볼 수는 없다.
     */
    public boolean teamAchieved(Long challengeId, long goalAmount) {
        List<MemberBalance> members = activeMembersWithBalance(challengeId);
        if (members.isEmpty()) {
            return false;
        }
        return members.stream()
                .allMatch(m -> achievedByMember(m.getMemberId(), goalAmount, balanceOf(m)));
    }

    /**
     * 방금 들어온 입금을 빼면 팀이 아직 미달성이었는가.
     *
     * <p>"마지막 주자가 채우는 순간"에만 축하가 떠야 하므로, 이미 달성한 팀에서 미션을 할 때마다
     * 다시 뜨지 않게 하려면 <b>직전 상태</b>를 함께 봐야 한다. 프론트가 개인 축하에서 쓰는 조건과
     * 같은 논리다.
     */
    public boolean teamJustAchieved(Long challengeId, Long payeeMemberId, long goalAmount, long paidAmount) {
        List<MemberBalance> members = activeMembersWithBalance(challengeId);
        if (members.isEmpty()) {
            return false;
        }

        boolean allAchievedNow = members.stream()
                .allMatch(m -> achievedByMember(m.getMemberId(), goalAmount, balanceOf(m)));
        if (!allAchievedNow) {
            return false;
        }

        // 입금 전 잔액으로 되돌려 본다. 그때도 전원 달성이었다면 이번 입금으로 달성한 것이 아니다.
        return members.stream()
                .filter(m -> m.getMemberId().equals(payeeMemberId))
                .anyMatch(m -> !achievedByMember(m.getMemberId(), goalAmount, balanceOf(m) - paidAmount));
    }

    public int activeMemberCount(Long challengeId) {
        return activeMembersWithBalance(challengeId).size();
    }

    private List<MemberBalance> activeMembersWithBalance(Long challengeId) {
        return memberMapper.findWithBalanceByChallengeId(challengeId).stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .toList();
    }

    /** 계좌가 아직 없는 짧은 구간에는 잔액이 null 로 온다. */
    private static long balanceOf(MemberBalance member) {
        return member.getBalance() == null ? 0L : member.getBalance();
    }
}
