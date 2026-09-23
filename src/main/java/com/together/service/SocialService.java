package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.Comment;
import com.together.dto.Notification;
import com.together.dto.NotificationType;
import com.together.dto.Member;
import com.together.dto.MissionLogDetail;
import com.together.dto.Reaction;
import com.together.dto.ReactionCount;
import com.together.dto.TeamMessage;
import com.together.dto.TeamMessageView;
import com.together.dto.request.CommentCreateRequest;
import com.together.dto.request.ReactionToggleRequest;
import com.together.dto.request.TeamMessageCreateRequest;
import com.together.dto.response.SocialResponses;
import com.together.config.MessageProperties;
import com.together.mapper.CommentMapper;
import com.together.mapper.NotificationMapper;
import com.together.mapper.MissionLogMapper;
import com.together.mapper.ReactionMapper;
import com.together.mapper.TeamMessageMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-022, FR-023. 미션 상세의 리액션과 댓글.
 *
 * <p>둘 다 입금 건이 아니라 미션 기록에 달린다. 같은 챌린지 참여자만 남길 수 있다.
 */
@Service
public class SocialService {

    /** 알림함에 넣을 미리보기 길이. 전문은 메시지 목록에서 읽는다. */
    private static final int NOTIFICATION_PREVIEW_LENGTH = 40;

    /** 알림이 어떤 대상에 매달렸는지 나타내는 값. 메시지를 되짚어 찾을 때 이 값으로 건다. */
    private static final String TEAM_MESSAGE_TARGET = "TEAM_MESSAGE";

    private final CommentMapper commentMapper;
    private final ReactionMapper reactionMapper;
    private final MissionLogMapper missionLogMapper;
    private final MemberAccessService memberAccess;
    private final TeamMessageMapper teamMessageMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationPolicy notificationPolicy;
    private final MessageProperties messageProperties;
    private final Clock clock;

