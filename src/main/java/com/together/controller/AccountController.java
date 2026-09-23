package com.together.controller;

import com.together.common.security.LoginUserId;
import com.together.dto.response.AccountResponses;
import com.together.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account")
@RestController
@RequestMapping("/challenges/{challengeId}/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "챌린지 전체 계좌 합산 (화면 3·8)")
    @GetMapping
    public AccountResponses.ChallengeAccounts challengeAccounts(@LoginUserId Long userId,
                                                                @PathVariable Long challengeId) {
        return accountService.challengeAccounts(userId, challengeId);
    }

    @Operation(summary = "내 가상 계좌 잔액 (화면 13)")
    @GetMapping("/me")
    public AccountResponses.MyAccount myAccount(@LoginUserId Long userId,
                                                @PathVariable Long challengeId) {
        return accountService.myAccount(userId, challengeId);
    }

    @Operation(summary = "거래 내역 (화면 13, 본인 전용). 오프셋 페이지네이션")
    @GetMapping("/me/transactions")
    public AccountResponses.TransactionList myTransactions(
            @LoginUserId Long userId,
            @PathVariable Long challengeId,
            @Parameter(description = "0부터 시작, 기본 0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "기본 20, 최대 100")
            @RequestParam(required = false) Integer size) {
        return accountService.myTransactions(userId, challengeId, page, size);
    }
}
