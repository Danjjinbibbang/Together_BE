package com.together.acceptance;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.together.common.security.JwtTokenProvider;
import com.together.dto.Account;
import com.together.dto.Challenge;
import com.together.dto.DailyMissionAssignment;
import com.together.dto.Member;
import com.together.dto.Mission;
import com.together.dto.User;
import com.together.mapper.AccountMapper;
import com.together.mapper.ChallengeMapper;
import com.together.mapper.DailyMissionAssignmentMapper;
import com.together.mapper.MemberMapper;
import com.together.mapper.MissionMapper;
import com.together.mapper.NotificationMapper;
import com.together.mapper.UserMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 인수 테스트 공통 바탕. HTTP 계층부터 실제 Oracle 까지 한 번에 태운다.
 *
 * <p>{@code MapperSmokeTest} 가 SQL 이 스키마와 맞는지를 본다면, 이쪽은 <b>권한·상태 전이·알림
 * 발송처럼 서비스가 판단하는 규칙</b>이 실제 요청에서 그대로 동작하는지를 본다. 라우팅만 확인하는
 * {@code ApiSurfaceTest} 와도 층이 다르다.
 *
 * <p>{@code @Transactional} 이라 각 테스트가 끝나면 전부 롤백된다. MockMvc 는 같은 스레드에서
 * 돌아 테스트 트랜잭션을 그대로 쓴다.
 *
 * <p>Spring Boot 4 에서 {@code @AutoConfigureMockMvc} 가 클래스패스에 없는 별도 모듈로 빠졌다.
 * 대신 컨텍스트에서 직접 MockMvc 를 만들고 <b>보안 필터 체인을 붙였다</b> — 인증 동작(401·403)까지
 * 진짜로 태우기 위해서다.
 */
@SpringBootTest
@Transactional
abstract class AcceptanceTestSupport {

    /** 유니크 제약(카카오ID·초대코드·닉네임)에 부딪히지 않도록 붙이는 일련번호. */
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private WebApplicationContext context;

    @Autowired protected JwtTokenProvider tokenProvider;
    @Autowired protected Clock clock;

    @Autowired protected UserMapper userMapper;
    @Autowired protected ChallengeMapper challengeMapper;
    @Autowired protected MemberMapper memberMapper;
    @Autowired protected AccountMapper accountMapper;
    @Autowired protected MissionMapper missionMapper;
    @Autowired protected DailyMissionAssignmentMapper assignmentMapper;
    @Autowired protected NotificationMapper notificationMapper;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected static String unique(String prefix) {
        return prefix + SEQ.incrementAndGet();
    }

    /** FR-033 프론트가 붙이는 것과 같은 형태의 인증 헤더. */
    protected String bearer(Long userId) {
        return "Bearer " + tokenProvider.createToken(userId);
    }

    /**
     * 요청 바디 JSON. Boot 4 는 Jackson 3(tools.jackson)으로 옮겨 가 com.fasterxml 쪽
     * ObjectMapper 빈이 없다. 바디가 전부 납작한 객체라 여기서 직접 만든다.
     *
     * <p>키·값을 번갈아 넘긴다. 숫자와 true/false/null 은 그대로, 나머지는 문자열로 감싼다.
     */
    protected String json(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(keyValues[i]).append("\":");
            String value = keyValues[i + 1];
            if (value == null || value.matches("-?[0-9]+|true|false|null")) {
                sb.append(value);
            } else {
                sb.append('"')
                        .append(value.replace("\\", "\\\\").replace("\"", "\\\""))
                        .append('"');
            }
        }
        return sb.append('}').toString();
    }

    // ------------------------------------------------------------------ 픽스처

    protected Long newUser() {
        User user = new User();
        user.setKakaoId(unique("acceptance-kakao-"));
        userMapper.insert(user);
        return user.getId();
    }

    protected Challenge newChallenge(long goalAmount) {
        Challenge challenge = new Challenge();
        challenge.setTitle(unique("인수 테스트 챌린지 "));
        challenge.setTargetAmount(goalAmount);
        challenge.setStartDate(LocalDate.now(clock).minusDays(1));
        challenge.setEndDate(LocalDate.now(clock).plusDays(30));
        challenge.setInviteCode(unique("ACPT"));
        challengeMapper.insert(challenge);
        return challengeMapper.findById(challenge.getId());
    }

    /** 멤버와 계좌를 함께 만든다 — 참여 API 가 만들어 주는 상태와 같다. */
    protected Member newMember(Long userId, Long challengeId, String role) {
        Member member = new Member();
        member.setUserId(userId);
        member.setChallengeId(challengeId);
        member.setNickname(unique("멤버"));
        member.setRole(role);
        member.setStatus("ACTIVE");
        memberMapper.insert(member);

        Account account = new Account();
        account.setMemberId(member.getId());
        account.setBalance(0L);
        accountMapper.insert(account);
        return member;
    }

    protected void addBalance(Long memberId, long amount) {
        accountMapper.addBalance(accountMapper.findByMemberId(memberId).getId(), amount);
    }

    protected long balanceOf(Long memberId) {
        return accountMapper.findByMemberId(memberId).getBalance();
    }

    protected Mission newMission(Long challengeId, Long ownerMemberId, int minTextLength) {
        Mission mission = new Mission();
        mission.setChallengeId(challengeId);
        mission.setTitle(unique("미션 "));
        mission.setSubmitType("TEXT");
        mission.setRewardMin(100);
        mission.setRewardMax(10_000);
        mission.setMinTextLength(minTextLength);
        mission.setCreatedByType("OWNER");
        mission.setCreatedByMemberId(ownerMemberId);
        mission.setIsActive("Y");
        missionMapper.insert(mission);
        return mission;
    }

    /**
     * 오늘의 배정을 직접 만든다. 서버가 뽑게 두면 보상 금액이 범위 안에서 랜덤이라
     * "이 입금으로 목표를 채운다"는 상황을 만들 수 없다.
     */
    protected DailyMissionAssignment assignToday(Long challengeId, Long missionId, int rewardAmount) {
        DailyMissionAssignment assignment = new DailyMissionAssignment();
        assignment.setChallengeId(challengeId);
        assignment.setMissionId(missionId);
        assignment.setAssignedDate(LocalDate.now(clock));
        assignment.setRewardAmount(rewardAmount);
        assignmentMapper.insert(assignment);
        return assignment;
    }

    /** 한 멤버가 받은 특정 종류의 알림 수. 발송 규칙을 확인할 때 쓴다. */
    protected long notificationCount(Long memberId, String notiType) {
        return notificationMapper.findByReceiverMemberId(memberId).stream()
                .filter(n -> notiType.equals(n.getNotiType()))
                .count();
    }
}