    public SocialService(CommentMapper commentMapper,
                         ReactionMapper reactionMapper,
                         MissionLogMapper missionLogMapper,
                         MemberAccessService memberAccess,
                         TeamMessageMapper teamMessageMapper,
                         NotificationMapper notificationMapper,
                         NotificationPolicy notificationPolicy,
                         MessageProperties messageProperties,
                         Clock clock) {
        this.commentMapper = commentMapper;
        this.reactionMapper = reactionMapper;
        this.missionLogMapper = missionLogMapper;
        this.memberAccess = memberAccess;
        this.teamMessageMapper = teamMessageMapper;
        this.notificationMapper = notificationMapper;
        this.notificationPolicy = notificationPolicy;
        this.messageProperties = messageProperties;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ 댓글

    public SocialResponses.CommentList listComments(Long userId, Long missionLogId) {
        requireTeammate(userId, missionLogId);
        return new SocialResponses.CommentList(
                commentMapper.findViewsByMissionLogId(missionLogId).stream()
                        .map(c -> new SocialResponses.CommentList.Item(
                                c.getCommentId(), c.getMemberId(), c.getNickname(),
                                c.getContent(), c.getCreatedAt()))
                        .toList());
    }

    @Transactional
    public SocialResponses.CommentCreated createComment(Long userId, Long missionLogId,
                                                        CommentCreateRequest request) {
        Member me = requireTeammate(userId, missionLogId);

        Comment comment = new Comment();
        comment.setMissionLogId(missionLogId);
        comment.setSenderMemberId(me.getId());
        comment.setContent(request.content());
        commentMapper.insert(comment);

        Comment saved = commentMapper.findById(comment.getId());
        return new SocialResponses.CommentCreated(
                saved.getId(), missionLogId, me.getId(), saved.getContent(), saved.getCreatedAt());
    }

    /** 작성자 본인만 지울 수 있다. */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다.");
        }
        Member author = memberAccess.requireMemberById(comment.getSenderMemberId());
        if (!author.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인이 쓴 댓글만 삭제할 수 있습니다.");
        }
        commentMapper.deleteById(commentId);
    }

    // ------------------------------------------------------------------ 리액션

    public SocialResponses.ReactionSummary reactions(Long userId, Long missionLogId) {
        Member me = requireTeammate(userId, missionLogId);

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ReactionCount row : reactionMapper.countByMissionLogId(missionLogId)) {
            counts.put(row.getReactionType(), row.getCount());
        }

        Reaction mine = reactionMapper.findByMissionLogIdAndSenderMemberId(missionLogId, me.getId());
        return new SocialResponses.ReactionSummary(counts, mine == null ? null : mine.getReactionType());
    }

    /**
     * 리액션 토글. 멤버당 1개라 세 갈래로 갈린다 — 없으면 등록, 같은 타입이면 해제,
     * 다른 타입이면 기존 걸 지우고 교체.
     */
    @Transactional
    public SocialResponses.ReactionToggled toggleReaction(Long userId, Long missionLogId,
                                                          ReactionToggleRequest request) {
        Member me = requireTeammate(userId, missionLogId);
        Reaction existing = reactionMapper.findByMissionLogIdAndSenderMemberId(missionLogId, me.getId());

        if (existing == null) {
            insertReaction(missionLogId, me.getId(), request.reactionType());
            return new SocialResponses.ReactionToggled(missionLogId, me.getId(), request.reactionType());
        }

        reactionMapper.deleteById(existing.getId());
        if (existing.getReactionType().equals(request.reactionType())) {
            // 같은 걸 다시 눌렀으니 해제 상태로 끝낸다.
            return new SocialResponses.ReactionToggled(missionLogId, me.getId(), null);
        }
        insertReaction(missionLogId, me.getId(), request.reactionType());
        return new SocialResponses.ReactionToggled(missionLogId, me.getId(), request.reactionType());
    }

    private void insertReaction(Long missionLogId, Long memberId, String reactionType) {
        Reaction reaction = new Reaction();
        reaction.setMissionLogId(missionLogId);
        reaction.setSenderMemberId(memberId);
        reaction.setReactionType(reactionType);
        reactionMapper.insert(reaction);
    }

    /** 같은 챌린지 참여자인지 확인하고 내 Member 를 돌려준다. */
    private Member requireTeammate(Long userId, Long missionLogId) {
        MissionLogDetail detail = missionLogMapper.findDetailById(missionLogId);
        if (detail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "미션 기록을 찾을 수 없습니다.");
        }
        return memberAccess.requireActiveMember(userId, detail.getChallengeId());
    }

    // ------------------------------------------------------------------ 팀 응원 메시지(FR-044)

    /**
     * 팀 전체에 한 줄 응원을 보낸다. 실시간 채팅이 아니라 단건 발송이라, 받는 쪽은 기존
     * 알림함(화면 15)에서 읽는다.
     *
     * <p>알림은 <b>보낸 사람을 빼고</b> ACTIVE 팀원에게만 간다. 자기 메시지를 자기 알림함에서
     * 다시 보는 것은 의미가 없고, 탈퇴자에게 보내면 읽을 사람이 없다.
     *
     * <p>도배는 하루 발송 건수로 막는다. 같은 사람이 하루에 {@code app.message.daily-limit-per-member}
     * 건을 넘기면 429 다 — 알림이 멤버수만큼 복제되는 구조라 한 사람의 남용이 팀 전체 알림함을 덮는다.
     */
    @Transactional
    public SocialResponses.TeamMessageSent sendTeamMessage(Long userId, Long challengeId,
                                                           TeamMessageCreateRequest request) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        requireUnderDailyLimit(me);

        TeamMessage message = new TeamMessage();
        message.setChallengeId(challengeId);
        message.setSenderMemberId(me.getId());
        message.setContent(request.content());
        teamMessageMapper.insert(message);

        TeamMessage saved = teamMessageMapper.findById(message.getId());
        int notified = notifyTeam(challengeId, me, saved);

        return new SocialResponses.TeamMessageSent(
                saved.getId(), challengeId, me.getId(), saved.getContent(), saved.getCreatedAt(),
                notified);
    }

    /** 팀 응원 메시지 목록. 최신순 커서 페이지네이션이다(피드·알림함과 같은 방식). */
    public SocialResponses.TeamMessageList listTeamMessages(Long userId, Long challengeId,
                                                            Long cursor, Integer size) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        int limit = Paging.size(size);

        List<TeamMessageView> rows =
                teamMessageMapper.findViewsByChallengeId(challengeId, cursor, limit + 1);
        boolean hasNext = rows.size() > limit;
        List<TeamMessageView> page = hasNext ? rows.subList(0, limit) : rows;
        Long nextCursor = hasNext ? page.get(page.size() - 1).getMessageId() : null;

        return new SocialResponses.TeamMessageList(
                page.stream()
                        .map(row -> new SocialResponses.TeamMessageList.Item(
                                row.getMessageId(), row.getSenderMemberId(), row.getNickname(),
                                row.getContent(), row.getCreatedAt(), row.getUpdatedAt(),
                                row.getSenderMemberId().equals(me.getId())))
                        .toList(),
                nextCursor);
    }

    private void requireUnderDailyLimit(Member me) {
        LocalDateTime todayStart = LocalDate.now(clock).atStartOfDay();
        int sentToday = teamMessageMapper.countBySenderSince(me.getId(), todayStart);
        if (sentToday >= messageProperties.dailyLimitPerMember()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "오늘 보낼 수 있는 응원 메시지를 다 썼습니다. 하루 "
                            + messageProperties.dailyLimitPerMember() + "건까지 보낼 수 있어요.");
        }
    }

    /**
     * 보낸 사람을 뺀 ACTIVE 팀원에게 알림을 만든다. 수신을 꺼둔 사람(§15 설정)은 행 자체를 만들지 않는다.
     *
     * <p>알림 본문에는 전문이 아니라 앞부분만 넣는다 — NOTIFICATION.CONTENT 가 500바이트라 한글
     * 200자 메시지가 그대로 들어가지 않고, 알림함은 어차피 미리보기 자리다.
     */
    private int notifyTeam(Long challengeId, Member sender, TeamMessage message) {
        Set<Long> optedOut = notificationPolicy.disabledMemberIds(challengeId, NotificationType.TEAM_CHEER);
        String content = notificationContent(sender, message.getContent());

        int notified = 0;
        for (Member receiver : memberAccess.activeMembersOf(challengeId)) {
            if (receiver.getId().equals(sender.getId()) || optedOut.contains(receiver.getId())) {
                continue;
            }
            Notification noti = new Notification();
            noti.setReceiverMemberId(receiver.getId());
            noti.setNotiType(NotificationType.TEAM_CHEER.name());
            noti.setContent(content);
            noti.setTargetType(TEAM_MESSAGE_TARGET);
            noti.setTargetId(message.getId());
            noti.setIsRead("N");
            notificationMapper.insert(noti);
            notified++;
        }
        return notified;
    }


    /**
     * 메시지를 고친다. 작성자 본인만 할 수 있다(FR-044).
     *
     * <p>핵심은 <b>팀원 알림함까지 따라가는 것</b>이다. 알림 본문은 발송 시점 스냅샷이라 원본만
     * 고치면 팀원에게는 옛 문구가 그대로 남는다. 같은 트랜잭션에서 알림 행의 본문도 갱신한다.
     *
     * <p>읽음 상태는 되돌리지 않는다. 오타 한 글자 고쳤다고 팀 전체 뱃지가 다시 켜지면
     * 그 자체가 알림을 남용하는 통로가 된다.
     */
    @Transactional
    public SocialResponses.TeamMessageUpdated editTeamMessage(Long userId, Long challengeId,
                                                              Long messageId,
                                                              TeamMessageCreateRequest request) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        TeamMessage message = requireOwnMessage(me, challengeId, messageId);

        teamMessageMapper.updateContent(messageId, request.content());
        notificationMapper.updateContentByTarget(
                TEAM_MESSAGE_TARGET, messageId, notificationContent(me, request.content()));

        TeamMessage updated = teamMessageMapper.findById(messageId);
        return new SocialResponses.TeamMessageUpdated(
                updated.getId(), challengeId, message.getSenderMemberId(), updated.getContent(),
                updated.getCreatedAt(), updated.getUpdatedAt());
    }

    /**
     * 메시지를 지운다. 작성자 본인만 할 수 있다.
     *
     * <p>팀원 알림함에서는 <b>흔적 없이 사라진다</b> — 알림 행을 실제로 지우므로 안 읽은 건이었다면
     * 뱃지 숫자도 함께 줄어든다. 반면 원본은 남긴다(soft delete): 지우고 다시 보내는 식으로 하루
     * 발송 한도를 우회하는 것을 막아야 하고, 누가 언제 무엇을 지웠는지도 남아야 하기 때문이다.
     */
    @Transactional
    public void deleteTeamMessage(Long userId, Long challengeId, Long messageId) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        requireOwnMessage(me, challengeId, messageId);

        teamMessageMapper.softDelete(messageId);
        notificationMapper.deleteByTarget(TEAM_MESSAGE_TARGET, messageId);
    }

    /**
     * 살아 있는 내 메시지인지 확인한다.
     *
     * <p>이미 지운 메시지는 404 다 — 남에게는 존재 자체가 사라진 것이라 "있는데 못 고친다"로
     * 보이면 안 된다. 남의 메시지는 403 이다(댓글 삭제와 같은 규칙).
     */
    private TeamMessage requireOwnMessage(Member me, Long challengeId, Long messageId) {
        TeamMessage message = teamMessageMapper.findById(messageId);
        if (message == null || message.getDeletedAt() != null
                || !message.getChallengeId().equals(challengeId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다.");
        }
        if (!message.getSenderMemberId().equals(me.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인이 보낸 메시지만 수정·삭제할 수 있습니다.");
        }
        return message;
    }

    private static String notificationContent(Member sender, String content) {
        return sender.getNickname() + "님의 응원: " + preview(content);
    }

    private static String preview(String content) {
        return content.length() <= NOTIFICATION_PREVIEW_LENGTH
                ? content
                : content.substring(0, NOTIFICATION_PREVIEW_LENGTH) + "…";
    }
}
