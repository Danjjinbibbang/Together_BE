package com.together.service;

import com.together.dto.MemberNotificationSetting;
import com.together.dto.NotificationType;
import com.together.mapper.MemberNotificationSettingMapper;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "이 사람이 이 종류의 알림을 받을지"를 판단하는 자리.
 *
 * <p>알림을 만드는 곳이 셋(입금·반려·이의제기)이라 각자 판단하게 두면 규칙이 갈라진다.
 * 저장된 행이 없으면 켜짐이라는 해석도 여기 한 곳에만 둔다.
 */
@Service
public class NotificationPolicy {

    private static final String DISABLED = "N";

    private final MemberNotificationSettingMapper settingMapper;

    public NotificationPolicy(MemberNotificationSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    /** 저장된 값이 없으면 켜짐으로 본다 — 기본은 수신이다. */
    public boolean enabledFor(Long memberId, NotificationType type) {
        return !DISABLED.equals(settingMapper.findEnabled(memberId, type.name()));
    }

    /**
     * 이 챌린지에서 해당 종류를 꺼둔 멤버 집합.
     *
     * <p>팀 전원에게 뿌릴 때 멤버마다 조회하면 N 번 왕복하므로 한 번에 받아 걸러 쓴다.
     */
    public Set<Long> disabledMemberIds(Long challengeId, NotificationType type) {
        return new HashSet<>(settingMapper.findDisabledMemberIds(challengeId, type.name()));
    }

    /**
     * 설정 화면에 내려줄 전체 상태. 저장된 적 없는 종류도 켜짐으로 채워서 모든 종류를 담는다 —
     * 프론트가 종류 목록을 따로 알 필요가 없게 하려는 것이다.
     */
    public Map<NotificationType, Boolean> settingsOf(Long memberId) {
        Map<NotificationType, Boolean> result = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            result.put(type, true);
        }
        for (MemberNotificationSetting saved : settingMapper.findByMemberId(memberId)) {
            NotificationType type = parse(saved.getNotiType());
            // 더 이상 쓰지 않는 종류가 남아 있을 수 있어 모르는 값은 무시한다
            if (type != null) {
                result.put(type, !DISABLED.equals(saved.getEnabled()));
            }
        }
        return result;
    }

    /** 보낸 종류만 반영한다. 나머지는 건드리지 않는다. */
    @Transactional
    public void update(Long memberId, Map<NotificationType, Boolean> changes) {
        changes.forEach((type, enabled) ->
                settingMapper.upsert(memberId, type.name(), enabled ? "Y" : DISABLED));
    }

    private static NotificationType parse(String raw) {
        try {
            return NotificationType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
