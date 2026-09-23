package com.together.service;

import com.together.dto.MemberActivityDate;
import com.together.mapper.MissionLogMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * FR-025① 챌린지별 스트릭 — 그 챌린지에서만의 연속 성공일.
 *
 * <p>전체 활동 연속일(FR-025②, {@link MeService})과 분리돼 있다. 팀원이 함께 보는 값이라
 * 다른 챌린지 활동이 섞이면 "저 사람은 우리 챌린지를 꾸준히 하고 있다"는 뜻이 흐려지기 때문이다.
 *
 * <p>그래서 기준도 다르다. 이쪽은 <b>성공한 날</b>만 세고(제출만 하고 반려된 날은 끊긴다),
 * 전체 활동 연속일은 제출한 날이면 인정한다.
 */
@Service
public class StreakService {

    /** 성공으로 치는 상태. 방장이 재판정으로 승인한 것도 성공이다. */
    private static final List<String> SUCCESS_STATUSES = List.of("AI_APPROVED", "OWNER_APPROVED");

    /**
     * 한 번에 읽어올 (멤버 × 성공한 날) 행 수 상한.
     * 멤버 20명 × 100일이면 2,000행이라, 스트릭이 이보다 길어질 일은 사실상 없다.
     */
    private static final int LOOKBACK_ROWS = 2000;

    private final MissionLogMapper missionLogMapper;
    private final Clock clock;

    public StreakService(MissionLogMapper missionLogMapper, Clock clock) {
        this.missionLogMapper = missionLogMapper;
        this.clock = clock;
    }

    /**
     * 챌린지 멤버 전원의 스트릭. 멤버마다 쿼리를 날리지 않으려고 한 번에 읽어 갈라 센다.
     *
     * <p>성공 기록이 없는 멤버는 맵에 없다 — 꺼내 쓸 때 0으로 보면 된다.
     */
    public Map<Long, Integer> byMember(Long challengeId) {
        LocalDate today = LocalDate.now(clock);

        Map<Long, List<LocalDate>> datesByMember = new HashMap<>();
        for (MemberActivityDate row
                : missionLogMapper.findSuccessDatesByChallengeId(challengeId, SUCCESS_STATUSES, LOOKBACK_ROWS)) {
            datesByMember.computeIfAbsent(row.getMemberId(), id -> new ArrayList<>())
                    .add(row.getActivityDate());
        }

        Map<Long, Integer> streaks = new HashMap<>();
        datesByMember.forEach((memberId, dates) ->
                streaks.put(memberId, StreakCalculator.count(dates, today)));
        return streaks;
    }
}
