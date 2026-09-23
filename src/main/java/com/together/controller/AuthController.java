package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.request.KakaoLoginRequest;
import com.together.dto.request.KakaoUnlinkCallbackRequest;
import com.together.dto.request.LogoutRequest;
import com.together.dto.request.TermsAgreementRequest;
import com.together.dto.request.TokenRefreshRequest;
import com.together.dto.response.AuthResponses;
import com.together.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "카카오 인가코드로 로그인/회원가입")
    @PostMapping("/kakao")
    public AuthResponses.KakaoLogin loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        return authService.loginWithKakao(request);
    }

    @Operation(summary = "액세스 토큰 갱신 (FR-035). 인증 헤더 없이 리프레시 토큰만 보낸다")
    @PostMapping("/refresh")
    public AuthResponses.TokenPair refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "로그아웃 (FR-035). 리프레시 토큰을 폐기한다")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@LoginUserId Long userId, @Valid @RequestBody LogoutRequest request) {
        authService.logout(userId, request);
    }

    @Operation(summary = "카카오 연결 끊기 통보 수신 (카카오가 호출). 계정 탈퇴와 같은 처리를 한다")
    @PostMapping("/kakao/unlink-callback")
    @ResponseStatus(HttpStatus.OK)
    public void kakaoUnlinkCallback(
            @Parameter(description = "KakaoAK {ADMIN_KEY} 형식. 카카오 콘솔에 등록한 값과 대조한다")
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) KakaoUnlinkCallbackRequest request) {
        authService.handleKakaoUnlink(authorization, request);
    }

    @Operation(summary = "최초 가입자 약관 동의 처리 (화면 1)")
    @PostMapping("/terms-agreement")
    public AuthResponses.TermsAgreement agreeTerms(@LoginUserId Long userId,
                                                   @Valid @RequestBody TermsAgreementRequest request) {
        return authService.agreeTerms(userId, request);
    }
}
