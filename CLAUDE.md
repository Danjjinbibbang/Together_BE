# 프로젝트 개요

"같이 모으기" 백엔드 레포. 친구/소모임이 함께 저축 목표를 세우고 매일 랜덤 미션을 수행해 가상 계좌에 저축하는 소셜 저축 챌린지 서비스.

요구사항정의서/기획서/와이어프레임은 Notion에서 관리하며 Notion MCP로 연결되어 있음. API 구현 전 Notion에서 최신 요구사항정의서(특히 FR-012 계열의 미션 완료·오탐 처리 흐름)를 먼저 확인할 것. 프론트엔드는 별도 레포(`같이모으기-frontend`)이므로 API 계약은 아래 규칙을 따를 것.

# 기술 스택

- Java 17 + Spring Boot
- 영속성: MyBatis
- DB: Oracle
- 인증: 카카오 소셜 로그인만 지원. 카카오 인가코드를 받아 카카오 토큰 교환 후 자체 JWT 발급

# 폴더 구조

`/controller`(요청 검증, 로직 없음) · `/service`(비즈니스 로직, 트랜잭션 경계) · `/mapper`(MyBatis 인터페이스, `/resources/mapper` XML과 1:1) · `/dto`(request/response, 계층이 아니라 데이터 객체)

# 계층 원칙

Controller → Service → Mapper → Oracle 순으로 흐르고, DTO는 계층이 아니라 그 사이를 오가는 데이터. Mapper에는 비즈니스 로직(조건 분기)을 넣지 말 것.

# 계정 / 챌린지 데이터 구조

- `User`: 카카오 로그인으로 생성되는 전역 계정 (kakao_id, email)
- `Member`: 챌린지별 참여 정보. 닉네임은 여기 저장 (User가 아니라 Member에 귀속 → 챌린지마다 다른 닉네임 가능)
- 제약조건: `UNIQUE(user_id, challenge_id)`, `UNIQUE(challenge_id, nickname)`

# API 계약

새 엔드포인트가 필요하거나 응답 구조가 바뀌어야 하면, 프론트가 뭘 필요로 할지 추측하지 말고 Notion의 API 명세 페이지를 먼저 확인/갱신한 뒤 구현할 것.

# 하지 말아야 할 것

- DB 계정 정보 등 시크릿 값 커밋 금지 (환경변수 사용)
- 요구사항정의서에 없는 엔드포인트/필드를 임의로 추가하지 말 것