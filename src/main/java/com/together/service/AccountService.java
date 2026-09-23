package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.Account;
import com.together.dto.Challenge;
import com.together.dto.Member;
import com.together.dto.MemberBalance;
import com.together.dto.response.AccountResponses;
import com.together.mapper.AccountMapper;
import com.together.mapper.ChallengeMapper;
import com.together.mapper.MemberMapper;
import com.together.mapper.TransactionMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * FR-017, FR-018. 가상 계좌 조회.
 *
 * <p>팀원의 <b>잔액</b>은 공개하지만 항목별 <b>거래 내역</b>은 본인만 본다(2026-08-20 확정).
 * 거래 내역은 활동 공백과 회수 이력까지 드러나 잔액과 민감도가 다르기 때문이다.
 */
@Service
public class AccountService {

    private final AccountMapper accountMapper;
    private final ChallengeMapper challengeMapper;
    private final MemberMapper memberMapper;
    private final TransactionMapper transactionMapper;
    private final MemberAccessService memberAccess;
    private final GoalService goalService;

    public AccountService(AccountMapper accountMapper,
                          ChallengeMapper challengeMapper,
                          MemberMapper memberMapper,
                          TransactionMapper transactionMapper,
                          MemberAccessService memberAccess,
                          GoalService goalService) {
        this.accountMapper = accountMapper;
        this.challengeMapper = challengeMapper;
        this.memberMapper = memberMapper;
        this.transactionMapper = transactionMapper;
        this.memberAccess = memberAccess;
        this.goalService = goalService;
    }

    /** 챌린지 전체 계좌 합산(화면 3·8). 탈퇴자는 집계에서 빠진다. */
    public AccountResponses.ChallengeAccounts challengeAccounts(Long userId, Long challengeId) {
        memberAccess.requireActiveMember(userId, challengeId);

        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "챌린지를 찾을 수 없습니다.");
        }

        long totalBalance = accountMapper.sumBalanceByChallengeId(challengeId);
        List<AccountResponses.ChallengeAccounts.Item> items =
                memberMapper.findWithBalanceByChallengeId(challengeId).stream()
                        .filter(m -> "ACTIVE".equals(m.getStatus()))
                        .map(this::toItem)
                        .toList();

        return new AccountResponses.ChallengeAccounts(
                totalBalance,
                challenge.getTargetAmount(),
                goalService.progressRate(challengeId, totalBalance, challenge.getTargetAmount()),
                items);
    }

    public AccountResponses.MyAccount myAccount(Long userId, Long challengeId) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        Account account = requireAccount(me.getId());
        return new AccountResponses.MyAccount(account.getId(), me.getId(), account.getBalance());
    }

    /** 본인 거래 내역(화면 13). 팀원 것을 보는 엔드포인트는 두지 않는다. 오프셋 페이지네이션. */
    public AccountResponses.TransactionList myTransactions(Long userId, Long challengeId,
                                                           Integer page, Integer size) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        Account account = requireAccount(me.getId());
        int pageNo = Paging.page(page);
        int pageSize = Paging.size(size);

        List<AccountResponses.TransactionList.Item> items =
                transactionMapper.findByAccountId(account.getId(), Paging.offset(pageNo, pageSize), pageSize)
                        .stream()
                        .map(t -> new AccountResponses.TransactionList.Item(
                                t.getId(), t.getMissionLogId(), t.getOriginalTransactionId(),
                                t.getTxType(), t.getAmount(), t.getCreatedAt()))
                        .toList();

        return new AccountResponses.TransactionList(
                items, pageNo, pageSize, transactionMapper.countByAccountId(account.getId()));
    }

    private AccountResponses.ChallengeAccounts.Item toItem(MemberBalance m) {
        return new AccountResponses.ChallengeAccounts.Item(
                m.getMemberId(), m.getNickname(), m.getBalance());
    }

    private Account requireAccount(Long memberId) {
        Account account = accountMapper.findByMemberId(memberId);
        if (account == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다.");
        }
        return account;
    }
}
