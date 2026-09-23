package com.together.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 업무 규칙 위반. 상태 코드는 Notion API 명세서의 Response Codes 열을 따른다.
 * (예: 초대코드 참여 시 닉네임 중복 → 409)
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}