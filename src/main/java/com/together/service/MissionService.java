package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.config.ReviewProperties;
import com.together.dto.Account;
import com.together.dto.Challenge;
import com.together.dto.DailyMissionAssignment;
import com.together.dto.Member;
import com.together.dto.Mission;
import com.together.dto.MissionLog;
import com.together.dto.MissionLogDetail;
import com.together.dto.MissionLogFeedItem;
import com.together.dto.Notification;
import com.together.dto.NotificationType;
import com.together.dto.Transaction;
import com.together.dto.request.AiMissionGenerateRequest;
import com.together.dto.request.DisputeRequest;
import com.together.dto.request.MissionCreateRequest;
import com.together.dto.request.MissionSubmitRequest;
import com.together.dto.request.MissionUpdateRequest;
import com.together.dto.request.ResubmitRequest;
import com.together.dto.request.ReviewRequest;
import com.together.dto.response.MissionResponses;
import com.together.mapper.AccountMapper;
import com.together.mapper.ChallengeMapper;
import com.together.mapper.DailyMissionAssignmentMapper;
import com.together.mapper.MissionLogMapper;
import com.together.mapper.MissionMapper;
import com.together.mapper.NotificationMapper;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * FR-007 ~ FR-016. 미션 관리, 오늘의 미션 배정, 제출과 완료 판정.
 *
 * <p>상태 전이(FR-012 8단계)의 판단은 전부 여기서 한다. 매퍼는 받은 값을 기록만 한다.
 */
@Service
public class MissionService {

    // FR-012 상태
    private static final String SUBMITTED = "SUBMITTED";
    private static final String AI_APPROVED = "AI_APPROVED";
    private static final String AI_REJECTED = "AI_REJECTED";
    private static final String DISPUTE_REQUESTED = "DISPUTE_REQUESTED";
    private static final String RECHECK_REQUESTED = "RECHECK_REQUESTED";
    private static final String RESUBMITTED = "RESUBMITTED";
    private static final String OWNER_APPROVED = "OWNER_APPROVED";
    private static final String OWNER_REJECTED = "OWNER_REJECTED";

    private static final String REVIEWER_AI = "AI";
    private static final String REVIEWER_OWNER = "OWNER";
    /** 방장이 기한 내에 판정하지 않아 서버가 대신 확정한 경우(FR-012c). */
    private static final String REVIEWER_AUTO = "AUTO";

    /** 방장이 재판정할 수 있는 상태들. */
    private static final List<String> REVIEWABLE =
            List.of(DISPUTE_REQUESTED, RECHECK_REQUESTED, RESUBMITTED);

