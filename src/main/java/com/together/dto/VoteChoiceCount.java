package com.together.dto;

import lombok.Getter;
import lombok.Setter;

/** 투표 집계 한 줄. 아무도 고르지 않은 선택지는 행 자체가 없으므로 서비스가 0 으로 채운다. */
@Getter
@Setter
public class VoteChoiceCount {

    /** FIXED / AI */
    private String choice;
    private Integer count;
}
