package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.request.AiMissionGenerateRequest;
import com.together.dto.request.DisputeRequest;
import com.together.dto.request.MissionCreateRequest;
import com.together.dto.request.MissionSubmitRequest;
import com.together.dto.request.MissionUpdateRequest;
import com.together.dto.request.ResubmitRequest;
import com.together.dto.request.ReviewRequest;
import com.together.dto.response.MissionResponses;
import com.together.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 미션 마스터 관리, 오늘의 미션, 수행 기록과 FR-012 재판정 흐름.
 *
 * <p>경로 접두사가 {@code /challenges}, {@code /missions}, {@code /mission-logs} 셋으로 갈려
 * 클래스 수준 매핑을 두지 않았다 — API 명세서의 경로를 그대로 따른 결과다.
 */
@Tag(name = "Mission")
@RestController
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    // ------------------------------------------------------------------ 미션 관리(방장)

    @Operation(summary = "챌린지의 미션 목록 조회 (방장 관리용). 오프셋 페이지네이션")
    @GetMapping("/challenges/{challengeId}/missions")
    public MissionResponses.MissionList listMissions(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Parameter(description = "0부터 시작, 기본 0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "기본 20, 최대 100")
            @RequestParam(required = false) Integer size) {
        return missionService.listMissions(userId, challengeId, page, size);
    }

    @Operation(summary = "미션 직접 생성 (방장 전용)")
    @PostMapping("/challenges/{challengeId}/missions")
    @ResponseStatus(HttpStatus.CREATED)
    public MissionResponses.MissionCreated createMission(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody MissionCreateRequest request) {
        return missionService.createMission(userId, challengeId, request);
    }

    @Operation(summary = "AI 미션 제안 (저장하지 않고 후보만 반환)")
    @PostMapping("/challenges/{challengeId}/missions/ai-generate")
    public MissionResponses.AiSuggestions generateSuggestions(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody AiMissionGenerateRequest request) {
        return missionService.generateSuggestions(userId, challengeId, request);
    }

    @Operation(summary = "미션 수정/비활성화 (방장 전용)")
    @PatchMapping("/missions/{missionId}")
    public MissionResponses.MissionUpdated updateMission(
            @LoginUserId Long userId,
            @PathVariable Long missionId,
            @Valid @RequestBody MissionUpdateRequest request) {
        return missionService.updateMission(userId, missionId, request);
    }

    // ------------------------------------------------------------------ 오늘의 미션·제출

    @Operation(summary = "미션 삭제 (방장 전용). 배정된 적 없는 미션만 가능")
    @DeleteMapping("/missions/{missionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMission(@LoginUserId Long userId, @PathVariable Long missionId) {
        missionService.deleteMission(userId, missionId);
    }

    @Operation(summary = "오늘의 미션 카드뽑기 결과/요청 (화면 10)")
    @GetMapping("/challenges/{challengeId}/missions/today")
    public MissionResponses.Today today(@LoginUserId Long userId,
                                        @PathVariable Long challengeId) {
        return missionService.today(userId, challengeId);
    }

    @Operation(summary = "미션 수행 최초 제출 (화면 11)")
    @PostMapping("/missions/{missionId}/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public MissionResponses.Submitted submit(@LoginUserId Long userId,
                                             @PathVariable Long missionId,
                                             @Valid @RequestBody MissionSubmitRequest request) {
        return missionService.submit(userId, missionId, request);
    }

    // ------------------------------------------------------------------ 기록 조회

    @Operation(summary = "미션 로그 목록 (캘린더/피드용, 화면 16). 커서 페이지네이션")
    @GetMapping("/challenges/{challengeId}/mission-logs")
    public MissionResponses.MissionLogList feed(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Parameter(description = "특정 날짜만 (캘린더용)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "특정 멤버만")
            @RequestParam(required = false) Long memberId,
            @Parameter(description = "직전 응답의 nextCursor. 첫 페이지는 생략")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "기본 20, 최대 100")
            @RequestParam(required = false) Integer size) {
        return missionService.feed(userId, challengeId, date, memberId, cursor, size);
    }

    @Operation(summary = "미션 로그 상세 (화면 12·14)")
    @GetMapping("/mission-logs/{missionLogId}")
    public MissionResponses.MissionLogDetailResponse detail(@LoginUserId Long userId,
                                                            @PathVariable Long missionLogId) {
        return missionService.detail(userId, missionLogId);
    }

    // ------------------------------------------------------------------ FR-012 재판정 흐름

    @Operation(summary = "AI 오탐 이의제기 (화면 12)")
    @PostMapping("/mission-logs/{missionLogId}/dispute")
    public MissionResponses.StatusChanged dispute(@LoginUserId Long userId,
                                                  @PathVariable Long missionLogId,
                                                  @Valid @RequestBody DisputeRequest request) {
        return missionService.dispute(userId, missionLogId, request);
    }

    @Operation(summary = "그대로 재검토 요청")
    @PatchMapping("/mission-logs/{missionLogId}/dispute/recheck")
    public MissionResponses.StatusChanged recheck(@LoginUserId Long userId,
                                                  @PathVariable Long missionLogId) {
        return missionService.recheck(userId, missionLogId);
    }

    @Operation(summary = "수정 후 재제출")
    @PatchMapping("/mission-logs/{missionLogId}/dispute/resubmit")
    public MissionResponses.StatusChanged resubmit(@LoginUserId Long userId,
                                                   @PathVariable Long missionLogId,
                                                   @Valid @RequestBody ResubmitRequest request) {
        return missionService.resubmit(userId, missionLogId, request);
    }

    @Operation(summary = "방장 무응답 자동 승인 요청 (FR-012c, 작성자 본인)")
    @PostMapping("/mission-logs/{missionLogId}/review/auto-approve")
    public MissionResponses.Reviewed autoApprove(@LoginUserId Long userId,
                                                 @PathVariable Long missionLogId) {
        return missionService.autoApprove(userId, missionLogId);
    }

    @Operation(summary = "방장/2차 판독 재판정")
    @PostMapping("/mission-logs/{missionLogId}/review")
    public MissionResponses.Reviewed review(@LoginUserId Long userId,
                                            @PathVariable Long missionLogId,
                                            @Valid @RequestBody ReviewRequest request) {
        return missionService.review(userId, missionLogId, request);
    }
}
