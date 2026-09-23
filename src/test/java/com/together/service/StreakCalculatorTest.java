package com.together.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * FR-025 스트릭 계산 규칙. 매퍼가 주는 날짜 목록을 어떻게 세는지가 전부라 DB 없이 본다.
 */
class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 25);

    @Test
    @DisplayName("오늘부터 연속이면 오늘까지 센다")
    void countsFromToday() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 23));

        assertThat(StreakCalculator.count(dates, TODAY)).isEqualTo(3);
    }

    @Test
    @DisplayName("오늘 아직 안 했어도 어제까지 이어져 있으면 유지된다")
    void keepsStreakWhenTodayIsStillOpen() {
        List<LocalDate> dates = List.of(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 23));

        // 자정을 넘기자마자 0으로 보이면 "오늘 하면 이어진다"는 동기부여가 사라진다
        assertThat(StreakCalculator.count(dates, TODAY)).isEqualTo(2);
    }

    @Test
    @DisplayName("그제까지만 있으면 끊긴 것으로 본다")
    void breaksWhenYesterdayIsMissing() {
        List<LocalDate> dates = List.of(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 22));

        assertThat(StreakCalculator.count(dates, TODAY)).isZero();
    }

    @Test
    @DisplayName("중간에 빠진 날이 있으면 거기서 멈춘다")
    void stopsAtTheGap() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 24),
                // 8/23 이 빠져 있다
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 21));

        assertThat(StreakCalculator.count(dates, TODAY)).isEqualTo(2);
    }

    @Test
    @DisplayName("순서가 섞이거나 중복이 있어도 결과는 같다")
    void ignoresOrderAndDuplicates() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 24));

        assertThat(StreakCalculator.count(dates, TODAY)).isEqualTo(2);
    }

    @Test
    @DisplayName("기록이 없으면 0")
    void zeroWhenEmpty() {
        assertThat(StreakCalculator.count(List.of(), TODAY)).isZero();
    }
}
