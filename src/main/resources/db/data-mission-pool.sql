--------------------------------------------------------------------------------
-- 고정 미션 풀 시드 데이터 (FR-007, FR-010)
-- 요구사항정의서의 "미션 유형 예시" 표를 그대로 옮긴 것이다.
-- 풀이 작으면 카드뽑기 연출이 무의미해지므로(v0.2 변경 이력 참고) 운영 전 보강이 필요하다.
--
-- CHALLENGE_ID 가 NULL 이라 전역 공용 풀로 들어간다. CREATED_BY_TYPE 도 NULL 인데,
-- API 명세서의 OWNER/AI 는 방장이 등록한 챌린지 전용 미션에만 해당하기 때문이다.
-- 전역 풀을 폐지하기로 하면 이 파일과 MISSION.CHALLENGE_ID nullable 을 함께 정리해야 한다.
--
-- 보상은 고정값이 아니라 범위다(claude 컨텍스트 §12). 실제 지급액은 오늘의 미션 배정 시점에
-- 이 범위 안에서 균등 랜덤으로 한 번 뽑아 DAILY_MISSION_ASSIGNMENT.REWARD_AMOUNT 에 저장된다.
--------------------------------------------------------------------------------
INSERT INTO MISSION (TITLE, SUBMIT_TYPE, REWARD_MIN, REWARD_MAX, MIN_TEXT_LENGTH)
VALUES ('오늘 감사한 점 3가지 적기', 'TEXT', 100, 2000, 30);

INSERT INTO MISSION (TITLE, SUBMIT_TYPE, REWARD_MIN, REWARD_MAX, MIN_TEXT_LENGTH)
VALUES ('오늘을 키워드 3개로 표현하기', 'TEXT', 100, 1000, 15);

INSERT INTO MISSION (TITLE, SUBMIT_TYPE, REWARD_MIN, REWARD_MAX, MIN_TEXT_LENGTH)
VALUES ('오늘 하루 한 줄 회고', 'TEXT', 100, 1500, 20);

INSERT INTO MISSION (TITLE, SUBMIT_TYPE, REWARD_MIN, REWARD_MAX, MIN_TEXT_LENGTH)
VALUES ('오늘 기분 이모지 + 이유 한 줄', 'TEXT', 100, 800, 15);

INSERT INTO MISSION (TITLE, SUBMIT_TYPE, REWARD_MIN, REWARD_MAX, MIN_TEXT_LENGTH)
VALUES ('오늘 나에게 칭찬 한마디', 'TEXT', 100, 800, 15);

COMMIT;
