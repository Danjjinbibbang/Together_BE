package com.together.service;

/**
 * 페이지 파라미터 정리를 한 곳에 모은다.
 *
 * <p>프론트가 보낸 값을 그대로 SQL 에 넘기면 `size=100000` 같은 요청 하나로 목록 전체를 긁어갈 수
 * 있어, 서비스 진입 시점에 잘라낸다.
 */
final class Paging {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;

    private Paging() {
    }

    /** 1 미만이면 기본값, 상한을 넘으면 상한으로 자른다. */
    static int size(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(requested, MAX_SIZE);
    }

    /** 음수 페이지는 첫 페이지로 본다. */
    static int page(Integer requested) {
        return requested == null || requested < 0 ? 0 : requested;
    }

    static int offset(int page, int size) {
        return page * size;
    }
}
