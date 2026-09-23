package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.Account;
import com.together.dto.Challenge;
import com.together.dto.Member;
import com.together.dto.Notification;
import com.together.dto.NotificationType;
import com.together.dto.Transaction;
import com.together.mapper.AccountMapper;
import com.together.mapper.ChallengeMapper;
import com.together.mapper.MemberMapper;
import com.together.mapper.NotificationMapper;
import com.together.mapper.TransactionMapper;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-017, FR-018, FR-012d. 보상 지급·회수와 그에 따른 알림.
 *
 * <p>DB 트리거를 두지 않았으므로 거래 INSERT 와 잔액 갱신을 여기서 한 트랜잭션으로 묶는다.
 * 거래를 만드는 경로를 이 클래스로 모아 둔 이유도 잔액 갱신을 빠뜨릴 자리를 없애기 위해서다.
 */
@Service
public class RewardService {

    /** 보상 지급 */
    public static final String TYPE_REWARD = "MISSION_REWARD";
    /** 이미 지급한 건의 취소 */
    public static final String TYPE_REVERSAL = "REVERSAL";
    /** 재판정 통과 후 재지급 */
    public static final String TYPE_RE_PAYMENT = "RE_PAYMENT";

    private final TransactionMapper transactionMapper;
    private final AccountMapper accountMapper;
    private final MemberMapper memberMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationPolicy notificationPolicy;
    private final ChallengeMapper challengeMapper;
    private final GoalService goalService;

    public RewardService(TransactionMapper transactionMapper,
                         AccountMapper accountMapper,
                         MemberMapper memberMapper,
                         NotificationMapper notificationMapper,
                         NotificationPolicy notificationPolicy,
                         ChallengeMapper challengeMapper,
                         GoalService goalService) {
        this.transactionMapper = transactionMapper;
        this.accountMapper = accountMapper;
        this.memberMapper = memberMapper;
        this.notificationMapper = notificationMapper;
        this.notificationPolicy = notificationPolicy;
        this.challengeMapper = challengeMapper;
        this.goalService = goalService;
    }

    /**
     * 보상을 지급하고 팀원 전원에게 입금 알림을 보낸다(FR-020).
     *
     * @param txType {@link #TYPE_REWARD} 또는 {@link #TYPE_RE_PAYMENT}
     * @return 생성된 거래 ID
     */
    @Transactional
    public Long pay(Member member, Long missionLogId, int amount, String txType, Long originalTransactionId) {
        Account account = requireAccount(member.getId());

        Transaction tx = new Transaction();
        tx.setAccountId(account.getId());
        tx.setMissionLogId(missionLogId);
        tx.setOriginalTransactionId(originalTransactionId);
        tx.setAmount((long) amount);
        tx.setTxType(txType);
        transactionMapper.insert(tx);

        accountMapper.addBalance(account.getId(), (long) amount);

        notifyTeam(member, missionLogId, amount);
        notifyTeamGoalAchieved(member, amount);
        return tx.getId();
    }

    /**
     * 이미 지급된 보상을 회수한다(FR-012d). 원거래를 가리키는 음수 거래를 남긴다.
     *
     * @return 생성된 취소 거래 ID
     */
    @Transactional
    public Long reverse(Member member, Long missionLogId, Transaction original) {
        Account account = requireAccount(member.getId());

        Transaction tx = new Transaction();
        tx.setAccountId(account.getId());
        tx.setMissionLogId(missionLogId);
        tx.setOriginalTransactionId(original.getId());
        tx.setAmount(-original.getAmount());
        tx.setTxType(TYPE_REVERSAL);
        transactionMapper.insert(tx);

        try {
            accountMapper.addBalance(account.getId(), -original.getAmount());
        } catch (DataIntegrityViolationException e) {
            // CK_ACCOUNT_BALANCE 위반. 이미 쓴 돈을 회수하려는 상황이라 조용히 넘기면 안 된다.
            throw new BusinessException(HttpStatus.CONFLICT, "잔액이 부족해 회수할 수 없습니다.");
        }

        return tx.getId();
    }

