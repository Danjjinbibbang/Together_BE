package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.request.MissionModeVoteBallotRequest;
import com.together.dto.request.MissionModeVoteOpenRequest;
import com.together.dto.response.ChallengeResponses;
import com.together.service.MissionModeVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-008 확장 — 미션 운영 방식을 팀원 투표로 정한다(화면 9에서 진입).
 *
 * <p>방장 단독 결정({@code PATCH /challenges/{id}/mission-mode})은 그대로 남는다. 투표로 확정되면
 * 같은 필드가 갱신되므로, 그룹 홈이나 미션 뽑기 쪽은 아무것도 바꿀 필요가 없다.
 */
@Tag(name = "Challenge")
@RestController
@RequestMapping("/challenges/{challengeId}/mission-mode/vote")
public class MissionModeVoteController {

    private final MissionModeVoteService voteService;

    public MissionModeVoteController(MissionModeVoteService voteService) {
        this.voteService = voteService;
    }

    @Operation(summary = "미션 방식 투표 시작 (FR-008, 방장 전용)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeResponses.MissionModeVoteStatus open(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody(required = false) MissionModeVoteOpenRequest request) {
        return voteService.open(userId, challengeId, request);
    }

    @Operation(summary = "미션 방식 투표 현황 조회 (열린 투표가 없으면 직전 결과)")
    @GetMapping
    public ChallengeResponses.MissionModeVoteStatus status(@LoginUserId Long userId,
                                                           @PathVariable Long challengeId) {
        return voteService.status(userId, challengeId);
    }

    @Operation(summary = "투표하기 (다시 보내면 표가 바뀐다)")
    @PostMapping("/ballots")
    public ChallengeResponses.MissionModeVoteStatus castBallot(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody MissionModeVoteBallotRequest request) {
        return voteService.castBallot(userId, challengeId, request);
    }

    @Operation(summary = "투표 조기 마감 (방장 전용). 기한이 지나면 조회·투표 시점에 자동 마감된다")
    @PostMapping("/close")
    public ChallengeResponses.MissionModeVoteStatus close(@LoginUserId Long userId,
                                                          @PathVariable Long challengeId) {
        return voteService.close(userId, challengeId);
    }
}
