package com.together.mapper;

import com.together.dto.Transaction;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * TRANSACTIONS 테이블 접근. {@code resources/mapper/TransactionMapper.xml} 과 1:1 대응한다.
 */
public interface TransactionMapper {

    Transaction findById(@Param("id") Long id);

    /**
     * 계좌의 거래 내역을 최신순으로(IX_TRANSACTIONS_ACCOUNT). 오프셋 페이지네이션.
     *
     * <p>과거 내역이라 앞에 끼어드는 일이 드물어 커서까지는 필요 없다.
     */
    List<Transaction> findByAccountId(
            @Param("accountId") Long accountId, @Param("offset") int offset, @Param("limit") int limit);

    int countByAccountId(@Param("accountId") Long accountId);

    /** 한 미션 기록에 쌓인 거래. FR-012d 회수/재지급 이력 확인용. */
    List<Transaction> findByMissionLogId(@Param("missionLogId") Long missionLogId);

    int insert(Transaction transaction);
}