    /**
     * 이 미션 기록으로 지금까지 실제 지급된 순액. 0보다 크면 이미 받은 상태다.
     *
     * <p>지급 → 회수 → 재지급이 같은 기록에 쌓이므로 마지막 거래 하나만 봐서는 판단할 수 없다.
     */
    public long netPaidAmount(Long missionLogId) {
        return transactionMapper.findByMissionLogId(missionLogId).stream()
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    /** 회수 대상이 되는 마지막 지급 거래. 없으면 {@code null}. */
    public Transaction lastPayment(Long missionLogId) {
        List<Transaction> transactions = transactionMapper.findByMissionLogId(missionLogId);
        Transaction last = null;
        for (Transaction tx : transactions) {
            if (TYPE_REWARD.equals(tx.getTxType()) || TYPE_RE_PAYMENT.equals(tx.getTxType())) {
                last = tx;
            }
        }
        return last;
    }

    /**
     * FR-020 입금 알림은 팀원 전원에게 간다. 탈퇴자와 이 종류를 끈 사람은 뺀다.
     *
     * <p>끈 사람에게는 행 자체를 만들지 않는다 — 나중에 다시 켰을 때 그동안의 알림이 몰려오면
     * 끈 의미가 없기 때문이다. 꺼둔 사람 목록은 한 번에 받아 멤버마다 조회하지 않는다.
     */
    private void notifyTeam(Member earner, Long missionLogId, int amount) {
        String content = "%s님이 오늘 미션을 완료하고 %,d원을 모았어요".formatted(earner.getNickname(), amount);
        Set<Long> optedOut = notificationPolicy.disabledMemberIds(
                earner.getChallengeId(), NotificationType.MISSION_REWARD);

        for (Member receiver : memberMapper.findByChallengeId(earner.getChallengeId())) {
            if ("LEFT".equals(receiver.getStatus()) || optedOut.contains(receiver.getId())) {
                continue;
            }
            Notification noti = new Notification();
            noti.setReceiverMemberId(receiver.getId());
            noti.setNotiType(NotificationType.MISSION_REWARD.name());
            noti.setContent(content);
            noti.setTargetType("MISSION_LOG");
            noti.setTargetId(missionLogId);
            noti.setIsRead("N");
            notificationMapper.insert(noti);
        }
    }


    /**
     * FR-045 팀 전체 목표 달성 알림. <b>마지막 주자가 자기 목표를 채우는 순간</b>에만 한 번 나간다.
     *
     * <p>이 알림이 필요한 이유는 팀 달성이 "내 행동"으로 발생하지 않기 때문이다. 이미 목표를 채운
     * 사람은 미션을 하지 않으므로 완료 화면에 오지 않고, 그러면 팀이 다 채웠다는 소식을 받을 길이
     * 없다. 알림에는 {@code isRead} 가 있어 "이미 봤는지"도 자연히 해결된다(프론트 §12 요청).
     *
     * <p>대상은 미션이 아니라 챌린지라 {@code targetType} 이 {@code CHALLENGE} 다 — 그동안
     * {@code MISSION_LOG} 하나뿐이었던 값이 여기서 늘어난다.
     */
    private void notifyTeamGoalAchieved(Member earner, int paidAmount) {
        Long challengeId = earner.getChallengeId();
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null || challenge.getTargetAmount() == null) {
            return;
        }
        if (!goalService.teamJustAchieved(
                challengeId, earner.getId(), challenge.getTargetAmount(), paidAmount)) {
            return;
        }

        String content = "%s 팀원 모두가 목표를 달성했어요!".formatted(challenge.getTitle());
        Set<Long> optedOut = notificationPolicy.disabledMemberIds(
                challengeId, NotificationType.TEAM_GOAL_ACHIEVED);

        for (Member receiver : memberMapper.findByChallengeId(challengeId)) {
            if ("LEFT".equals(receiver.getStatus()) || optedOut.contains(receiver.getId())) {
                continue;
            }
            Notification noti = new Notification();
            noti.setReceiverMemberId(receiver.getId());
            noti.setNotiType(NotificationType.TEAM_GOAL_ACHIEVED.name());
            noti.setContent(content);
            noti.setTargetType("CHALLENGE");
            noti.setTargetId(challengeId);
            noti.setIsRead("N");
            notificationMapper.insert(noti);
        }
    }

    private Account requireAccount(Long memberId) {
        Account account = accountMapper.findByMemberId(memberId);
        if (account == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다.");
        }
        return account;
    }
}
