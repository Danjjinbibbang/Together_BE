package com.together.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * TRANSACTIONS 테이블 행. 입금 기록(FR-017, FR-018, FR-012d).
 *
 * <p>사용자가 직접 만드는 API 는 없다. 재판정 로직 내부에서만 생성된다.
 */
@Getter
@Setter
public class Transaction {

    private Long id;
    private Long accountId;
    private Long missionLogId;
    /** REVERSAL / RE_PAYMENT 일 때만 값이 있다. 최초 지급이면 NULL */
    private Long originalTransactionId;
    /** REVERSAL 은 음수 */
    private Long amount;
    /** MISSION_REWARD(보상) / REVERSAL(취소) / RE_PAYMENT(재지급) */
    private String txType;
    private LocalDateTime createdAt;
}
