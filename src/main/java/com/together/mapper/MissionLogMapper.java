package com.together.mapper;

import com.together.dto.CalendarEntry;
import com.together.dto.MemberActivityDate;
import com.together.dto.MissionLog;
import com.together.dto.MissionLogDetail;
import com.together.dto.MissionLogFeedItem;
import com.together.dto.MyMissionLogItem;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MISSION_LOG 테이블 접근. {@code resources/mapper/MissionLogMapper.xml} 과 1:1 대응한다.
 */
public interface MissionLogMapper {

    MissionLog findById(@Param("id") Long id);

    /**
     * {@code GET /mission-logs/{missionLogId}} 용. 미션 제목·작성자 닉네임·챌린지를 조인한다.
     */
    MissionLogDetail findDetailById(@Param("id") Long id);

    /** 한 멤버가 특정 배정건에 남긴 기록(UK_MISSION_LOG_MEMBER_ASSIGNMENT). 미제출이면 {@code null}. */
    MissionLog findByMemberIdAndAssignmentId(
            @Param("memberId") Long memberId, @Param("assignmentId") Long assignmentId);

    /** 한 사람의 수행 기록을 최신순으로(IX_MISSION_LOG_MEMBER). */
    List<MissionLog> findByMemberId(@Param("memberId") Long memberId);

    /** 한 배정건에 대한 팀원 전원의 제출 기록. */
    List<MissionLog> findByAssignmentId(@Param("assignmentId") Long assignmentId);

    /** 상태별 조회(IX_MISSION_LOG_STATUS). 방장의 재판정 대기 목록 등에 쓴다. */
    List<MissionLog> findByStatus(@Param("status") String status);

    /**
     * {@code GET /challenges/{id}/mission-logs} 용. 캘린더(화면 16)와 팀 활동 피드(FR-039)가 쓴다.
     *
     * <p>{@code assignedDate} 와 {@code memberId} 는 선택 필터다. null 을 넘기면 그 조건은 적용되지
     * 않는다 — 동적 SQL 분기 대신 {@code (#{param} IS NULL OR ...)} 로 표현했다.
     *
     * <p>커서 방식이다. 피드는 새 제출이 계속 앞에 끼어들어 오프셋으로는 중복·누락이 생긴다.
     *
     * @param cursor 이 ID 보다 작은 것만. 첫 페이지는 {@code null}
     * @param limit  다음 페이지 존재 여부를 알려면 서비스가 필요한 개수보다 1 크게 넘긴다
     */
    List<MissionLogFeedItem> findFeedByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("assignedDate") LocalDate assignedDate,
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    int insert(MissionLog missionLog);

    /** 수정 후 재제출(PATCH .../dispute/resubmit). 본문·첨부를 덮어쓴다. */
    int updateSubmission(
            @Param("id") Long id,
            @Param("submitContent") String submitContent,
            @Param("attachmentUrl") String attachmentUrl);

    /**
     * FR-012 8단계 상태 전이를 기록한다. 어느 상태로 보낼지, 판정 주체가 누구인지,
     * 반려 사유가 무엇인지는 서비스가 판단해서 넘긴다.
     */
    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("reviewedBy") String reviewedBy,
            @Param("rejectReasonCode") String rejectReasonCode);

    /** 지급이 확정될 때 실제 지급액을 기록한다. */
    int updateRewardAmount(@Param("id") Long id, @Param("rewardAmount") Integer rewardAmount);

    /**
     * FR-012c 방장 판정 대기 시작 시각을 지금(DB 시계)으로 찍는다.
     * 이의제기·재검토요청·재제출로 대기열에 들어갈 때마다 부른다.
     */
    int touchReviewRequestedAt(@Param("id") Long id);

    /** FR-012b 재제출 횟수를 1 올린다. */
    int increaseResubmitCount(@Param("id") Long id);

    /**
     * {@code GET /me/calendar} 용(FR-027). 참여 중인 모든 챌린지를 가로질러 하루 × 챌린지로 집계한다.
     *
     * <p>금액은 MISSION_LOG.REWARD_AMOUNT 가 아니라 TRANSACTIONS 의 순액이다. 오탐으로 회수된
     * 기록은 지급액이 남아 있어서, 그대로 더하면 캘린더 합계가 계좌 잔액과 어긋난다.
     */
    List<CalendarEntry> findCalendarEntriesByUserId(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** {@code GET /me/mission-logs?date=} 용(FR-040). 하루치라 페이지네이션이 없다. */
    List<MyMissionLogItem> findMyLogsByUserIdAndDate(
            @Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * FR-025② 전체 활동 연속일용. 챌린지를 가리지 않고 "제출한 날"의 distinct 목록을 최근순으로.
     *
     * <p>성공/반려를 따지지 않는다 — 요구사항이 "하루라도 활동했으면 인정"이다.
     * 연속 판정은 서비스(StreakCalculator)가 한다.
     */
    List<LocalDate> findActivityDatesByUserId(
            @Param("userId") Long userId, @Param("limit") int limit);

    /**
     * FR-025① 챌린지별 스트릭용. 그 챌린지 멤버 전원의 "성공한 날"을 최근순으로 돌려준다.
     *
     * @param statuses 성공으로 칠 상태. 무엇이 성공인지는 정책이라 서비스가 넘긴다
     */
    List<MemberActivityDate> findSuccessDatesByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("statuses") List<String> statuses,
            @Param("limit") int limit);
}
