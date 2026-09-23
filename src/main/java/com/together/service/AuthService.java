package com.together.service;

import com.together.common.exception.BusinessException;
import com.together.common.kakao.KakaoClient;
import com.together.common.kakao.KakaoUserResponse;
import com.together.common.security.JwtTokenProvider;
import com.together.config.KakaoProperties;
import com.together.dto.User;
import com.together.dto.request.KakaoLoginRequest;
import com.together.dto.request.KakaoUnlinkCallbackRequest;
import com.together.dto.request.LogoutRequest;
import com.together.dto.request.TermsAgreementRequest;
import com.together.dto.request.TokenRefreshRequest;
import com.together.dto.response.AuthResponses;
import com.together.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * FR-028 ~ FR-032, FR-035. 카카오 로그인, 약관 동의, 토큰 갱신·로그아웃, 카카오 연동 해제 수신.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** 카카오가 콜백에 붙여 보내는 인증 헤더 형식. */
    private static final String ADMIN_KEY_PREFIX = "KakaoAK ";

    private final KakaoClient kakaoClient;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final KakaoProperties kakaoProperties;

    public AuthService(KakaoClient kakaoClient,
                       JwtTokenProvider tokenProvider,
                       UserMapper userMapper,
                       RefreshTokenService refreshTokenService,
                       UserService userService,
                       KakaoProperties kakaoProperties) {
        this.kakaoClient = kakaoClient;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.kakaoProperties = kakaoProperties;
    }

    /**
     * 인가코드로 로그인한다. 처음 보는 카카오 계정이면 가입까지 함께 처리한다.
     *
     * <p>닉네임을 여기서 받지 않는 이유는 닉네임이 전역이 아니라 챌린지별 값이기 때문이다(FR-003 개정).
     */
    @Transactional
    public AuthResponses.KakaoLogin loginWithKakao(KakaoLoginRequest request) {
        String kakaoAccessToken = kakaoClient.exchangeAccessToken(request.authorizationCode());
        KakaoUserResponse kakaoUser = kakaoClient.fetchUser(kakaoAccessToken);
        String kakaoId = String.valueOf(kakaoUser.id());

        User user = userMapper.findByKakaoId(kakaoId);
        boolean isNewUser = user == null;
        if (isNewUser) {
            user = new User();
            user.setKakaoId(kakaoId);
            user.setEmail(kakaoUser.email());
            userMapper.insert(user);
            // 생성 시각은 DB 기본값이라 되읽어야 응답에 담을 수 있다.
            user = userMapper.findById(user.getId());
        }

        String accessToken = tokenProvider.createToken(user.getId());
        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthResponses.KakaoLogin(
                accessToken,
                refreshToken,
                tokenProvider.expirationSeconds(),
                isNewUser,
                new AuthResponses.UserSummary(user.getId(), user.getKakaoId(), user.getCreatedAt()));
    }

    /**
     * FR-035 액세스 토큰 갱신. 인증 헤더가 필요 없다 — 액세스 토큰이 이미 만료된 상태에서 부르는 API 다.
     *
     * <p>회전 방식이라 리프레시 토큰도 새로 나간다. 프론트는 둘 다 갈아끼워야 한다.
     */
    @Transactional
    public AuthResponses.TokenPair refresh(TokenRefreshRequest request) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(request.refreshToken());
        return new AuthResponses.TokenPair(
                tokenProvider.createToken(rotated.userId()),
                rotated.refreshToken(),
                tokenProvider.expirationSeconds());
    }

    /**
     * FR-035 로그아웃. 리프레시 토큰만 폐기한다.
     *
     * <p>이미 발급된 액세스 토큰은 stateless 라 회수할 수 없고 만료까지 유효하다. 프론트가 저장소에서
     * 지우는 것이 실질적인 로그아웃이고, 이 API 는 "갱신은 더 안 된다"를 서버에 남기는 역할이다.
     */
    @Transactional
    public void logout(Long userId, LogoutRequest request) {
        refreshTokenService.revoke(userId, request.refreshToken());
    }

    /**
     * 카카오 연결 끊기 통보(v0.4 §4 "카카오 연동 해제 처리").
     *
     * <p>사용자가 카카오 계정 설정에서 우리 앱 연결을 끊으면 카카오가 이 엔드포인트를 부른다.
     * 우리 쪽에서는 <b>계정 탈퇴(FR-043)와 같은 처리</b>를 한다 — 로그인 수단이 사라진 계정을 살려둘
     * 이유가 없고, 기록은 남겨야 하기 때문이다.
     *
     * <p>방장으로 남아 있어도 막지 않는다. 계정 탈퇴 API 는 양도를 요구하지만, 이쪽은 이미 카카오에서
     * 연결이 끊긴 뒤라 거절할 방법이 없다. 방장 없는 챌린지가 되면 재판정은 FR-012c 의 자동 승인이 받는다.
     *
     * <p>모르는 회원번호가 와도 200 이다. 카카오는 실패를 재시도하는데, 우리에게 없는 계정이면
     * 몇 번을 다시 보내도 결과가 같기 때문이다.
     */
    @Transactional
    public void handleKakaoUnlink(String authorizationHeader, KakaoUnlinkCallbackRequest request) {
        requireAdminKey(authorizationHeader);

        if (request == null || request.userId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "user_id 가 없습니다.");
        }

        String kakaoId = String.valueOf(request.userId());
        User user = userMapper.findByKakaoId(kakaoId);
        if (user == null) {
            log.info("카카오 연결 끊기 통보 — 이미 없거나 탈퇴한 계정입니다. kakaoId={}", kakaoId);
            return;
        }
        userService.withdrawAfterKakaoUnlink(user.getId());
        log.info("카카오 연결 끊기로 계정을 닫았습니다. userId={}, referrerType={}",
                user.getId(), request.referrerType());
    }

    private void requireAdminKey(String authorizationHeader) {
        String adminKey = kakaoProperties.adminKey();
        if (!StringUtils.hasText(adminKey)) {
            // 키가 없으면 누구나 남의 계정을 닫을 수 있게 되므로, 설정 전에는 아무도 통과시키지 않는다.
            throw new BusinessException(HttpStatus.UNAUTHORIZED,
                    "카카오 Admin 키가 설정되지 않아 콜백을 처리할 수 없습니다.");
        }
        if (authorizationHeader == null || !authorizationHeader.equals(ADMIN_KEY_PREFIX + adminKey)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "카카오 Admin 키가 올바르지 않습니다.");
        }
    }

    /** FR-028 약관 동의. 동의 시각은 DB 시계를 쓴다. */
    @Transactional
    public AuthResponses.TermsAgreement agreeTerms(Long userId, TermsAgreementRequest request) {
        if (userMapper.updateTermsAgreement(userId, request.termsVersion()) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        User user = userMapper.findById(userId);
        return new AuthResponses.TermsAgreement(
                user.getId(), user.getTermsAgreedAt(), user.getTermsVersion());
    }
}
