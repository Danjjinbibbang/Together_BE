package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.request.MemberUpdateRequest;
import com.together.dto.response.MemberResponses;
import com.together.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member")
@RestController
@RequestMapping("/challenges/{challengeId}/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "챌린지 내 멤버 목록")
    @GetMapping
    public MemberResponses.MemberList list(@LoginUserId Long userId,
                                           @PathVariable Long challengeId) {
        return memberService.list(userId, challengeId);
    }

    @Operation(summary = "내 멤버 정보 조회")
    @GetMapping("/me")
    public MemberResponses.Me me(@LoginUserId Long userId, @PathVariable Long challengeId) {
        return memberService.me(userId, challengeId);
    }

    @Operation(summary = "닉네임 등 내 정보 수정")
    @PatchMapping("/me")
    public MemberResponses.Updated updateMe(@LoginUserId Long userId,
                                            @PathVariable Long challengeId,
                                            @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.updateMe(userId, challengeId, request);
    }

    @Operation(summary = "챌린지 탈퇴 (본인, STATUS→LEFT)")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@LoginUserId Long userId, @PathVariable Long challengeId) {
        memberService.leave(userId, challengeId);
    }
}
