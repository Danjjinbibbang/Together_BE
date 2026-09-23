package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.CalendarEntry;
import com.together.dto.MyMissionLogItem;
import com.together.dto.response.MeResponses;
import com.together.mapper.MissionLogMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * FR-025② · FR-027 · FR-040 · FR-041. 내 기록 캘린더(화면 16).
 *
 * <p>다른 조회와 달리 챌린지를 가로지른다. 챌린지별로 나눠 받으면 프론트가 참여 챌린지 수만큼
 * 호출해 직접 합쳐야 하고, 전체 활동 연속일은 애초에 한 챌린지 안에서 셀 수 없다.
 *
 * <p>권한 검사는 {@code MemberAccessService} 를 쓰지 않는다 — 조회 범위가 "내가 멤버인 기록"
 * 으로 SQL 안에서 이미 좁혀져 있어, 남의 기록이 섞일 수 없기 때문이다.
 */
@Service
public class MeService {

    /** 스트릭을 세려고 읽어오는 최대 일수. 1년을 넘기면 어차피 중간에 끊긴 날이 있다. */
    private static final int STREAK_LOOKBACK_DAYS = 400;

    /** 한 번에 조회할 수 있는 캘린더 범위. 월 단위 화면이라 넉넉히 잡아도 1년이면 충분하다. */
    private static final int MAX_RANGE_DAYS = 366;

    private final MissionLogMapper missionLogMapper;
    private final Clock clock;

    public MeService(MissionLogMapper missionLogMapper, Clock clock) {
        this.missionLogMapper = missionLogMapper;
        this.clock = clock;
    }

    /**
     * 캘린더 점 데이터와 전체 활동 연속일(FR-025②).
     *
     * <p>범위를 생략하면 이번 달이다. 활동이 없는 날은 담지 않는다 — 프론트가 날짜 격자를
     * 어차피 직접 그리기 때문에 빈 날까지 채우면 응답만 두 배가 된다.
     */
    public MeResponses.Calendar calendar(Long userId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : start.withDayOfMonth(start.lengthOfMonth());

        if (end.isBefore(start)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "to 는 from 보다 앞설 수 없습니다.");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "조회 범위는 " + MAX_RANGE_DAYS + "일을 넘을 수 없습니다.");
        }

        Map<LocalDate, List<MeResponses.Calendar.Day.ChallengeEntry>> byDate = new LinkedHashMap<>();
        Map<LocalDate, Long> rewardByDate = new LinkedHashMap<>();
        for (CalendarEntry entry : missionLogMapper.findCalendarEntriesByUserId(userId, start, end)) {
            byDate.computeIfAbsent(entry.getActivityDate(), d -> new ArrayList<>())
                    .add(new MeResponses.Calendar.Day.ChallengeEntry(
                            entry.getChallengeId(), entry.getChallengeTitle(), entry.getThemeColor(),
                            entry.getMissionCount(), entry.getRewardAmount()));
            rewardByDate.merge(entry.getActivityDate(), entry.getRewardAmount(), Long::sum);
        }

        List<MeResponses.Calendar.Day> days = byDate.entrySet().stream()
                .map(e -> new MeResponses.Calendar.Day(
                        e.getKey(), rewardByDate.get(e.getKey()), e.getValue()))
                .toList();

        int totalStreak = StreakCalculator.count(
                missionLogMapper.findActivityDatesByUserId(userId, STREAK_LOOKBACK_DAYS), today);

        return new MeResponses.Calendar(start, end, days, totalStreak);
    }

    /** FR-040 날짜별 내 미션 목록. 날짜를 생략하면 오늘이다. */
    public MeResponses.MyMissionLogList missionLogs(Long userId, LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(clock);

        List<MeResponses.MyMissionLogList.Item> items =
                missionLogMapper.findMyLogsByUserIdAndDate(userId, target).stream()
                        .map(MeService::toItem)
                        .toList();

        return new MeResponses.MyMissionLogList(target, items);
    }

    private static MeResponses.MyMissionLogList.Item toItem(MyMissionLogItem row) {
        return new MeResponses.MyMissionLogList.Item(
                row.getMissionLogId(), row.getChallengeId(), row.getChallengeTitle(),
                row.getThemeColor(), row.getMissionTitle(), row.getAssignedDate(),
                row.getStatus(), row.getReviewedBy(), row.getRewardAmount(),
                "Y".equals(row.getIsPublic()));
    }
}
