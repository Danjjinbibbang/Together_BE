package com.together.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * ACCOUNT 테이블 행. 가상 계좌(FR-017). Member 와 1:1.
 */
@Getter
@Setter
public class Account {

    private Long id;
    private Long memberId;
    private Long balance;
}
