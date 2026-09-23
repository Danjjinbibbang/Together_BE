package com.together.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Account 태그 응답 모음. */
public final class AccountResponses {

    private AccountResponses() {
    }

    /**
     * {@code GET /challenges/{challengeId}/accounts} (화면 3·8).
     *
     * <p>팀원 잔액은 공개하지만 항목별 거래 내역은 본인만 본다(2026-08-20 확정).
     * 집계는 {@code STATUS=ACTIVE} 멤버만 포함한다.
     */
    public record ChallengeAccounts(
            Long totalBalance,
            Long goalAmount,
            Integer progressRate,
            List<Item> accounts
    ) {

        public record Item(Long memberId, String nickname, Long balance) {
        }
    }

    /** {@code GET /challenges/{challengeId}/accounts/me} (화면 13) */
    public record MyAccount(Long accountId, Long memberId, Long balance) {
    }

    /**
     * {@code GET /challenges/{challengeId}/accounts/me/transactions} (화면 13). 본인 전용.
     *
     * <p>오프셋 페이지네이션. {@code totalCount} 로 전체 페이지 수를 계산할 수 있다.
     */
    public record TransactionList(List<Item> transactions, int page, int size, int totalCount) {

        public record Item(
                Long transactionId,
                Long missionLogId,
                Long originalTransactionId,
                String type,
                Long amount,
                LocalDateTime createdAt
        ) {
        }
    }
}