    private final MissionMapper missionMapper;
    private final DailyMissionAssignmentMapper assignmentMapper;
    private final MissionLogMapper missionLogMapper;
    private final ChallengeMapper challengeMapper;
    private final AccountMapper accountMapper;
    private final NotificationMapper notificationMapper;
    private final MemberAccessService memberAccess;
    private final GoalService goalService;
    private final RewardService rewardService;
    private final NotificationPolicy notificationPolicy;
    private final MissionSuggestionGenerator suggestionGenerator;
    private final ReviewProperties reviewProperties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public MissionService(MissionMapper missionMapper,
                          DailyMissionAssignmentMapper assignmentMapper,
                          MissionLogMapper missionLogMapper,
                          ChallengeMapper challengeMapper,
                          AccountMapper accountMapper,
                          NotificationMapper notificationMapper,
                          MemberAccessService memberAccess,
                          GoalService goalService,
                          RewardService rewardService,
                          NotificationPolicy notificationPolicy,
                          MissionSuggestionGenerator suggestionGenerator,
                          ReviewProperties reviewProperties,
                          Clock clock) {
        this.missionMapper = missionMapper;
        this.assignmentMapper = assignmentMapper;
        this.missionLogMapper = missionLogMapper;
        this.challengeMapper = challengeMapper;
        this.accountMapper = accountMapper;
        this.notificationMapper = notificationMapper;
        this.memberAccess = memberAccess;
        this.goalService = goalService;
        this.rewardService = rewardService;
        this.notificationPolicy = notificationPolicy;
        this.suggestionGenerator = suggestionGenerator;
        this.reviewProperties = reviewProperties;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ 미션 관리(방장)

    /** 방장 미션 관리 화면(23). 비활성 미션도 함께 준다. 오프셋 페이지네이션. */
    public MissionResponses.MissionList listMissions(Long userId, Long challengeId,
                                                     Integer page, Integer size) {
        memberAccess.requireOwner(userId, challengeId);
        int pageNo = Paging.page(page);
        int pageSize = Paging.size(size);

        List<MissionResponses.MissionList.Item> items =
                missionMapper.findAllByChallengeId(challengeId, Paging.offset(pageNo, pageSize), pageSize)
                        .stream()
                        .map(m -> new MissionResponses.MissionList.Item(
                                m.getId(), m.getTitle(), m.getSubmitType(),
                                m.getRewardMin(), m.getRewardMax(), m.getMinTextLength(),
                                toBoolean(m.getIsActive()), m.getCreatedByType()))
                        .toList();

        return new MissionResponses.MissionList(
                items, pageNo, pageSize, missionMapper.countByChallengeId(challengeId));
    }

    @Transactional
    public MissionResponses.MissionCreated createMission(Long userId, Long challengeId,
                                                         MissionCreateRequest request) {
        Member owner = memberAccess.requireOwner(userId, challengeId);
        validateRewardRange(request.rewardMin(), request.rewardMax());
        if ("TEXT".equals(request.submitType()) && request.minTextLength() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "텍스트 미션은 최소 글자수가 필요합니다.");
        }

        Mission mission = new Mission();
        mission.setChallengeId(challengeId);
        mission.setTitle(request.title());
        mission.setSubmitType(request.submitType());
        mission.setRewardMin(request.rewardMin());
        mission.setRewardMax(request.rewardMax());
        mission.setMinTextLength(request.minTextLength());
        mission.setCreatedByType(request.createdByType() == null ? "OWNER" : request.createdByType());
        mission.setCreatedByMemberId(owner.getId());
        mission.setIsActive("Y");
        missionMapper.insert(mission);

        return new MissionResponses.MissionCreated(
                mission.getId(), challengeId, mission.getTitle(), mission.getSubmitType(),
                mission.getRewardMin(), mission.getRewardMax(), mission.getMinTextLength(),
                true, mission.getCreatedByType(), owner.getId());
    }

    /** 보낸 필드만 반영한다. 기존 값을 읽어 병합한 뒤 통째로 덮어쓴다. */
    @Transactional
    public MissionResponses.MissionUpdated updateMission(Long userId, Long missionId,
                                                         MissionUpdateRequest request) {
        Mission mission = requireChallengeMission(missionId);
        memberAccess.requireOwner(userId, mission.getChallengeId());

        if (request.title() != null) {
            mission.setTitle(request.title());
        }
        if (request.rewardMin() != null) {
            mission.setRewardMin(request.rewardMin());
        }
        if (request.rewardMax() != null) {
            mission.setRewardMax(request.rewardMax());
        }
        if (request.minTextLength() != null) {
            mission.setMinTextLength(request.minTextLength());
        }
        if (request.isActive() != null) {
            mission.setIsActive(request.isActive() ? "Y" : "N");
        }
        validateRewardRange(mission.getRewardMin(), mission.getRewardMax());

        missionMapper.update(mission);
        return new MissionResponses.MissionUpdated(
                mission.getId(), mission.getTitle(), mission.getRewardMin(),
                mission.getRewardMax(), mission.getMinTextLength(), toBoolean(mission.getIsActive()));
    }

    /** FR-009. 저장하지 않고 후보만 돌려준다 — 방장이 골라 미션 생성 API 로 등록한다. */
    public MissionResponses.AiSuggestions generateSuggestions(Long userId, Long challengeId,
                                                              AiMissionGenerateRequest request) {
        memberAccess.requireOwner(userId, challengeId);
        return new MissionResponses.AiSuggestions(suggestionGenerator.generate(request.theme()));
    }


