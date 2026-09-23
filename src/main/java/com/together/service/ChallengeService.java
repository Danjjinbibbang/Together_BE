package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.config.InviteProperties;
import com.together.dto.Account;
import com.together.dto.Challenge;
import com.together.dto.ChallengeSummary;
import com.together.dto.Member;
import com.together.dto.request.ChallengeCreateRequest;
import com.together.dto.request.ChallengeJoinRequest;
import com.together.dto.request.ChallengeUpdateRequest;
import com.together.dto.request.MissionModeUpdateRequest;
import com.together.dto.request.OwnerTransferRequest;
import com.together.dto.response.ChallengeResponses;
import com.together.mapper.AccountMapper;
import com.together.mapper.ChallengeMapper;
import com.together.mapper.MemberMapper;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-001 ~ FR-007, FR-036 ~ FR-038. 챌린지 생성·참여·운영.
 */
@Service
public class ChallengeService {

    /** 사람이 불러주기 쉽도록 헷갈리는 글자(O/0, I/1)는 뺐다. */
    private static final char[] INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int INVITE_CODE_LENGTH = 8;
    private static final int INVITE_CODE_MAX_ATTEMPTS = 5;

    private final ChallengeMapper challengeMapper;
    private final MemberMapper memberMapper;
    private final AccountMapper accountMapper;
    private final MemberAccessService memberAccess;
    private final GoalService goalService;
    private final StreakService streakService;
    private final InviteProperties inviteProperties;
    private final SecureRandom random = new SecureRandom();

    public ChallengeService(ChallengeMapper challengeMapper,
                            MemberMapper memberMapper,
                            AccountMapper accountMapper,
                            MemberAccessService memberAccess,
                            GoalService goalService,
                            StreakService streakService,
                            InviteProperties inviteProperties) {
        this.challengeMapper = challengeMapper;
        this.memberMapper = memberMapper;
        this.accountMapper = accountMapper;
        this.memberAccess = memberAccess;
        this.goalService = goalService;
        this.streakService = streakService;
        this.inviteProperties = inviteProperties;
    }

    public ChallengeResponses.ChallengeList list(Long userId) {
        List<ChallengeSummary> summaries = challengeMapper.findSummariesByUserId(userId);
        return new ChallengeResponses.ChallengeList(summaries.stream()
                .map(s -> new ChallengeResponses.ChallengeList.Item(
                        s.getChallengeId(), s.getTitle(), s.getGoalAmount(), s.getCurrentBalance(),
                        s.getStartDate(), s.getEndDate(), s.getMemberCount(), s.getThemeColor()))
                .toList());
    }

    /**
     * 챌린지를 만든다(화면 5·6). 만든 사람이 방장이 되고 가상 계좌도 함께 생긴다.
     *
     * <p>미션 운영 방식은 여기서 정하지 않는다 — 생성 응답의 {@code missionMode} 가 null 이고,
     * 화면 9에서 따로 설정한다. 그래야 "아직 안 정했음"이 응답에 그대로 드러난다.
     */
    @Transactional
    public ChallengeResponses.Created create(Long userId, ChallengeCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }

        Challenge challenge = new Challenge();
        challenge.setTitle(request.title());
        challenge.setTargetAmount(request.goalAmount());
        challenge.setStartDate(request.startDate());
        challenge.setEndDate(request.endDate());
        challenge.setInviteCode(generateUniqueInviteCode());
        challengeMapper.insert(challenge);

        Member owner = new Member();
        owner.setUserId(userId);
        owner.setChallengeId(challenge.getId());
        owner.setNickname(request.nickname());
        owner.setRole("OWNER");
        owner.setStatus("ACTIVE");
        memberMapper.insert(owner);

        createAccountFor(owner.getId());

