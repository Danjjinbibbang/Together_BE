package com.together.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.together.dto.Account;
import com.together.dto.CalendarEntry;
import com.together.dto.Challenge;
import com.together.dto.ChallengeSummary;
import com.together.dto.Comment;
import com.together.dto.CommentView;
import com.together.dto.DailyMissionAssignment;
import com.together.dto.Member;
import com.together.dto.MemberActivityDate;
import com.together.dto.MemberBalance;
import com.together.dto.Mission;
import com.together.dto.MissionLog;
import com.together.dto.MissionLogDetail;
import com.together.dto.MissionLogFeedItem;
import com.together.dto.MissionModeVote;
import com.together.dto.MyMissionLogItem;
import com.together.dto.Notification;
import com.together.dto.Reaction;
import com.together.dto.ReactionCount;
import com.together.dto.RefreshToken;
import com.together.dto.TeamMessage;
import com.together.dto.TeamMessageView;
import com.together.dto.TodayMission;
import com.together.dto.Transaction;
import com.together.dto.User;
import com.together.dto.VoteChoiceCount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * 전체 매퍼의 모든 statement 가 실제 Oracle(v0.4 + API 명세서 정렬 스키마)에서 실행되는지 확인한다.
 *
 * <p>단위 검증이 아니라 배선 검증이다 — XML 이 인터페이스에 바인딩되는지, 컬럼명·별칭·SQL 문법이
 * 실제 스키마와 맞는지를 본다. 조인 쿼리는 별칭이 DTO 프로퍼티로 제대로 매핑되는지까지 본다.
 * {@code @MybatisTest} 는 트랜잭션이라 끝나면 전부 롤백된다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MapperSmokeTest {

    @Autowired private UserMapper userMapper;
    @Autowired private ChallengeMapper challengeMapper;
    @Autowired private MemberMapper memberMapper;
    @Autowired private MemberNotificationSettingMapper settingMapper;
    @Autowired private AccountMapper accountMapper;
    @Autowired private MissionMapper missionMapper;
    @Autowired private DailyMissionAssignmentMapper assignmentMapper;
    @Autowired private MissionLogMapper missionLogMapper;
    @Autowired private TransactionMapper transactionMapper;
    @Autowired private NotificationMapper notificationMapper;
    @Autowired private ReactionMapper reactionMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private MissionModeVoteMapper voteMapper;
    @Autowired private MissionModeVoteBallotMapper ballotMapper;
    @Autowired private RefreshTokenMapper refreshTokenMapper;
    @Autowired private TeamMessageMapper teamMessageMapper;

    @Test
    @DisplayName("16개 테이블의 전체 statement 가 스키마와 맞는다")
    void allMapperStatementsRunAgainstRealSchema() {
        // --- USERS ---------------------------------------------------------
        User user = new User();
        user.setKakaoId("smoke-kakao-1");
        userMapper.insert(user);
        assertThat(user.getId()).isNotNull();

        // --- CHALLENGE -----------------------------------------------------
        Challenge challenge = new Challenge();
        challenge.setTitle("스모크 챌린지");
        challenge.setTargetAmount(300_000L);
        challenge.setStartDate(LocalDate.of(2026, 9, 1));
        challenge.setEndDate(LocalDate.of(2026, 12, 31));
        challenge.setInviteCode("SMOKE-0001");
        // 생성 시점에는 미션 방식을 정하지 않는다(응답이 null)
        challengeMapper.insert(challenge);

        assertThat(challengeMapper.findById(challenge.getId()).getMissionMode()).isNull();
        assertThat(challengeMapper.findByInviteCode("SMOKE-0001")).isNotNull();

        challenge.setTitle("이름 바꾼 챌린지");
        assertThat(challengeMapper.update(challenge)).isEqualTo(1);
        assertThat(challengeMapper.updateMissionMode(challenge.getId(), "FIXED")).isEqualTo(1);

        // --- MEMBER --------------------------------------------------------
        Member member = new Member();
        member.setUserId(user.getId());
        member.setChallengeId(challenge.getId());
        member.setNickname("짠돌이철수");
        member.setRole("OWNER");
        member.setStatus("ACTIVE");
        member.setThemeColor("#18A8F1");
        memberMapper.insert(member);

        assertThat(memberMapper.findById(member.getId()).getRole()).isEqualTo("OWNER");
        assertThat(memberMapper.findByUserIdAndChallengeId(user.getId(), challenge.getId()))
                .isNotNull();
        assertThat(memberMapper.findByChallengeId(challenge.getId())).hasSize(1);

        assertThat(memberMapper.updateNickname(member.getId(), "새닉네임")).isEqualTo(1);
        assertThat(memberMapper.updateThemeColor(member.getId(), "#FF7A59")).isEqualTo(1);

        // --- 알림 종류별 수신 설정 ------------------------------------------
        // 저장된 행이 없으면 켜짐으로 본다
        assertThat(settingMapper.findEnabled(member.getId(), "MISSION_REWARD")).isNull();
        assertThat(settingMapper.findDisabledMemberIds(challenge.getId(), "MISSION_REWARD")).isEmpty();

        // upsert 는 없으면 삽입
        assertThat(settingMapper.upsert(member.getId(), "MISSION_REWARD", "N")).isEqualTo(1);
        assertThat(settingMapper.findEnabled(member.getId(), "MISSION_REWARD")).isEqualTo("N");
        assertThat(settingMapper.findDisabledMemberIds(challenge.getId(), "MISSION_REWARD"))
                .containsExactly(member.getId());
        // 다른 종류는 영향받지 않는다
        assertThat(settingMapper.findEnabled(member.getId(), "MISSION_REJECTED")).isNull();

        // 같은 키로 다시 부르면 갱신(중복 삽입 아님)
        assertThat(settingMapper.upsert(member.getId(), "MISSION_REWARD", "Y")).isEqualTo(1);
        assertThat(settingMapper.findEnabled(member.getId(), "MISSION_REWARD")).isEqualTo("Y");
        assertThat(settingMapper.findByMemberId(member.getId())).hasSize(1);
        assertThat(settingMapper.findDisabledMemberIds(challenge.getId(), "MISSION_REWARD")).isEmpty();
        assertThat(memberMapper.updateRole(member.getId(), "MEMBER")).isEqualTo(1);
        assertThat(memberMapper.updateStatus(member.getId(), "LEFT")).isEqualTo(1);
        assertThat(memberMapper.findById(member.getId()).getStatus()).isEqualTo("LEFT");
        // 이후 조회들이 ACTIVE 를 전제로 하므로 되돌린다
        memberMapper.updateStatus(member.getId(), "ACTIVE");
        memberMapper.updateRole(member.getId(), "OWNER");

        // --- ACCOUNT -------------------------------------------------------
        Account account = new Account();
        account.setMemberId(member.getId());
        account.setBalance(0L);
        accountMapper.insert(account);

        assertThat(accountMapper.addBalance(account.getId(), 3_000L)).isEqualTo(1);
        assertThat(accountMapper.findById(account.getId()).getBalance()).isEqualTo(3_000L);
        assertThat(accountMapper.findByMemberId(member.getId())).isNotNull();
        assertThat(accountMapper.sumBalanceByChallengeId(challenge.getId())).isEqualTo(3_000L);

        // 멤버 + 잔액 조인 (GET /members, GET /accounts 공용)
        List<MemberBalance> balances = memberMapper.findWithBalanceByChallengeId(challenge.getId());
        assertThat(balances).hasSize(1);
        assertThat(balances.get(0).getNickname()).isEqualTo("새닉네임");
        assertThat(balances.get(0).getBalance()).isEqualTo(3_000L);

        // 챌린지 목록 집계 (GET /challenges)
        List<ChallengeSummary> summaries = challengeMapper.findSummariesByUserId(user.getId());
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getGoalAmount()).isEqualTo(300_000L);
        assertThat(summaries.get(0).getCurrentBalance()).isEqualTo(3_000L);
        assertThat(summaries.get(0).getMemberCount()).isEqualTo(1);
        assertThat(summaries.get(0).getThemeColor()).isEqualTo("#FF7A59");

        // --- MISSION -------------------------------------------------------
        Mission mission = new Mission();
        mission.setChallengeId(challenge.getId());
        mission.setTitle("오늘 감사한 점 3가지 적기");
        mission.setSubmitType("TEXT");
        mission.setRewardMin(100);
        mission.setRewardMax(2_000);
        mission.setMinTextLength(30);
        mission.setCreatedByType("OWNER");
        mission.setCreatedByMemberId(member.getId());
        mission.setIsActive("Y");
        missionMapper.insert(mission);

        assertThat(missionMapper.findById(mission.getId()).getCreatedByType()).isEqualTo("OWNER");
        assertThat(missionMapper.findActiveByChallengeId(challenge.getId())).hasSize(1);
        assertThat(missionMapper.findAllByChallengeId(challenge.getId(), 0, 20)).hasSize(1);
        assertThat(missionMapper.countByChallengeId(challenge.getId())).isEqualTo(1);
        assertThat(missionMapper.findAllByChallengeId(challenge.getId(), 1, 20)).isEmpty();
        assertThat(missionMapper.findActiveFixedPool()).isNotEmpty();

        // --- DAILY_MISSION_ASSIGNMENT --------------------------------------
        DailyMissionAssignment assignment = new DailyMissionAssignment();
        assignment.setChallengeId(challenge.getId());
        assignment.setMissionId(mission.getId());
        assignment.setAssignedDate(LocalDate.of(2026, 9, 15));
        // 배정 시점에 범위에서 뽑아 고정한다. 실제 추첨은 서비스가 한다.
        assignment.setRewardAmount(1_400);
        assignmentMapper.insert(assignment);

        assertThat(assignmentMapper.findById(assignment.getId()).getRewardAmount()).isEqualTo(1_400);
        assertThat(assignmentMapper.findByChallengeIdAndAssignedDate(
                        challenge.getId(), LocalDate.of(2026, 9, 15)))
                .isNotNull();

        // 아직 제출 전이므로 mySubmissionStatus 는 null
        TodayMission today = assignmentMapper.findTodayMission(
                challenge.getId(), LocalDate.of(2026, 9, 15), member.getId());
        assertThat(today).isNotNull();
        assertThat(today.getTitle()).isEqualTo("오늘 감사한 점 3가지 적기");
        assertThat(today.getMinTextLength()).isEqualTo(30);
        assertThat(today.getMySubmissionStatus()).isNull();
        // 범위만 노출된다. 확정 금액은 이 응답에 담기지 않는다.
        assertThat(today.getRewardMin()).isEqualTo(100);
        assertThat(today.getRewardMax()).isEqualTo(2_000);

        // --- MISSION_LOG ---------------------------------------------------
        MissionLog log = new MissionLog();
        log.setMemberId(member.getId());
        log.setAssignmentId(assignment.getId());
        log.setSubmitContent("오늘은 아침에 날씨가 맑아서 기분이 좋았습니다.");
        log.setIsPublic("Y");
        log.setStatus("SUBMITTED");
        missionLogMapper.insert(log);

        assertThat(missionLogMapper.findById(log.getId())).isNotNull();
        assertThat(missionLogMapper.findByMemberIdAndAssignmentId(member.getId(), assignment.getId()))
                .isNotNull();
        assertThat(missionLogMapper.findByMemberId(member.getId())).hasSize(1);
        assertThat(missionLogMapper.findByAssignmentId(assignment.getId())).hasSize(1);

        // --- 미션 삭제 가능 여부 (FR-047) ------------------------------------
        // 이 미션은 이미 배정됐으므로 삭제 대상이 아니다 — 서비스가 이 카운트로 막는다
        assertThat(assignmentMapper.countByMissionId(mission.getId())).isEqualTo(1);

        Mission unusedMission = new Mission();
        unusedMission.setChallengeId(challenge.getId());
        unusedMission.setTitle("아직 아무도 뽑지 않은 미션");
        unusedMission.setSubmitType("TEXT");
        unusedMission.setRewardMin(100);
        unusedMission.setRewardMax(500);
        unusedMission.setMinTextLength(10);
        unusedMission.setCreatedByType("AI");
        unusedMission.setCreatedByMemberId(member.getId());
        unusedMission.setIsActive("Y");
        missionMapper.insert(unusedMission);

        assertThat(assignmentMapper.countByMissionId(unusedMission.getId())).isZero();
        assertThat(missionMapper.deleteById(unusedMission.getId())).isEqualTo(1);
        assertThat(missionMapper.findById(unusedMission.getId())).isNull();

        // 제출 후에는 오늘의 미션 조회에 내 상태가 실린다
        assertThat(assignmentMapper.findTodayMission(
                        challenge.getId(), LocalDate.of(2026, 9, 15), member.getId())
                        .getMySubmissionStatus())
                .isEqualTo("SUBMITTED");

        // FR-012 8단계: AI 반려 → 이의제기 → 재제출 → 방장 승인
        assertThat(missionLogMapper.updateStatus(log.getId(), "AI_REJECTED", "AI", "LOW_LIGHT"))
                .isEqualTo(1);
        assertThat(missionLogMapper.findByStatus("AI_REJECTED")).isNotEmpty();
        missionLogMapper.updateStatus(log.getId(), "DISPUTE_REQUESTED", "AI", "LOW_LIGHT");
        assertThat(missionLogMapper.updateSubmission(log.getId(), "수정된 텍스트 내용", null))
                .isEqualTo(1);
        missionLogMapper.updateStatus(log.getId(), "RESUBMITTED", null, null);
        assertThat(missionLogMapper.increaseResubmitCount(log.getId())).isEqualTo(1);
        missionLogMapper.updateStatus(log.getId(), "OWNER_APPROVED", "OWNER", null);
        // 완료 처리 시 배정에서 확정된 금액을 복사해 넣는다
        assertThat(missionLogMapper.updateRewardAmount(log.getId(), assignment.getRewardAmount()))
                .isEqualTo(1);

        MissionLog reloaded = missionLogMapper.findById(log.getId());
        assertThat(reloaded.getStatus()).isEqualTo("OWNER_APPROVED");
        assertThat(reloaded.getReviewedBy()).isEqualTo("OWNER");
        assertThat(reloaded.getResubmitCount()).isEqualTo(1);
        assertThat(reloaded.getSubmitContent()).isEqualTo("수정된 텍스트 내용");

        // 상세 조인 (GET /mission-logs/{id})
        MissionLogDetail detail = missionLogMapper.findDetailById(log.getId());
        assertThat(detail.getMissionTitle()).isEqualTo("오늘 감사한 점 3가지 적기");
        assertThat(detail.getChallengeId()).isEqualTo(challenge.getId());
        assertThat(detail.getNickname()).isEqualTo("새닉네임");

        // 피드 조회 (GET /challenges/{id}/mission-logs) — 선택 필터 조합을 모두 태운다
        assertThat(missionLogMapper.findFeedByChallengeId(challenge.getId(), null, null, null, 20)).hasSize(1);
        assertThat(missionLogMapper.findFeedByChallengeId(
                        challenge.getId(), LocalDate.of(2026, 9, 15), null, null, 20))
                .hasSize(1);
        assertThat(missionLogMapper.findFeedByChallengeId(challenge.getId(), null, member.getId(), null, 20))
                .hasSize(1);
        assertThat(missionLogMapper.findFeedByChallengeId(
                        challenge.getId(), LocalDate.of(2026, 1, 1), null, null, 20))
                .isEmpty();

        List<MissionLogFeedItem> feed =
                missionLogMapper.findFeedByChallengeId(challenge.getId(), null, null, null, 20);
        assertThat(feed.get(0).getSubmitContentPreview()).isEqualTo("수정된 텍스트 내용");
        assertThat(feed.get(0).getAssignedDate()).isEqualTo(LocalDate.of(2026, 9, 15));

        // 커서를 이 기록의 ID 로 주면 그보다 오래된 것만 남으므로 비어야 한다
        assertThat(missionLogMapper.findFeedByChallengeId(
                        challenge.getId(), null, null, log.getId(), 20))
                .isEmpty();

        // --- TRANSACTIONS --------------------------------------------------
        Transaction reward = new Transaction();
        reward.setAccountId(account.getId());
        reward.setMissionLogId(log.getId());
        reward.setAmount(1_000L);
        reward.setTxType("MISSION_REWARD");
        transactionMapper.insert(reward);

        // 취소 거래는 원거래를 가리킨다
        Transaction reversal = new Transaction();
        reversal.setAccountId(account.getId());
        reversal.setMissionLogId(log.getId());
        reversal.setOriginalTransactionId(reward.getId());
        reversal.setAmount(-1_000L);
        reversal.setTxType("REVERSAL");
        transactionMapper.insert(reversal);

        assertThat(transactionMapper.findById(reversal.getId()).getOriginalTransactionId())
                .isEqualTo(reward.getId());
        assertThat(transactionMapper.findByAccountId(account.getId(), 0, 20)).hasSize(2);
        assertThat(transactionMapper.countByAccountId(account.getId())).isEqualTo(2);
        // 오프셋 페이지네이션: 1건씩 끊으면 페이지마다 다른 거래가 나와야 한다
        List<Transaction> firstPage = transactionMapper.findByAccountId(account.getId(), 0, 1);
        List<Transaction> secondPage = transactionMapper.findByAccountId(account.getId(), 1, 1);
        assertThat(firstPage).hasSize(1);
        assertThat(secondPage).hasSize(1);
        assertThat(firstPage.get(0).getId()).isNotEqualTo(secondPage.get(0).getId());
        assertThat(transactionMapper.findByAccountId(account.getId(), 2, 1)).isEmpty();
        assertThat(transactionMapper.findByMissionLogId(log.getId())).hasSize(2);

        // --- NOTIFICATION --------------------------------------------------
        Notification noti = new Notification();
        noti.setReceiverMemberId(member.getId());
        noti.setNotiType("MISSION_REWARD");
        noti.setContent("새닉네임님이 오늘 미션을 완료하고 1,000원을 모았어요");
        noti.setTargetType("MISSION_LOG");
        noti.setTargetId(log.getId());
        noti.setIsRead("N");
        notificationMapper.insert(noti);

        assertThat(notificationMapper.findById(noti.getId()).getTargetType())
                .isEqualTo("MISSION_LOG");
        assertThat(notificationMapper.findByReceiverMemberId(member.getId())).hasSize(1);
        // 알림함과 배지는 챌린지가 아니라 사용자 전역 기준이다
        assertThat(notificationMapper.findByUserId(user.getId(), null, 20)).hasSize(1);
        // 커서를 이 알림의 ID 로 주면 더 오래된 게 없어 비어야 한다
        assertThat(notificationMapper.findByUserId(user.getId(), noti.getId(), 20)).isEmpty();
        assertThat(notificationMapper.countUnreadByUserId(user.getId())).isEqualTo(1);
        assertThat(notificationMapper.markAsRead(noti.getId())).isEqualTo(1);
        assertThat(notificationMapper.countUnreadByUserId(user.getId())).isZero();

        // --- REACTION ------------------------------------------------------
        Reaction reaction = new Reaction();
        reaction.setMissionLogId(log.getId());
        reaction.setSenderMemberId(member.getId());
        reaction.setReactionType("CLAP");
        reactionMapper.insert(reaction);

        assertThat(reactionMapper.findByMissionLogIdAndSenderMemberId(log.getId(), member.getId())
                        .getReactionType())
                .isEqualTo("CLAP");
        assertThat(reactionMapper.findByMissionLogId(log.getId())).hasSize(1);

        List<ReactionCount> counts = reactionMapper.countByMissionLogId(log.getId());
        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).getReactionType()).isEqualTo("CLAP");
        assertThat(counts.get(0).getCount()).isEqualTo(1);

        assertThat(reactionMapper.deleteById(reaction.getId())).isEqualTo(1);
        assertThat(reactionMapper.findByMissionLogId(log.getId())).isEmpty();

        // --- COMMENTS ------------------------------------------------------
        Comment comment = new Comment();
        comment.setMissionLogId(log.getId());
        comment.setSenderMemberId(member.getId());
        comment.setContent("오늘도 화이팅! 😊");
        commentMapper.insert(comment);

        assertThat(commentMapper.findById(comment.getId()).getContent()).isEqualTo("오늘도 화이팅! 😊");

        List<CommentView> views = commentMapper.findViewsByMissionLogId(log.getId());
        assertThat(views).hasSize(1);
        assertThat(views.get(0).getNickname()).isEqualTo("새닉네임");
        assertThat(views.get(0).getMemberId()).isEqualTo(member.getId());

        assertThat(commentMapper.deleteById(comment.getId())).isEqualTo(1);

        // --- 내 기록 캘린더 (GET /me/calendar, GET /me/mission-logs) ------------
        // 이 시점의 기록은 OWNER_APPROVED 이고 거래는 지급(+1,000) → 회수(-1,000) 라 순액이 0 이다.
        // 캘린더는 거래 순액을, 하루 목록은 기록의 지급액을 보므로 둘이 다른 것이 정상이다.
        List<CalendarEntry> calendar = missionLogMapper.findCalendarEntriesByUserId(
                user.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        assertThat(calendar).hasSize(1);
        assertThat(calendar.get(0).getActivityDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(calendar.get(0).getChallengeId()).isEqualTo(challenge.getId());
        assertThat(calendar.get(0).getChallengeTitle()).isEqualTo("이름 바꾼 챌린지");
        assertThat(calendar.get(0).getThemeColor()).isEqualTo("#FF7A59");
        assertThat(calendar.get(0).getMissionCount()).isEqualTo(1);
        assertThat(calendar.get(0).getRewardAmount()).isZero();

        // 범위 밖은 비어야 한다
        assertThat(missionLogMapper.findCalendarEntriesByUserId(
                        user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isEmpty();

        List<MyMissionLogItem> myLogs =
                missionLogMapper.findMyLogsByUserIdAndDate(user.getId(), LocalDate.of(2026, 9, 15));
        assertThat(myLogs).hasSize(1);
        assertThat(myLogs.get(0).getMissionLogId()).isEqualTo(log.getId());
        assertThat(myLogs.get(0).getChallengeTitle()).isEqualTo("이름 바꾼 챌린지");
        assertThat(myLogs.get(0).getMissionTitle()).isEqualTo("오늘 감사한 점 3가지 적기");
        assertThat(myLogs.get(0).getStatus()).isEqualTo("OWNER_APPROVED");
        assertThat(myLogs.get(0).getRewardAmount()).isEqualTo(1_400);
        assertThat(myLogs.get(0).getIsPublic()).isEqualTo("Y");
        assertThat(missionLogMapper.findMyLogsByUserIdAndDate(user.getId(), LocalDate.of(2026, 9, 16)))
                .isEmpty();

        // --- 스트릭 원자료 (FR-025) -----------------------------------------
        // 연속일 자체는 StreakCalculator 가 센다. 여기서는 날짜 목록이 제대로 나오는지만 본다.
        assertThat(missionLogMapper.findActivityDatesByUserId(user.getId(), 400))
                .containsExactly(LocalDate.of(2026, 9, 15));

        List<MemberActivityDate> successDates = missionLogMapper.findSuccessDatesByChallengeId(
                challenge.getId(), List.of("AI_APPROVED", "OWNER_APPROVED"), 2_000);
        assertThat(successDates).hasSize(1);
        assertThat(successDates.get(0).getMemberId()).isEqualTo(member.getId());
        assertThat(successDates.get(0).getActivityDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        // 성공으로 치지 않는 상태만 넘기면 비어야 한다
        assertThat(missionLogMapper.findSuccessDatesByChallengeId(
                        challenge.getId(), List.of("AI_REJECTED"), 2_000))
                .isEmpty();

        // --- 계정 탈퇴 (DELETE /users/me) ---------------------------------
        assertThat(memberMapper.findByUserId(user.getId())).hasSize(1);
        assertThat(memberMapper.updateStatusByUserId(user.getId(), "LEFT")).isEqualTo(1);
        assertThat(memberMapper.findById(member.getId()).getStatus()).isEqualTo("LEFT");
        // 이미 LEFT 면 건드리지 않는다
        assertThat(memberMapper.updateStatusByUserId(user.getId(), "LEFT")).isZero();

        assertThat(userMapper.withdraw(user.getId())).isEqualTo(1);
        User withdrawn = userMapper.findById(user.getId());
        assertThat(withdrawn.getWithdrawnAt()).isNotNull();
        assertThat(withdrawn.getEmail()).isNull();
        // 카카오 회원번호를 치환해 둔 덕에 같은 계정으로 재가입할 수 있다
        assertThat(withdrawn.getKakaoId()).isEqualTo("withdrawn:" + user.getId());
        assertThat(userMapper.findByKakaoId("smoke-kakao-1")).isNull();
        // 두 번째 호출은 갱신 건수 0 — 서비스가 이걸 409 로 바꿔 돌려준다
        assertThat(userMapper.withdraw(user.getId())).isZero();

        // --- 미션 방식 팀원 투표 (FR-008) -----------------------------------
        MissionModeVote vote = new MissionModeVote();
        vote.setChallengeId(challenge.getId());
        vote.setDeadline(LocalDateTime.of(2026, 9, 20, 18, 0));
        vote.setOpenedByMemberId(member.getId());
        voteMapper.insert(vote);
        assertThat(vote.getId()).isNotNull();

        assertThat(voteMapper.findById(vote.getId()).getStatus()).isEqualTo("OPEN");
        assertThat(voteMapper.findOpenByChallengeId(challenge.getId())).isNotNull();
        assertThat(voteMapper.findLatestByChallengeId(challenge.getId()).getId()).isEqualTo(vote.getId());

        // 아직 아무도 안 던졌다
        assertThat(ballotMapper.find(vote.getId(), member.getId())).isNull();
        assertThat(ballotMapper.countByChoice(vote.getId())).isEmpty();

        // 던지고 → 마음을 바꿔도 행이 늘지 않는다(MERGE)
        assertThat(ballotMapper.upsert(vote.getId(), member.getId(), "FIXED")).isEqualTo(1);
        assertThat(ballotMapper.find(vote.getId(), member.getId()).getChoice()).isEqualTo("FIXED");
        assertThat(ballotMapper.upsert(vote.getId(), member.getId(), "AI")).isEqualTo(1);
        assertThat(ballotMapper.find(vote.getId(), member.getId()).getChoice()).isEqualTo("AI");

        List<VoteChoiceCount> tally = ballotMapper.countByChoice(vote.getId());
        assertThat(tally).hasSize(1);
        assertThat(tally.get(0).getChoice()).isEqualTo("AI");
        assertThat(tally.get(0).getCount()).isEqualTo(1);

        // 닫는다. OPEN 인 행만 닫히므로 두 번째 호출은 0 이다(동시 마감 방지)
        LocalDateTime closedAt = LocalDateTime.of(2026, 9, 20, 18, 0, 1);
        assertThat(voteMapper.close(vote.getId(), "DECIDED", "AI", closedAt)).isEqualTo(1);
        assertThat(voteMapper.close(vote.getId(), "DECIDED", "AI", closedAt)).isZero();

        MissionModeVote closed = voteMapper.findById(vote.getId());
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getOutcome()).isEqualTo("DECIDED");
        assertThat(closed.getResultMode()).isEqualTo("AI");
        assertThat(voteMapper.findOpenByChallengeId(challenge.getId())).isNull();

        // --- 방장 무응답 자동 승인의 기준 시각 (FR-012c) ----------------------
        assertThat(missionLogMapper.findById(log.getId()).getReviewRequestedAt()).isNull();
        assertThat(missionLogMapper.touchReviewRequestedAt(log.getId())).isEqualTo(1);
        assertThat(missionLogMapper.findById(log.getId()).getReviewRequestedAt()).isNotNull();

        // --- 리프레시 토큰 (FR-035) ------------------------------------------
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        token.setExpiresAt(LocalDateTime.of(2026, 10, 15, 0, 0));
        refreshTokenMapper.insert(token);
        assertThat(token.getId()).isNotNull();

        RefreshToken found = refreshTokenMapper.findByTokenHash(token.getTokenHash());
        assertThat(found.getUserId()).isEqualTo(user.getId());
        // 폐기 전에는 살아 있다 — 서비스가 "없는 토큰"과 "폐기된 토큰"을 구분해야 하기 때문이다
        assertThat(found.getRevokedAt()).isNull();

        LocalDateTime revokedAt = LocalDateTime.of(2026, 9, 21, 9, 0);
        assertThat(refreshTokenMapper.revokeById(token.getId(), revokedAt)).isEqualTo(1);
        // 이미 폐기된 행은 다시 건드리지 않는다(폐기 시각이 덮어써지면 이력이 흐려진다)
        assertThat(refreshTokenMapper.revokeById(token.getId(), revokedAt)).isZero();
        assertThat(refreshTokenMapper.findByTokenHash(token.getTokenHash()).getRevokedAt()).isNotNull();

        // 계정 탈퇴·재사용 감지에서 쓰는 일괄 폐기. 이미 다 내려간 뒤라 0 이다
        assertThat(refreshTokenMapper.revokeAllByUserId(user.getId(), revokedAt)).isZero();

        // --- 팀 응원 메시지 (FR-044) -----------------------------------------
        TeamMessage cheer = new TeamMessage();
        cheer.setChallengeId(challenge.getId());
        cheer.setSenderMemberId(member.getId());
        cheer.setContent("이번 주도 다들 화이팅! 🔥");
        teamMessageMapper.insert(cheer);
        assertThat(cheer.getId()).isNotNull();

        assertThat(teamMessageMapper.findById(cheer.getId()).getContent())
                .isEqualTo("이번 주도 다들 화이팅! 🔥");

        // 목록은 발신자 닉네임을 조인해서 준다
        List<TeamMessageView> cheers =
                teamMessageMapper.findViewsByChallengeId(challenge.getId(), null, 20);
        assertThat(cheers).hasSize(1);
        assertThat(cheers.get(0).getMessageId()).isEqualTo(cheer.getId());
        assertThat(cheers.get(0).getNickname()).isEqualTo("새닉네임");

        // 커서를 이 메시지 ID 로 주면 그보다 오래된 것만 남으므로 비어야 한다
        assertThat(teamMessageMapper.findViewsByChallengeId(challenge.getId(), cheer.getId(), 20))
                .isEmpty();

        // 도배 방지 카운트 — 오늘 보낸 건 세어지고, 내일 이후로 자르면 0 이다
        assertThat(teamMessageMapper.countBySenderSince(member.getId(), LocalDate.now().atStartOfDay()))
                .isEqualTo(1);
        assertThat(teamMessageMapper.countBySenderSince(
                        member.getId(), LocalDate.now().plusDays(1).atStartOfDay()))
                .isZero();

        // 수정 — 원본이 바뀌고 UPDATED_AT 이 찍힌다
        assertThat(teamMessageMapper.updateContent(cheer.getId(), "역시 우리 팀!")).isEqualTo(1);
        TeamMessage editedCheer = teamMessageMapper.findById(cheer.getId());
        assertThat(editedCheer.getContent()).isEqualTo("역시 우리 팀!");
        assertThat(editedCheer.getUpdatedAt()).isNotNull();

        // 팀원 알림함 동기화 — 대상으로 되짚어 본문을 한 번에 바꾼다
        Notification cheerNoti = new Notification();
        cheerNoti.setReceiverMemberId(member.getId());
        cheerNoti.setNotiType("TEAM_CHEER");
        cheerNoti.setContent("새닉네임님의 응원: 이번 주도 다들 화이팅! 🔥");
        cheerNoti.setTargetType("TEAM_MESSAGE");
        cheerNoti.setTargetId(cheer.getId());
        cheerNoti.setIsRead("Y");
        notificationMapper.insert(cheerNoti);

        assertThat(notificationMapper.updateContentByTarget(
                        "TEAM_MESSAGE", cheer.getId(), "새닉네임님의 응원: 역시 우리 팀!"))
                .isEqualTo(1);
        Notification synced = notificationMapper.findById(cheerNoti.getId());
        assertThat(synced.getContent()).isEqualTo("새닉네임님의 응원: 역시 우리 팀!");
        // 읽음 상태는 건드리지 않는다 — 수정했다고 뱃지를 다시 켜지 않는다
        assertThat(synced.getIsRead()).isEqualTo("Y");

        // 삭제 — 원본은 soft delete, 알림은 흔적 없이 사라진다
        assertThat(teamMessageMapper.softDelete(cheer.getId())).isEqualTo(1);
        assertThat(teamMessageMapper.softDelete(cheer.getId())).isZero();
        assertThat(teamMessageMapper.findById(cheer.getId()).getDeletedAt()).isNotNull();
        assertThat(teamMessageMapper.findViewsByChallengeId(challenge.getId(), null, 20)).isEmpty();

        assertThat(notificationMapper.deleteByTarget("TEAM_MESSAGE", cheer.getId())).isEqualTo(1);
        assertThat(notificationMapper.findById(cheerNoti.getId())).isNull();

        // 지운 뒤에도 하루 한도 카운트에는 남는다 — 지우고 다시 보내는 우회를 막는다
        assertThat(teamMessageMapper.countBySenderSince(member.getId(), LocalDate.now().atStartOfDay()))
                .isEqualTo(1);
    }
}