    /**
     * FR-047 미션 삭제(화면 25 "위험 구역"). 방장 전용.
     *
     * <p><b>한 번이라도 배정된 미션은 지울 수 없다.</b> 그 배정에 미션 로그와 거래가 매달려 있어,
     * 지우면 팀의 과거 기록이 통째로 깨진다. 이 경우는 409 를 돌려주고 비활성화를 안내한다 —
     * 비활성화하면 오늘의 미션 뽑기에서 빠지므로 사용자가 원하는 결과는 대부분 그쪽으로 얻어진다.
     *
     * <p>아직 아무도 뽑지 않은 미션(잘못 만든 것)만 실제로 지운다. 명세가 "완전 삭제 미제공"이었던
     * 것을 이 조건부 삭제로 대체한다.
     */
    @Transactional
    public void deleteMission(Long userId, Long missionId) {
        Mission mission = requireChallengeMission(missionId);
        memberAccess.requireOwner(userId, mission.getChallengeId());

        if (assignmentMapper.countByMissionId(missionId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 배정된 적이 있는 미션은 삭제할 수 없습니다. 비활성화하면 더 이상 뽑히지 않습니다.");
        }
        missionMapper.deleteById(missionId);
    }

    // ------------------------------------------------------------------ 오늘의 미션

    /**
     * FR-010 오늘의 미션(화면 10). 아직 안 뽑혔으면 이 호출 시점에 배정을 만든다.
     *
     * <p>배정은 챌린지당 하루 1건이고 보상 금액도 이때 한 번만 뽑는다. 팀원 여럿이 동시에 열어도
     * UNIQUE 제약이 한 건만 남기므로, 충돌하면 이미 만들어진 배정을 다시 읽는다.
     */
    @Transactional
    public MissionResponses.Today today(Long userId, Long challengeId) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        LocalDate today = LocalDate.now(clock);

        if (assignmentMapper.findByChallengeIdAndAssignedDate(challengeId, today) == null) {
            if (missionMapper.findActiveByChallengeId(challengeId).isEmpty()) {
                // 방장이 미션을 전부 비활성화한 상태. 오류가 아니라 "오늘 뽑을 게 없음"이다.
                return MissionResponses.Today.none();
            }
            try {
                assignmentMapper.insert(drawAssignment(challengeId, today));
            } catch (DuplicateKeyException e) {
                // 다른 팀원이 먼저 뽑았다. 그 결과를 그대로 쓴다.
            }
        }

        var result = assignmentMapper.findTodayMission(challengeId, today, me.getId());
        if (result == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "오늘의 미션 배정에 실패했습니다.");
        }
        return new MissionResponses.Today(
                true,
                result.getAssignmentId(), result.getMissionId(), result.getTitle(),
                result.getSubmitType(), result.getRewardMin(), result.getRewardMax(),
                result.getMinTextLength(), result.getAssignedDate(), result.getMySubmissionStatus());
    }

    /**
     * 미션 제출(화면 11). 오늘 배정된 미션에만 제출할 수 있다.
     *
     * <p>텍스트 미션이 최소 글자수를 채우면 이 자리에서 바로 완료 처리하고 보상을 지급한다.
     * 사진 미션은 AI 판별이 필요해 SUBMITTED 로 남는다 — 판별 연동은 아직 없다.
     */
    @Transactional
    public MissionResponses.Submitted submit(Long userId, Long missionId, MissionSubmitRequest request) {
        Mission mission = requireChallengeMission(missionId);
        Member me = memberAccess.requireActiveMember(userId, mission.getChallengeId());
        requireNotAchieved(me, mission.getChallengeId());
        LocalDate today = LocalDate.now(clock);

        DailyMissionAssignment assignment =
                assignmentMapper.findByChallengeIdAndAssignedDate(mission.getChallengeId(), today);
        if (assignment == null || !assignment.getMissionId().equals(missionId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "오늘 배정된 미션이 아닙니다.");
        }
        if (missionLogMapper.findByMemberIdAndAssignmentId(me.getId(), assignment.getId()) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "오늘 미션은 이미 제출했습니다.");
        }
        validateSubmission(mission, request.submitContent(), request.attachmentUrl());

        MissionLog log = new MissionLog();
        log.setMemberId(me.getId());
        log.setAssignmentId(assignment.getId());
        log.setSubmitContent(request.submitContent());
        log.setAttachmentUrl(request.attachmentUrl());
        log.setIsPublic(request.isPublic() == null || request.isPublic() ? "Y" : "N");
        log.setStatus(SUBMITTED);
        missionLogMapper.insert(log);

        if (autoApprovable(mission, request.submitContent())) {
            missionLogMapper.updateStatus(log.getId(), AI_APPROVED, REVIEWER_AI, null);
            missionLogMapper.updateRewardAmount(log.getId(), assignment.getRewardAmount());
            rewardService.pay(me, log.getId(), assignment.getRewardAmount(),
                    RewardService.TYPE_REWARD, null);
            return new MissionResponses.Submitted(log.getId(), AI_APPROVED);
        }
        return new MissionResponses.Submitted(log.getId(), SUBMITTED);
    }

    // ------------------------------------------------------------------ 조회

    /**
     * 캘린더(화면 16)와 팀 활동 피드(FR-039). 비공개 기록은 미리보기를 지운다.
     *
     * <p>커서 방식. 다음 페이지 존재 여부를 알려고 1건 더 읽고 잘라낸다.
     */
    public MissionResponses.MissionLogList feed(Long userId, Long challengeId, LocalDate date,
                                                Long memberId, Long cursor, Integer size) {
        memberAccess.requireActiveMember(userId, challengeId);
        int limit = Paging.size(size);

        List<MissionLogFeedItem> rows =
                missionLogMapper.findFeedByChallengeId(challengeId, date, memberId, cursor, limit + 1);
        boolean hasNext = rows.size() > limit;
        List<MissionLogFeedItem> page = hasNext ? rows.subList(0, limit) : rows;
        Long nextCursor = hasNext ? page.get(page.size() - 1).getMissionLogId() : null;

        return new MissionResponses.MissionLogList(
                page.stream()
                        .map(item -> new MissionResponses.MissionLogList.Item(
                                item.getMissionLogId(), item.getAssignmentId(), item.getMissionTitle(),
                                item.getAssignedDate(), item.getMemberId(), item.getNickname(),
                                item.getStatus(), item.getReviewedBy(), item.getRewardAmount(),
                                toBoolean(item.getIsPublic()),
                                toBoolean(item.getIsPublic()) ? item.getSubmitContentPreview() : null))
                        .toList(),
                nextCursor);
    }

    /** 미션 상세(화면 12·14). 비공개 기록은 작성자 본인에게만 본문과 첨부를 보여준다. */
    public MissionResponses.MissionLogDetailResponse detail(Long userId, Long missionLogId) {
        MissionLogDetail detail = missionLogMapper.findDetailById(missionLogId);
        if (detail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "미션 기록을 찾을 수 없습니다.");
        }
        Member me = memberAccess.requireActiveMember(userId, detail.getChallengeId());

        boolean isPublic = toBoolean(detail.getIsPublic());
        boolean isAuthor = me.getId().equals(detail.getMemberId());
        boolean showContent = isPublic || isAuthor;

        return new MissionResponses.MissionLogDetailResponse(
                detail.getMissionLogId(), detail.getAssignmentId(), detail.getMissionTitle(),
                detail.getChallengeId(), detail.getMemberId(), detail.getNickname(),
                showContent ? detail.getSubmitContent() : null,
                showContent ? detail.getAttachmentUrl() : null,
                isPublic, detail.getStatus(), detail.getReviewedBy(),
                detail.getRejectReasonCode(), detail.getRewardAmount(), detail.getCreatedAt());
    }

    // ------------------------------------------------------------------ FR-012 이의제기 흐름

    /** 오탐 이의제기(화면 12). AI 가 반려한 건에만 걸 수 있다. */
    @Transactional
    public MissionResponses.StatusChanged dispute(Long userId, Long missionLogId, DisputeRequest request) {
        MissionLog log = requireOwnLog(userId, missionLogId);
        if (!AI_REJECTED.equals(log.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "AI 반려 상태에서만 이의제기할 수 있습니다.");
        }
        // 사유는 방장이 재판정 화면에서 읽는 값이라 기록 자체를 바꾸지 않고 알림으로 전달한다.
        missionLogMapper.updateStatus(missionLogId, DISPUTE_REQUESTED, log.getReviewedBy(),
                log.getRejectReasonCode());
        // 여기서부터 방장 판정을 기다린다. 무응답 자동 승인(FR-012c)의 기준 시각이다.
        missionLogMapper.touchReviewRequestedAt(missionLogId);
        notifyOwners(log, "이의제기가 접수됐습니다: " + request.reason(), missionLogId);
        return new MissionResponses.StatusChanged(missionLogId, DISPUTE_REQUESTED);
    }

    /** 그대로 재검토 요청. 제출 내용은 건드리지 않고 대기열로만 보낸다. */
    @Transactional
    public MissionResponses.StatusChanged recheck(Long userId, Long missionLogId) {
        MissionLog log = requireOwnLog(userId, missionLogId);
        requireDisputeRequested(log);
        missionLogMapper.updateStatus(missionLogId, RECHECK_REQUESTED, log.getReviewedBy(),
                log.getRejectReasonCode());
        // 대기 시작 시각을 다시 찍는다 — 마지막 요청 기준으로 기다려야 공평하다.
        missionLogMapper.touchReviewRequestedAt(missionLogId);
        return new MissionResponses.StatusChanged(missionLogId, RECHECK_REQUESTED);
    }

    /** 수정 후 재제출(FR-012b). 재제출 횟수를 함께 올린다. */
    @Transactional
    public MissionResponses.StatusChanged resubmit(Long userId, Long missionLogId, ResubmitRequest request) {
        MissionLog log = requireOwnLog(userId, missionLogId);
        requireDisputeRequested(log);

        String content = request.submitContent() != null ? request.submitContent() : log.getSubmitContent();
        String attachment = request.attachmentUrl() != null ? request.attachmentUrl() : log.getAttachmentUrl();
        if (!StringUtils.hasText(content) && !StringUtils.hasText(attachment)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "재제출할 내용이 없습니다.");
        }

        missionLogMapper.updateSubmission(missionLogId, content, attachment);
        missionLogMapper.increaseResubmitCount(missionLogId);
        missionLogMapper.updateStatus(missionLogId, RESUBMITTED, null, null);
        // 대기 시작 시각을 다시 찍는다 — 마지막 요청 기준으로 기다려야 공평하다.
        missionLogMapper.touchReviewRequestedAt(missionLogId);
        return new MissionResponses.StatusChanged(missionLogId, RESUBMITTED);
    }

    /**
     * 방장 재판정(FR-012c·d). 승인하면 보상을 지급하고, 이미 지급된 건을 반려하면 회수한다.
     *
     * <p>지급 여부는 마지막 거래가 아니라 순액으로 판단한다 — 지급→회수→재지급이 같은 기록에 쌓인다.
     */
    @Transactional
    public MissionResponses.Reviewed review(Long userId, Long missionLogId, ReviewRequest request) {
        MissionLogDetail detail = missionLogMapper.findDetailById(missionLogId);
        if (detail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "미션 기록을 찾을 수 없습니다.");
        }
        memberAccess.requireOwner(userId, detail.getChallengeId());

        MissionLog log = missionLogMapper.findById(missionLogId);
        if (!REVIEWABLE.contains(log.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "재판정할 수 있는 상태가 아닙니다.");
        }

        Member author = memberAccess.requireMemberById(detail.getMemberId());

        if (Boolean.TRUE.equals(request.approved())) {
            missionLogMapper.updateStatus(missionLogId, OWNER_APPROVED, REVIEWER_OWNER, null);

            if (rewardService.netPaidAmount(missionLogId) > 0) {
                // 이미 받은 상태에서 승인만 확정된 경우라 추가 지급은 없다.
                return new MissionResponses.Reviewed(
                        missionLogId, OWNER_APPROVED, REVIEWER_OWNER, null, null);
            }
            int amount = requireAssignmentReward(log.getAssignmentId());
            missionLogMapper.updateRewardAmount(missionLogId, amount);
            Transaction previous = rewardService.lastPayment(missionLogId);
            String type = previous == null ? RewardService.TYPE_REWARD : RewardService.TYPE_RE_PAYMENT;
            Long txId = rewardService.pay(author, missionLogId, amount, type,
                    previous == null ? null : previous.getId());
            return new MissionResponses.Reviewed(missionLogId, OWNER_APPROVED, REVIEWER_OWNER, txId, null);
        }

        if (!StringUtils.hasText(request.rejectReasonCode())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "반려할 때는 사유 코드가 필요합니다.");
        }
        missionLogMapper.updateStatus(missionLogId, OWNER_REJECTED, REVIEWER_OWNER,
                request.rejectReasonCode());

        Long reversalId = null;
        if (rewardService.netPaidAmount(missionLogId) > 0) {
            reversalId = rewardService.reverse(author, missionLogId, rewardService.lastPayment(missionLogId));
        }
        notifyAuthor(detail, "미션이 반려됐습니다. 이의제기를 할 수 있어요.", missionLogId);
        return new MissionResponses.Reviewed(
                missionLogId, OWNER_REJECTED, REVIEWER_OWNER, null, reversalId);
    }

    /**
     * FR-012c 방장 무응답 자동 승인.
     *
     * <p>요구사항정의서 v0.4 §4 가 "방장이 없거나 응답이 없을 때의 예외 처리"로 남겨둔 자리다.
     * 이의제기한 기록이 방장 판정을 기다리다 그대로 묻히면 작성자는 정당한 보상을 영영 못 받는다.
     *
     * <p>두 경우에만 열린다.
     * <ul>
     *   <li>방장 판정을 기다린 지 {@code app.review.owner-response-hours} 가 지났을 때</li>
     *   <li>챌린지에 ACTIVE 방장이 아예 없을 때(카카오 연동 해제 등으로 방장 계정이 닫힌 경우)</li>
     * </ul>
     *
     * <p>호출은 <b>작성자 본인만</b> 할 수 있다. 남이 부를 수 있으면 남의 기록을 마음대로 확정하는
     * 길이 열리는 데다, 승인은 어차피 작성자에게 유리한 처분이라 대신 눌러줄 이유도 없다.
     *
     * <p>판정 주체는 {@code AUTO} 로 남긴다 — 방장이 승인한 것처럼 기록되면 나중에 이력이 사실과 달라진다.
     */
    @Transactional
    public MissionResponses.Reviewed autoApprove(Long userId, Long missionLogId) {
        MissionLogDetail detail = missionLogMapper.findDetailById(missionLogId);
        if (detail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "미션 기록을 찾을 수 없습니다.");
        }
        Member me = memberAccess.requireActiveMember(userId, detail.getChallengeId());
        if (!me.getId().equals(detail.getMemberId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인 기록만 자동 승인을 요청할 수 있습니다.");
        }

        MissionLog log = missionLogMapper.findById(missionLogId);
        if (!REVIEWABLE.contains(log.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "방장 판정을 기다리는 상태가 아닙니다.");
        }
        requireOwnerUnresponsive(log, detail.getChallengeId());

        missionLogMapper.updateStatus(missionLogId, OWNER_APPROVED, REVIEWER_AUTO, null);
        if (rewardService.netPaidAmount(missionLogId) > 0) {
            // 이미 받은 상태에서 승인만 확정된 경우라 추가 지급은 없다.
            return new MissionResponses.Reviewed(missionLogId, OWNER_APPROVED, REVIEWER_AUTO, null, null);
        }

        int amount = requireAssignmentReward(log.getAssignmentId());
        missionLogMapper.updateRewardAmount(missionLogId, amount);
        Transaction previous = rewardService.lastPayment(missionLogId);
        String type = previous == null ? RewardService.TYPE_REWARD : RewardService.TYPE_RE_PAYMENT;
        Member author = memberAccess.requireMemberById(detail.getMemberId());
        Long txId = rewardService.pay(author, missionLogId, amount, type,
                previous == null ? null : previous.getId());
        return new MissionResponses.Reviewed(missionLogId, OWNER_APPROVED, REVIEWER_AUTO, txId, null);
    }

    /**
     * 자동 승인 조건. 기한이 남아 있고 방장도 살아 있으면 409 로 막는다.
     *
     * <p>대기 시작 시각이 비어 있는 기록은 이 기능이 생기기 전에 쌓인 것이라 제출 시각을 기준으로 본다.
     */
    private void requireOwnerUnresponsive(MissionLog log, Long challengeId) {
        boolean ownerAbsent = memberAccess.activeMembersOf(challengeId).stream()
                .noneMatch(member -> "OWNER".equals(member.getRole()));
        if (ownerAbsent) {
            return;
        }

        LocalDateTime waitingSince = log.getReviewRequestedAt() != null
                ? log.getReviewRequestedAt()
                : log.getCreatedAt();
        if (LocalDateTime.now(clock).isBefore(waitingSince.plusHours(reviewProperties.ownerResponseHours()))) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "아직 방장 판정 기한이 남았습니다. " + reviewProperties.ownerResponseHours()
                            + "시간이 지난 뒤에 다시 시도해주세요.");
        }
    }

    // ------------------------------------------------------------------ 내부

    /** 배정할 미션을 뽑고 보상 금액도 이때 한 번 확정한다. */
    private DailyMissionAssignment drawAssignment(Long challengeId, LocalDate date) {
        List<Mission> pool = missionMapper.findActiveByChallengeId(challengeId);
        if (pool.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "등록된 미션이 없습니다. 방장이 미션을 먼저 등록해야 합니다.");
        }
        Mission picked = pool.get(random.nextInt(pool.size()));
        if (picked.getRewardMin() == null || picked.getRewardMax() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "미션에 보상 범위가 설정돼 있지 않습니다.");
        }

        DailyMissionAssignment assignment = new DailyMissionAssignment();
        assignment.setChallengeId(challengeId);
        assignment.setMissionId(picked.getId());
        assignment.setAssignedDate(date);
        assignment.setRewardAmount(drawReward(picked.getRewardMin(), picked.getRewardMax()));
        return assignment;
    }

    /** FR-013 균등 랜덤. 상·하한을 모두 포함한다. */
    private int drawReward(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /**
     * FR-046. 목표를 채운 사람은 미션을 더 수행하지 않는다.
     *
     * <p>화면에서 버튼만 숨기면 우회할 수 있어 서버가 막는다(프론트 §12 요청). 다만 <b>리액션·댓글은
     * 그대로 열어 둔다</b> — 다 채운 사람이 아직 채우는 중인 팀원을 응원하라는 것이 이 규칙의 의도다.
     *
     * <p>오늘의 미션 조회(배정)는 막지 않는다. 배정은 챌린지 공용이라 달성자가 조회했다는 이유로
     * 막으면 팀 전체의 뽑기가 어그러진다.
     */
    private void requireNotAchieved(Member me, Long challengeId) {
        Challenge challenge = challengeMapper.findById(challengeId);
        if (challenge == null || challenge.getTargetAmount() == null) {
            return;
        }
        Account account = accountMapper.findByMemberId(me.getId());
        long balance = account == null || account.getBalance() == null ? 0L : account.getBalance();

        if (goalService.achievedByMember(me.getId(), challenge.getTargetAmount(), balance)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "목표 금액을 달성해 미션 수행이 끝났습니다. 팀원 응원은 계속할 수 있어요.");
        }
    }

    private boolean autoApprovable(Mission mission, String submitContent) {
        if (!"TEXT".equals(mission.getSubmitType())) {
            return false;
        }
        int required = mission.getMinTextLength() == null ? 0 : mission.getMinTextLength();
        return submitContent != null && submitContent.strip().length() >= required;
    }

    private void validateSubmission(Mission mission, String submitContent, String attachmentUrl) {
        if ("TEXT".equals(mission.getSubmitType()) && !StringUtils.hasText(submitContent)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "텍스트 미션은 작성 내용이 필요합니다.");
        }
        if ("PHOTO".equals(mission.getSubmitType()) && !StringUtils.hasText(attachmentUrl)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "사진 미션은 첨부가 필요합니다.");
        }
    }

    private void validateRewardRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "보상 하한이 상한보다 클 수 없습니다.");
        }
    }

    private Mission requireChallengeMission(Long missionId) {
        Mission mission = missionMapper.findById(missionId);
        if (mission == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "미션을 찾을 수 없습니다.");
        }
        if (mission.getChallengeId() == null) {
            // 전역 고정 풀 미션은 특정 챌린지에 속하지 않아 권한을 판단할 수 없다.
            throw new BusinessException(HttpStatus.BAD_REQUEST, "챌린지에 속한 미션이 아닙니다.");
        }
        return mission;
    }

    private MissionLog requireOwnLog(Long userId, Long missionLogId) {
        MissionLogDetail detail = missionLogMapper.findDetailById(missionLogId);
        if (detail == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "미션 기록을 찾을 수 없습니다.");
        }
        Member me = memberAccess.requireActiveMember(userId, detail.getChallengeId());
        if (!me.getId().equals(detail.getMemberId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인이 작성한 기록만 처리할 수 있습니다.");
        }
        return missionLogMapper.findById(missionLogId);
    }

    private void requireDisputeRequested(MissionLog log) {
        if (!DISPUTE_REQUESTED.equals(log.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이의제기 상태에서만 진행할 수 있습니다.");
        }
    }

    private int requireAssignmentReward(Long assignmentId) {
        DailyMissionAssignment assignment = assignmentMapper.findById(assignmentId);
        if (assignment == null || assignment.getRewardAmount() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "배정된 보상 금액을 찾을 수 없습니다.");
        }
        return assignment.getRewardAmount();
    }

    /**
     * FR-012a 오탐 알림은 작성자 본인에게만 간다.
     *
     * <p>본인이 이 종류를 껐으면 보내지 않는다. 반려 사실은 미션 상세에서 항상 확인할 수 있다.
     */
    private void notifyAuthor(MissionLogDetail detail, String content, Long missionLogId) {
        if (!notificationPolicy.enabledFor(detail.getMemberId(), NotificationType.MISSION_REJECTED)) {
            return;
        }

        Notification noti = new Notification();
        noti.setReceiverMemberId(detail.getMemberId());
        noti.setNotiType(NotificationType.MISSION_REJECTED.name());
        noti.setContent(content);
        noti.setTargetType("MISSION_LOG");
        noti.setTargetId(missionLogId);
        noti.setIsRead("N");
        notificationMapper.insert(noti);
    }

    /** 재판정할 사람에게 알린다. */
    private void notifyOwners(MissionLog log, String content, Long missionLogId) {
        MissionLogDetail detail = missionLogMapper.findDetailById(missionLogId);
        Set<Long> optedOut = notificationPolicy.disabledMemberIds(
                detail.getChallengeId(), NotificationType.MISSION_DISPUTED);

        for (Member member : memberAccess.activeMembersOf(detail.getChallengeId())) {
            if (!"OWNER".equals(member.getRole()) || optedOut.contains(member.getId())) {
                continue;
            }
            Notification noti = new Notification();
            noti.setReceiverMemberId(member.getId());
            noti.setNotiType(NotificationType.MISSION_DISPUTED.name());
            noti.setContent(content);
            noti.setTargetType("MISSION_LOG");
            noti.setTargetId(missionLogId);
            noti.setIsRead("N");
            notificationMapper.insert(noti);
        }
    }

    private static Boolean toBoolean(String yn) {
        return yn == null ? null : "Y".equals(yn);
    }
}