        return new ChallengeResponses.Created(
                challenge.getId(), challenge.getTitle(), challenge.getTargetAmount(),
                challenge.getStartDate(), challenge.getEndDate(), challenge.getInviteCode(),
                null, owner.getId(), owner.getRole());
    }

    public ChallengeResponses.Detail detail(Long userId, Long challengeId) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        Challenge challenge = requireChallenge(challengeId);
        long totalBalance = accountMapper.sumBalanceByChallengeId(challengeId);
        long myBalance = myBalance(me.getId());

        return new ChallengeResponses.Detail(
                challenge.getId(), challenge.getTitle(), challenge.getTargetAmount(),
                challenge.getStartDate(), challenge.getEndDate(), challenge.getMissionMode(),
                challenge.getInviteCode(), totalBalance,
                goalService.progressRate(challengeId, totalBalance, challenge.getTargetAmount()),
                me.getRole(), me.getId(),
                streakService.byMember(challengeId).getOrDefault(me.getId(), 0),
                myBalance,
                goalService.achievedByMember(me.getId(), challenge.getTargetAmount(), myBalance));
    }

    /** 계좌가 아직 없는 짧은 구간에는 0 으로 본다. */
    private long myBalance(Long memberId) {
        Account account = accountMapper.findByMemberId(memberId);
        return account == null || account.getBalance() == null ? 0L : account.getBalance();
    }

    /** FR-036. 보낸 필드만 반영하므로 기존 값을 읽어 병합한 뒤 통째로 덮어쓴다. */
    @Transactional
    public ChallengeResponses.Detail update(Long userId, Long challengeId, ChallengeUpdateRequest request) {
        memberAccess.requireOwner(userId, challengeId);
        Challenge challenge = requireChallenge(challengeId);

        if (request.title() != null) {
            challenge.setTitle(request.title());
        }
        if (request.goalAmount() != null) {
            challenge.setTargetAmount(request.goalAmount());
        }
        if (request.startDate() != null) {
            challenge.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            challenge.setEndDate(request.endDate());
        }
        if (challenge.getEndDate().isBefore(challenge.getStartDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }

        challengeMapper.update(challenge);
        return detail(userId, challengeId);
    }

    public ChallengeResponses.InviteCode inviteCode(Long userId, Long challengeId) {
        memberAccess.requireActiveMember(userId, challengeId);
        Challenge challenge = requireChallenge(challengeId);
        String url = inviteProperties.baseUrl() + "?code=" + challenge.getInviteCode();
        return new ChallengeResponses.InviteCode(challenge.getInviteCode(), url);
    }

    /**
     * 초대코드로 챌린지를 미리 본다(화면 7). <b>인증만 되면 참여 전에도 볼 수 있다</b> —
     * 참여할지 정하려면 무엇에 들어가는지부터 알아야 하기 때문이다.
     *
     * <p>보여주는 것은 챌린지명·목표금액·기간·인원까지다. 팀원 닉네임이나 잔액은 담지 않는다.
     * 코드만 알면 누구나 부를 수 있는 API 라 참여 전에 팀 내부가 드러나면 안 된다.
     */
    public ChallengeResponses.InvitePreview previewByInviteCode(Long userId, String inviteCode) {
        Challenge challenge = requireChallengeByInviteCode(inviteCode);
        Member existing = memberMapper.findByUserIdAndChallengeId(userId, challenge.getId());

        return new ChallengeResponses.InvitePreview(
                challenge.getId(), challenge.getTitle(), challenge.getTargetAmount(),
                challenge.getStartDate(), challenge.getEndDate(),
                goalService.activeMemberCount(challenge.getId()),
                existing != null && !"LEFT".equals(existing.getStatus()));
    }

    /**
     * 초대코드로 참여한다(화면 7). 링크를 눌렀다고 팀원이 되지 않고 닉네임을 받아야 가입이 끝난다.
     *
     * <p>경로에 challengeId 를 두지 않는다. 참여하려는 사람이 가진 건 초대코드뿐이라, 코드로
     * 챌린지를 찾는 일은 서버가 해야 한다(2026-08-25 프론트 요청 §12).
     *
     * <p>닉네임 중복과 재참여는 둘 다 409 다 — DB 의 UNIQUE 제약이 최종 방어선이라 예외를 잡아 변환한다.
     */
    @Transactional
    public ChallengeResponses.Joined join(Long userId, ChallengeJoinRequest request) {
        Challenge challenge = requireChallengeByInviteCode(request.inviteCode());
        Long challengeId = challenge.getId();

        Member existing = memberMapper.findByUserIdAndChallengeId(userId, challengeId);
        if (existing != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 참여한 챌린지입니다.");
        }

        Member member = new Member();
        member.setUserId(userId);
        member.setChallengeId(challengeId);
        member.setNickname(request.nickname());
        member.setRole("MEMBER");
        member.setStatus("ACTIVE");
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        createAccountFor(member.getId());

        return new ChallengeResponses.Joined(
                member.getId(), challengeId, member.getNickname(), member.getRole(), member.getStatus());
    }

    @Transactional
    public ChallengeResponses.MissionMode updateMissionMode(Long userId, Long challengeId,
                                                            MissionModeUpdateRequest request) {
        memberAccess.requireOwner(userId, challengeId);
        challengeMapper.updateMissionMode(challengeId, request.missionMode());
        return new ChallengeResponses.MissionMode(challengeId, request.missionMode());
    }

    /**
     * FR-037 방장 양도. 챌린지 테이블은 건드리지 않는다 — 방장 정보의 유일한 소스는 MEMBER.ROLE 이다.
     *
     * <p>두 UPDATE 가 한 트랜잭션이라 방장이 둘이거나 없는 중간 상태가 밖에서 보이지 않는다.
     */
    @Transactional
    public ChallengeResponses.OwnerTransferred transferOwner(Long userId, Long challengeId,
                                                            OwnerTransferRequest request) {
        Member currentOwner = memberAccess.requireOwner(userId, challengeId);

        Member newOwner = memberMapper.findById(request.newOwnerMemberId());
        if (newOwner == null || !challengeId.equals(newOwner.getChallengeId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "해당 챌린지의 멤버가 아닙니다.");
        }
        if (newOwner.getId().equals(currentOwner.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 방장입니다.");
        }
        if ("LEFT".equals(newOwner.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "탈퇴한 멤버에게는 양도할 수 없습니다.");
        }

        memberMapper.updateRole(currentOwner.getId(), "MEMBER");
        memberMapper.updateRole(newOwner.getId(), "OWNER");

        return new ChallengeResponses.OwnerTransferred(
                challengeId, currentOwner.getId(), newOwner.getId());
    }

    private void createAccountFor(Long memberId) {
        Account account = new Account();
        account.setMemberId(memberId);
        account.setBalance(0L);
        accountMapper.insert(account);
    }

    /**
     * 초대코드로 찾는다. 없는 코드는 404 다 — 참여 화면에서 오타를 바로 알려줘야 한다.
     *
     * <p>대소문자를 가린다. 초대코드는 대문자+숫자로만 만들지만(O/0, I/1 제외) 사람이
     * 불러주고 받아 적는 값이라 소문자로 칠 수 있어서다.
     */
    private Challenge requireChallengeByInviteCode(String inviteCode) {
        Challenge challenge = inviteCode == null
                ? null
                : challengeMapper.findByInviteCode(inviteCode.trim().toUpperCase(Locale.ROOT));
        if (challenge == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "초대코드에 해당하는 챌린지가 없습니다.");
        }
        return challenge;
    }

    private Challenge requireChallenge(Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "챌린지를 찾을 수 없습니다.");
        }
        return challenge;
    }

    /**
     * 초대코드를 뽑는다. 충돌이 드물어 몇 번만 재시도하고, 그래도 실패하면 예외를 올린다
     * (조용히 중복 코드를 쓰느니 실패하는 편이 낫다).
     */
    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < INVITE_CODE_MAX_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                code.append(INVITE_CODE_ALPHABET[random.nextInt(INVITE_CODE_ALPHABET.length)]);
            }
            String candidate = code.toString();
            if (challengeMapper.findByInviteCode(candidate) == null) {
                return candidate;
            }
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "초대코드 생성에 실패했습니다. 다시 시도해 주세요.");
    }
}
