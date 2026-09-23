--------------------------------------------------------------------------------
-- 2026-08-25 마이그레이션 — 계정 탈퇴(FR-043) / 캘린더·스트릭(FR-025, FR-027, FR-040)
--
-- schema-oracle.sql 은 새로 만드는 DB 용이고, 이 파일은 이미 만들어진 DB 를 따라잡게 한다.
-- 두 파일의 최종 형태는 같아야 한다.
--
-- 캘린더(GET /me/calendar)·날짜별 목록(GET /me/mission-logs)·스트릭은 기존 테이블만으로
-- 계산되므로 스키마 변경이 없다. 조회 경로가 MEMBER(USER_ID) → MISSION_LOG(MEMBER_ID) →
-- DAILY_MISSION_ASSIGNMENT(PK) 라 IX_MEMBER_USER 와 IX_MISSION_LOG_MEMBER 를 그대로 탄다.
--------------------------------------------------------------------------------

-- 계정 탈퇴(FR-043). NULL 이면 활성 계정이다.
-- 행을 지우지 않는 이유는 챌린지 탈퇴(FR-038)와 같다 — MEMBER 를 참조하는 과거 기록
-- (미션 로그·거래·댓글)이 남아야 하고, 닉네임도 MEMBER 에 있어 팀 기록이 깨지지 않는다.
ALTER TABLE USERS ADD (WITHDRAWN_AT TIMESTAMP);

-- 탈퇴 시 KAKAO_ID 를 'withdrawn:'||ID 로 치환하므로(개인정보 파기 + 같은 카카오 계정의
-- 재가입 허용) UK_USERS_KAKAO_ID 는 그대로 둔다. 별도 변경 없음.
