package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.response.MeResponses;
import com.together.service.MeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 기록 캘린더(화면 16). 참여 중인 모든 챌린지를 한 화면에 모으는 유일한 조회다.
 *
 * <p>다른 조회가 전부 {@code /challenges/{id}} 하위인 것과 달리 경로에 챌린지가 없다 —
 * FR-040 이 "소속 챌린지"를 목록의 한 필드로 요구하기 때문이다.
 */
@Tag(name = "Me")
@RestController
@RequestMapping("/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @Operation(summary = "내 기록 캘린더 — 날짜별 점 데이터 + 전체 활동 연속일 (화면 16)")
    @GetMapping("/calendar")
    public MeResponses.Calendar calendar(
            @LoginUserId Long userId,
            @Parameter(description = "시작일. 생략하면 이번 달 1일")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "종료일. 생략하면 시작일이 속한 달의 말일")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return meService.calendar(userId, from, to);
    }

    @Operation(summary = "날짜별 내 미션 목록 (화면 16에서 날짜 선택)")
    @GetMapping("/mission-logs")
    public MeResponses.MyMissionLogList missionLogs(
            @LoginUserId Long userId,
            @Parameter(description = "조회할 날짜. 생략하면 오늘")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return meService.missionLogs(userId, date);
    }
}
