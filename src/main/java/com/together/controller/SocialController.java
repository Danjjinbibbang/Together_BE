package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.request.CommentCreateRequest;
import com.together.dto.request.ReactionToggleRequest;
import com.together.dto.request.TeamMessageCreateRequest;
import com.together.dto.response.SocialResponses;
import com.together.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 댓글·리액션과 팀 응원 메시지.
 *
 * <p>댓글과 리액션은 입금 건이 아니라 미션 기록에 달린다(화면 14). 응원 메시지는 대상이 미션이
 * 아니라 챌린지 팀 전체라 경로가 {@code /challenges} 하위다(FR-044).
 */
@Tag(name = "Social")
@RestController
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @Operation(summary = "댓글 목록 (화면 14)")
    @GetMapping("/mission-logs/{missionLogId}/comments")
    public SocialResponses.CommentList listComments(@LoginUserId Long userId,
                                                    @PathVariable Long missionLogId) {
        return socialService.listComments(userId, missionLogId);
    }

    @Operation(summary = "댓글 작성 (화면 14)")
    @PostMapping("/mission-logs/{missionLogId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public SocialResponses.CommentCreated createComment(
            @LoginUserId Long userId,
            @PathVariable Long missionLogId,
            @Valid @RequestBody CommentCreateRequest request) {
        return socialService.createComment(userId, missionLogId, request);
    }

    @Operation(summary = "댓글 삭제 (작성자 본인만)")
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@LoginUserId Long userId, @PathVariable Long commentId) {
        socialService.deleteComment(userId, commentId);
    }

    // ------------------------------------------------------------------ 팀 응원 메시지(FR-044)

    @Operation(summary = "팀 응원 메시지 보내기 (팀원 누구나, 팀 전체에 알림)")
    @PostMapping("/challenges/{challengeId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public SocialResponses.TeamMessageSent sendTeamMessage(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Valid @RequestBody TeamMessageCreateRequest request) {
        return socialService.sendTeamMessage(userId, challengeId, request);
    }

    @Operation(summary = "팀 응원 메시지 목록. 커서 페이지네이션")
    @GetMapping("/challenges/{challengeId}/messages")
    public SocialResponses.TeamMessageList listTeamMessages(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Parameter(description = "직전 응답의 nextCursor. 첫 페이지는 생략")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "기본 20, 최대 100")
            @RequestParam(required = false) Integer size) {
        return socialService.listTeamMessages(userId, challengeId, cursor, size);
    }

    @Operation(summary = "팀 응원 메시지 수정 (작성자 본인만). 팀원 알림함의 문구도 같이 바뀐다")
    @PatchMapping("/challenges/{challengeId}/messages/{messageId}")
    public SocialResponses.TeamMessageUpdated editTeamMessage(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @PathVariable Long messageId,
            @Valid @RequestBody TeamMessageCreateRequest request) {
        return socialService.editTeamMessage(userId, challengeId, messageId, request);
    }

    @Operation(summary = "팀 응원 메시지 삭제 (작성자 본인만). 팀원 알림함에서도 사라진다")
    @DeleteMapping("/challenges/{challengeId}/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeamMessage(@LoginUserId Long userId,
                                  @PathVariable Long challengeId,
                                  @PathVariable Long messageId) {
        socialService.deleteTeamMessage(userId, challengeId, messageId);
    }

    // ------------------------------------------------------------------ 리액션

    @Operation(summary = "리액션 집계 (화면 14)")
    @GetMapping("/mission-logs/{missionLogId}/reactions")
    public SocialResponses.ReactionSummary reactions(@LoginUserId Long userId,
                                                     @PathVariable Long missionLogId) {
        return socialService.reactions(userId, missionLogId);
    }

    @Operation(summary = "리액션 등록/토글 (화면 14)")
    @PostMapping("/mission-logs/{missionLogId}/reactions")
    public SocialResponses.ReactionToggled toggleReaction(
            @LoginUserId Long userId,
            @PathVariable Long missionLogId,
            @Valid @RequestBody ReactionToggleRequest request) {
        return socialService.toggleReaction(userId, missionLogId, request);
    }
}
