package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.config.VoteProperties;
import com.together.dto.Challenge;
import com.together.dto.Member;
import com.together.dto.MissionModeVote;
import com.together.dto.MissionModeVoteBallot;
import com.together.dto.VoteChoiceCount;
import com.together.dto.request.MissionModeVoteBallotRequest;
import com.together.dto.request.MissionModeVoteOpenRequest;
import com.together.dto.response.ChallengeResponses;
import com.together.mapper.ChallengeMapper;
import com.together.mapper.MissionModeVoteBallotMapper;
import com.together.mapper.MissionModeVoteMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-008 확장 — 미션 운영 방식을 팀원 투표로 정한다.
 *
 * <p>방장이 단독으로 정하는 기존 경로({@code PATCH /challenges/{id}/mission-mode})는 그대로 둔다.
 * 투표는 그 결정을 팀에 맡기고 싶을 때 쓰는 대안이고, 확정되면 같은 필드에 반영된다.
 *
 * <p><b>마감은 lazy 하게 한다.</b> 스케줄러가 없는 서비스라(NFR-003 폴링 기반), 기한이 지난 뒤
 * 누군가 조회하거나 투표하려는 순간에 닫는다. 덕분에 방장이 마감을 잊어도 결과가 확정된다 —
 * FR-012c 의 "방장 무응답" 문제와 같은 성격의 대비책이다.
 */
@Service
public class MissionModeVoteService {

    private static final String OUTCOME_DECIDED = "DECIDED";
    private static final String OUTCOME_TIE = "TIE";
    private static final String OUTCOME_NOT_ENOUGH = "NOT_ENOUGH_VOTES";

    private static final String CHOICE_FIXED = "FIXED";
    private static final String CHOICE_AI = "AI";

    /** 선택지. CHALLENGE.MISSION_MODE 와 같은 값이라 확정 결과를 그대로 옮겨 담을 수 있다. */
    private static final List<String> CHOICES = List.of(CHOICE_FIXED, CHOICE_AI);

    private final MissionModeVoteMapper voteMapper;
    private final MissionModeVoteBallotMapper ballotMapper;
    private final ChallengeMapper challengeMapper;
    private final MemberAccessService memberAccess;
    private final VoteProperties voteProperties;
    private final Clock clock;

    public MissionModeVoteService(MissionModeVoteMapper voteMapper,
                                  MissionModeVoteBallotMapper ballotMapper,
                                  ChallengeMapper challengeMapper,
                                  MemberAccessService memberAccess,
                                  VoteProperties voteProperties,
                                  Clock clock) {
        this.voteMapper = voteMapper;
        this.ballotMapper = ballotMapper;
        this.challengeMapper = challengeMapper;
        this.memberAccess = memberAccess;
        this.voteProperties = voteProperties;
        this.clock = clock;
    }

    /** 방장이 투표를 연다. 챌린지당 열린 투표는 하나뿐이라 이미 있으면 409. */
    @Transactional
    public ChallengeResponses.MissionModeVoteStatus open(Long userId, Long challengeId,
                                                         MissionModeVoteOpenRequest request) {
        Member me = memberAccess.requireOwner(userId, challengeId);

        MissionModeVote running = closeIfExpired(voteMapper.findOpenByChallengeId(challengeId), challengeId);
        if (running != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 진행 중인 투표가 있습니다.");
        }

        int hours = request != null && request.durationHours() != null
                ? request.durationHours()
                : voteProperties.defaultDurationHours();

        MissionModeVote vote = new MissionModeVote();
        vote.setChallengeId(challengeId);
        vote.setDeadline(LocalDateTime.now(clock).plusHours(hours));
        vote.setOpenedByMemberId(me.getId());
        try {
            voteMapper.insert(vote);
        } catch (DuplicateKeyException e) {
            // 방장 둘이 동시에 열려고 한 경우. 부분 유니크 인덱스가 최종 방어선이다.
            throw new BusinessException(HttpStatus.CONFLICT, "이미 진행 중인 투표가 있습니다.");
        }
        return toStatus(voteMapper.findById(vote.getId()), challengeId, me.getId());
    }

