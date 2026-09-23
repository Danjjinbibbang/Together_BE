package com.together.mapper;

import com.together.dto.User;
import org.apache.ibatis.annotations.Param;

/**
 * USERS 테이블 접근. {@code resources/mapper/UserMapper.xml} 과 1:1 대응한다.
 */
public interface UserMapper {

    User findById(@Param("id") Long id);

    /** 카카오 회원번호로 기존 가입자를 찾는다. 없으면 {@code null}. */
    User findByKakaoId(@Param("kakaoId") String kakaoId);

    /** 신규 가입자를 저장한다. 채번된 ID 는 {@code user.id} 에 채워진다. */
    int insert(User user);

    /**
     * FR-043 계정 탈퇴. 행을 지우지 않고 탈퇴 시각만 남기되 개인정보는 지운다.
     *
     * <p>KAKAO_ID 는 UNIQUE 라 NULL 로 비우는 대신 {@code withdrawn:<id>} 로 바꾼다 —
     * 같은 카카오 계정으로 다시 가입할 수 있어야 하기 때문이다. 이미 탈퇴한 계정이면 0 을 돌려준다.
     */
    int withdraw(@Param("id") Long id);

    /** FR-028 약관 동의 처리. 동의 시각은 DB 시계를 쓴다. */
    int updateTermsAgreement(@Param("id") Long id, @Param("termsVersion") String termsVersion);
}
