package com.together.mapper;

import com.together.dto.Account;
import org.apache.ibatis.annotations.Param;

/**
 * ACCOUNT 테이블 접근. {@code resources/mapper/AccountMapper.xml} 과 1:1 대응한다.
 */
public interface AccountMapper {

    Account findById(@Param("id") Long id);

    /** Member 와 1:1 이므로 memberId 로 단건 조회된다(UK_ACCOUNT_MEMBER). */
    Account findByMemberId(@Param("memberId") Long memberId);

    /** 챌린지 전체 저축액. 참여자가 없으면 0. */
    Long sumBalanceByChallengeId(@Param("challengeId") Long challengeId);

    int insert(Account account);

    /**
     * 잔액을 {@code amount} 만큼 증감시킨다. 취소(REVERSAL)는 음수로 넘긴다.
     *
     * <p>덧셈을 DB 에서 처리해 동시 입금 시 갱신 유실을 막는다. 잔액이 음수가 되면
     * CK_ACCOUNT_BALANCE 제약에 걸려 실패한다 — 판단은 호출한 서비스가 한다.
     *
     * <p>거래 INSERT 와 반드시 같은 트랜잭션에서 호출해야 한다. DB 트리거를 두지 않았기 때문에
     * 이 호출을 빠뜨리면 잔액이 어긋난다.
     */
    int addBalance(@Param("id") Long id, @Param("amount") Long amount);
}
