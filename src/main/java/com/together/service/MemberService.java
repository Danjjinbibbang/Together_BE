package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.Member;
import com.together.dto.MemberBalance;
import com.together.dto.request.MemberUpdateRequest;
import com.together.dto.response.MemberResponses;
import com.together.mapper.MemberMapper;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-003 ~ FR-006, FR-038, FR-041. 챌린지 참여자 정보.
 */
@Service
public class MemberService {

    private final MemberMapper memberMapper;
    private final MemberAccessService memberAccess;
    private final StreakService streakService;
    private final NotificationPolicy notificationPolicy;

    public MemberService(MemberMapper memberMapper,
                         MemberAccessService memberAccess,
                         StreakService streakService,
                         NotificationPolicy notificationPolicy) {
        this.memberMapper = memberMapper;
        this.memberAccess = memberAccess;
        this.streakService = streakService;
        this.notificationPolicy = notificationPolicy;
    }

    /**
     * 멤버 목록. 탈퇴자도 함께 내려준다 — 과거 활동 기록에 닉네임을 붙여야 하기 때문이다.
     * 화면에서 걸러 표시할지는 프론트가 정한다.
     */
    public MemberResponses.MemberList list(Long userId, Long challengeId) {
        memberAccess.requireActiveMember(userId, challengeId);
        List<MemberBalance> members = memberMapper.findWithBalanceByChallengeId(challengeId);
        Map<Long, Integer> streaks = streakService.byMember(challengeId);
        return new MemberResponses.MemberList(members.stream()
                .map(m -> new MemberResponses.MemberList.Item(
                        m.getMemberId(), m.getNickname(), m.getRole(), m.getStatus(),
                        m.getBalance(), streaks.getOrDefault(m.getMemberId(), 0)))
                .toList());
    }

    public MemberResponses.Me me(Long userId, Long challengeId) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        return new MemberResponses.Me(
                me.getId(), me.getUserId(), me.getChallengeId(), me.getNickname(),
                me.getRole(), me.getStatus(), me.getThemeColor(),
                notificationPolicy.settingsOf(me.getId()));
    }

    /**
     * 보낸 필드만 바꾼다. 닉네임·테마색·알림설정은 성격이 달라 각각 별도 UPDATE 로 처리한다.
     *
     * <p>세 가지 모두 챌린지별 개인 설정이라 한 엔드포인트에 모았다 — 팀원에게는 보이지 않는다.
     */
    @Transactional
    public MemberResponses.Updated updateMe(Long userId, Long challengeId, MemberUpdateRequest request) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);

        if (request.nickname() != null) {
            try {
                memberMapper.updateNickname(me.getId(), request.nickname());
            } catch (DuplicateKeyException e) {
                throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
            }
        }
        if (request.themeColor() != null) {
            memberMapper.updateThemeColor(me.getId(), request.themeColor());
        }
        if (request.notificationSettings() != null) {
            notificationPolicy.update(me.getId(), request.notificationSettings());
        }

        Member updated = memberMapper.findById(me.getId());
        return new MemberResponses.Updated(
                updated.getId(), updated.getNickname(), updated.getThemeColor(),
                notificationPolicy.settingsOf(me.getId()));
    }

    /**
     * FR-038 탈퇴. 행을 지우지 않고 상태만 바꿔 과거 활동 기록을 남긴다.
     *
     * <p>방장은 먼저 양도해야 한다 — 방장 없는 챌린지가 되면 미션 관리와 재판정이 막히기 때문이다.
     */
    @Transactional
    public void leave(Long userId, Long challengeId) {
        Member me = memberAccess.requireActiveMember(userId, challengeId);
        if ("OWNER".equals(me.getRole())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "방장은 다른 멤버에게 양도한 뒤 탈퇴할 수 있습니다.");
        }
        memberMapper.updateStatus(me.getId(), "LEFT");
    }
}
