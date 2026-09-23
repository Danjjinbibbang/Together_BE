package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.request.ChallengeCreateRequest;
import com.together.dto.request.ChallengeJoinRequest;
import com.together.dto.request.ChallengeUpdateRequest;
import com.together.dto.request.MissionModeUpdateRequest;
import com.together.dto.request.OwnerTransferRequest;
import com.together.dto.response.ChallengeResponses;
import com.together.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Challenge")
@RestController
@RequestMapping("/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @Operation(summary = "내가 속한 챌린지 목록 (화면 4)")
    @GetMapping
    public ChallengeResponses.ChallengeList list(@LoginUserId Long userId) {
        return challengeService.list(userId);
    }

    @Operation(summary = "챌린지 생성 (화면 5·6, 요청자가 방장이 됨)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeResponses.Created create(@LoginUserId Long userId,
                                             @Valid @RequestBody ChallengeCreateRequest request) {
        return challengeService.create(userId, request);
    }

    @Operation(summary = "챌린지 상세 (그룹 홈, 화면 8)")
    @GetMapping("/{challengeId}")
    public ChallengeResponses.Detail detail(@LoginUserId Long userId,
                                            @PathVariable Long challengeId) {
        return challengeService.detail(userId, challengeId);
    }

    @Operation(summary = "챌린지 정보 수정 (방장 전용)")
    @PatchMapping("/{challengeId}")
    public ChallengeResponses.Detail update(@LoginUserId Long userId,
                                            @PathVariable Long challengeId,
                                            @Valid @RequestBody ChallengeUpdateRequest request) {
        return challengeService.update(userId, challengeId, request);
    }

    @Operation(summary = "초대코드/링크 조회 (화면 6)")
    @GetMapping("/{challengeId}/invite-code")
    public ChallengeResponses.InviteCode inviteCode(@LoginUserId Long userId,
                                                    @PathVariable Long challengeId) {
        return challengeService.inviteCode(userId, challengeId);
    }

    @Operation(summary = "초대코드로 챌린지 미리보기 (화면 7, 참여 전)")
    @GetMapping("/by-invite-code/{inviteCode}")
    public ChallengeResponses.InvitePreview previewByInviteCode(
            @LoginUserId Long userId,
            @PathVariable String inviteCode) {
        return challengeService.previewByInviteCode(userId, inviteCode);
    }

    @Operation(summary = "초대코드로 참여 (화면 7, 닉네임 필요). challengeId 는 서버가 코드로 찾는다")
    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeResponses.Joined join(@LoginUserId Long userId,
                                          @Valid @RequestBody ChallengeJoinRequest request) {
        return challengeService.join(userId, request);
    }

    @Operation(summary = "미션 운영 방식 설정 (방장 전용, 화면 9)")
    @PatchMapping("/{challengeId}/mission-mode")
    public ChallengeResponses.MissionMode updateMissionMode(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody MissionModeUpdateRequest request) {
        return challengeService.updateMissionMode(userId, challengeId, request);
    }

    @Operation(summary = "방장 양도 (방장 전용)")
    @PatchMapping("/{challengeId}/owner")
    public ChallengeResponses.OwnerTransferred transferOwner(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody OwnerTransferRequest request) {
        return challengeService.transferOwner(userId, challengeId, request);
    }
}
