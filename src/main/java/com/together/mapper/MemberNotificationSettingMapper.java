package com.together.mapper;

import com.together.dto.MemberNotificationSetting;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MEMBER_NOTIFICATION_SETTING 테이블 접근.
 * {@code resources/mapper/MemberNotificationSettingMapper.xml} 과 1:1 대응한다.
 *
 * <p>저장된 행은 "끈 것"뿐일 수도 있다. 행이 없으면 켜짐이라는 해석은 서비스가 한다.
 */
public interface MemberNotificationSettingMapper {

    List<MemberNotificationSetting> findByMemberId(@Param("memberId") Long memberId);

    /** 한 종류의 설정값. 저장된 적 없으면 {@code null}. */
    String findEnabled(@Param("memberId") Long memberId, @Param("notiType") String notiType);

    /**
     * 이 챌린지에서 해당 종류를 꺼둔 멤버들.
     *
     * <p>팀 전원에게 알림을 뿌릴 때 멤버마다 조회하지 않도록 한 번에 가져온다.
     */
    List<Long> findDisabledMemberIds(
            @Param("challengeId") Long challengeId, @Param("notiType") String notiType);

    /** 있으면 갱신, 없으면 삽입. 설정 화면이 같은 값을 여러 번 보내도 안전하다. */
    int upsert(@Param("memberId") Long memberId,
               @Param("notiType") String notiType,
               @Param("enabled") String enabled);
}
