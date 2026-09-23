package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.dto.Member;
import com.together.dto.User;
import com.together.mapper.MemberMapper;
import com.together.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-043 계정 탈퇴(화면 27 마이페이지)와 카카오 연동 해제 뒷정리.
 *
 * <p>챌린지 탈퇴(FR-038)와는 다른 개념이다. 그쪽은 챌린지 하나에서만 나가는 것이고, 이쪽은
 * 참여 중인 모든 챌린지에서 나가면서 계정 자체를 닫는다.
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final MemberMapper memberMapper;
    private final RefreshTokenService refreshTokenService;

    public UserService(UserMapper userMapper,
                       MemberMapper memberMapper,
                       RefreshTokenService refreshTokenService) {
        this.userMapper = userMapper;
        this.memberMapper = memberMapper;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 사용자가 직접 하는 계정 탈퇴.
     *
     * <p>방장으로 남아 있는 챌린지가 있으면 409 다. 방장 없는 챌린지가 되면 미션 관리와 재판정이
     * 막히기 때문인데, 챌린지 탈퇴(FR-038)에 이미 같은 규칙이 있어 그쪽과 맞췄다.
     * 다만 혼자 남은 챌린지는 넘겨줄 상대가 없으므로 막지 않는다.
     */
    @Transactional
    public void withdraw(Long userId) {
        requireActiveAccount(userId);
        requireNoOwnedChallengeWithOthers(userId);
        close(userId);
    }

    /**
     * 카카오에서 연결이 끊긴 뒤의 뒷정리(v0.4 §4 "카카오 연동 해제 처리").
     *
     * <p>같은 soft delete 지만 <b>방장 검사를 하지 않는다.</b> 이미 카카오 쪽에서 끊긴 상태라 거절해도
     * 되돌릴 수 없고, 로그인 수단이 사라진 계정을 열어두면 방장 자리만 비어 있는 채로 남는다.
     * 방장이 사라진 챌린지의 재판정은 FR-012c 자동 승인이 받아준다.
     */
    @Transactional
    public void withdrawAfterKakaoUnlink(Long userId) {
        close(userId);
    }

    /**
     * 행은 지우지 않고 개인정보만 지운다 — 과거 기록이 MEMBER 를 참조하고 있어 지우면 팀의 미션
     * 기록·거래·댓글이 함께 깨진다. 세션도 함께 끊어 남은 리프레시 토큰으로 되살아나지 않게 한다.
     */
    private void close(Long userId) {
        memberMapper.updateStatusByUserId(userId, "LEFT");
        refreshTokenService.revokeAll(userId);
        if (userMapper.withdraw(userId) == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 탈퇴한 계정입니다.");
        }
    }

    private void requireActiveAccount(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다.");
        }
    }

    private void requireNoOwnedChallengeWithOthers(Long userId) {
        for (Member membership : memberMapper.findByUserId(userId)) {
            if (!"ACTIVE".equals(membership.getStatus()) || !"OWNER".equals(membership.getRole())) {
                continue;
            }
            boolean someoneElseRemains = memberMapper.findByChallengeId(membership.getChallengeId())
                    .stream()
                    .anyMatch(other -> !other.getId().equals(membership.getId())
                            && "ACTIVE".equals(other.getStatus()));
            if (someoneElseRemains) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "방장으로 있는 챌린지가 있습니다. 먼저 방장을 양도한 뒤 탈퇴할 수 있습니다.");
            }
        }
    }
}