    /**
     * 현황 조회. 열린 투표가 없으면 직전 투표 결과를 보여준다 — 프론트가 "지난번엔 이렇게 정해졌다"를
     * 그릴 수 있어야 하기 때문이다. 한 번도 연 적이 없으면 404.
     */
    @Transactional
    public ChallengeResponses.MissionModeVoteStatus status(Long userId, Long challengeId) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);

        MissionModeVote vote = closeIfExpired(voteMapper.findOpenByChallengeId(challengeId), challengeId);
        if (vote == null) {
            vote = voteMapper.findLatestByChallengeId(challengeId);
        }
        if (vote == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "진행했거나 진행 중인 투표가 없습니다.");
        }
        return toStatus(vote, challengeId, me.getId());
    }

    /** 표를 던지거나 바꾼다. 탈퇴자는 애초에 여기까지 오지 못한다(requireActiveMember). */
    @Transactional
    public ChallengeResponses.MissionModeVoteStatus castBallot(Long userId, Long challengeId,
                                                               MissionModeVoteBallotRequest request) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);

        MissionModeVote vote = closeIfExpired(voteMapper.findOpenByChallengeId(challengeId), challengeId);
        if (vote == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "진행 중인 투표가 없습니다.");
        }

        ballotMapper.upsert(vote.getId(), me.getId(), request.choice());
        return toStatus(voteMapper.findById(vote.getId()), challengeId, me.getId());
    }

    /**
     * 방장이 기한 전에 조기 마감한다. 기한이 이미 지났다면 그냥 마감 처리만 된다.
     *
     * <p>마감은 방장 전용이지만, 기한이 지난 뒤에는 아무나 조회·투표해도 자동으로 닫힌다 —
     * 방장이 사라져도 결과가 확정되지 않은 채 남지 않게 하려는 것이다.
     */
    @Transactional
    public ChallengeResponses.MissionModeVoteStatus close(Long userId, Long challengeId) {
        Member me = memberAccess.requireOwner(userId, challengeId);

        MissionModeVote vote = voteMapper.findOpenByChallengeId(challengeId);
        if (vote == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "진행 중인 투표가 없습니다.");
        }
        closeNow(vote, challengeId);
        return toStatus(voteMapper.findById(vote.getId()), challengeId, me.getId());
    }

    // ------------------------------------------------------------------ 내부

    /** 기한이 지났으면 닫고 {@code null} 을 돌려준다. 아직이면 그대로 돌려준다. */
    private MissionModeVote closeIfExpired(MissionModeVote vote, Long challengeId) {
        if (vote == null) {
            return null;
        }
        if (vote.getDeadline().isAfter(LocalDateTime.now(clock))) {
            return vote;
        }
        closeNow(vote, challengeId);
        return null;
    }

    /**
     * 마감 판정. 표 계산만 하는 순수 함수라 따로 떼어 뒀다 — 여기가 이번 기능에서 가장 정책에 가까운
     * 자리라(무엇을 "결정됐다"고 볼 것인가) 테스트로 고정해 두는 편이 낫다.
     *
     * @param required 정족수. 이 수를 못 채우면 다수결을 따지지 않는다
     */
    static Decision decide(Map<String, Integer> counts, int required) {
        int fixed = counts.get(CHOICE_FIXED);
        int ai = counts.get(CHOICE_AI);
        if (fixed + ai < required) {
            return new Decision(OUTCOME_NOT_ENOUGH, null);
        }
        if (fixed == ai) {
            return new Decision(OUTCOME_TIE, null);
        }
        return new Decision(OUTCOME_DECIDED, fixed > ai ? CHOICE_FIXED : CHOICE_AI);
    }

    /** 마감 결과. {@code resultMode} 는 DECIDED 일 때만 값이 있다. */
    record Decision(String outcome, String resultMode) {
    }

    /**
     * 집계해서 닫는다. 정족수를 못 채웠거나 동률이면 챌린지의 미션 방식은 건드리지 않는다 —
     * 결정되지 않은 투표가 기존 설정을 뒤엎으면 안 되기 때문이다.
     */
    private void closeNow(MissionModeVote vote, Long challengeId) {
        Map<String, Integer> counts = countsOf(vote.getId());
        Decision decision = decide(counts, requiredVotes(challengeId));

        // 동시에 두 요청이 닫으려 하면 한쪽만 1을 받는다. 진 쪽은 반영까지 건너뛴다.
        if (voteMapper.close(vote.getId(), decision.outcome(), decision.resultMode(),
                LocalDateTime.now(clock)) == 1 && OUTCOME_DECIDED.equals(decision.outcome())) {
            challengeMapper.updateMissionMode(challengeId, decision.resultMode());
        }
    }

    /** 0표인 선택지도 키를 채워 돌려준다 — 프론트가 없는 키를 신경 쓰지 않게. */
    private Map<String, Integer> countsOf(Long voteId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        CHOICES.forEach(choice -> counts.put(choice, 0));
        for (VoteChoiceCount row : ballotMapper.countByChoice(voteId)) {
            counts.put(row.getChoice(), row.getCount());
        }
        return counts;
    }

    /**
     * 정족수 = ACTIVE 멤버의 {@code quorumPercent}% 이상 참여.
     *
     * <p>올림으로 계산하고 최소 1표는 있어야 한다 — 아무도 던지지 않은 투표가 "결정됨"이 될 수는 없다.
     */
    private int requiredVotes(Long challengeId) {
        int eligible = memberAccess.activeMembersOf(challengeId).size();
        int required = (int) Math.ceil(eligible * voteProperties.quorumPercent() / 100.0);
        return Math.max(required, 1);
    }

    private ChallengeResponses.MissionModeVoteStatus toStatus(MissionModeVote vote,
                                                              Long challengeId,
                                                              Long myMemberId) {
        Map<String, Integer> counts = countsOf(vote.getId());
        int voted = counts.values().stream().mapToInt(Integer::intValue).sum();
        MissionModeVoteBallot mine = ballotMapper.find(vote.getId(), myMemberId);
        Challenge challenge = challengeMapper.findById(challengeId);

        return new ChallengeResponses.MissionModeVoteStatus(
                vote.getId(), challengeId, vote.getStatus(), vote.getDeadline(),
                vote.getOutcome(), vote.getResultMode(),
                challenge == null ? null : challenge.getMissionMode(),
                memberAccess.activeMembersOf(challengeId).size(), voted, requiredVotes(challengeId),
                counts, mine == null ? null : mine.getChoice());
    }
}
