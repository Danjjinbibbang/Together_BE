package com.together.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * FR-025 스트릭 — 오늘(또는 어제)부터 하루도 빠지지 않고 이어진 날 수.
 *
 * <p>오늘 아직 안 했어도 어제까지 이어져 있으면 유지로 본다. 자정을 넘기자마자 0으로 보이면
 * "오늘 하면 이어진다"는 동기부여가 사라지기 때문이다.
 *
 * <p>SQL 로 연속 판정을 쓰면(LAG/그룹핑) 읽기 어려워지는 데다 무엇을 성공으로 칠지가 정책이라
 * 서비스 계층에 뒀다. 매퍼는 날짜 목록만 준다.
 */
final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * @param dates 활동/성공한 날짜들. 중복이나 정렬 상태는 상관없다
     * @param today 기준일
     */
    static int count(Collection<LocalDate> dates, LocalDate today) {
        Set<LocalDate> days = new HashSet<>(dates);
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
