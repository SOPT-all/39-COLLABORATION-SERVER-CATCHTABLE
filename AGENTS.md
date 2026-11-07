# 프로젝트 개요
- 스택: Java 21, Spring Boot 4.1.2, Spring Data JPA, MySQL, Lombok, Gradle
- 구조: `org.sopt.catchtable` 아래 도메인별 패키지(`user`, `store`, `vote`) + `global`(`config`/`response`/`exception`)
  - 각 도메인은 `controller` / `service` / `repository` / `domain` / `dto`로 구분

# 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 로컬 실행: `./gradlew bootRun` (`compose.yaml`의 MySQL이 Docker로 자동 실행됨, Docker 필요)

# 규칙
- 브랜치: `타입/#이슈번호` (예: `feat/#1`), `main`에서 분기, `develop` 없음
- 커밋/PR 제목: `타입: 작업 내용` (feat, refactor, bug, docs, test, build, chore)
- 응답은 `global/response`의 `BaseResponse`, 예외는 `global/exception`의 공통 예외로 처리
- 작업 후 `./gradlew test` 통과 확인

# 개발 원칙
## 성능
- 연관관계는 `@ManyToOne(fetch = LAZY)`로 선언, 목록 조회 시 N+1 여부 확인 (필요하면 fetch join / `@EntityGraph`)
- 조회 전용 서비스 메서드는 `@Transactional(readOnly = true)`
- 반복문 안에서 쿼리 호출 금지, 필요한 데이터는 한 번에 조회
- 자주 조회하는 조건 컬럼(FK, 상태값 등)은 인덱스 고려

## 계층·의존성
- 의존 방향은 `controller → service → repository`만 허용 (controller에서 repository 직접 호출 금지)
- 다른 도메인의 repository를 직접 주입하지 않기 (예: `VoteService`에서 `StoreRepository` 금지 → `StoreService`를 통해 조회)
- 서비스 간 순환 의존 금지 (A ↔ B가 서로 주입하면 구조를 다시 나눌 것)
- `domain`(엔티티)은 `dto`, `controller`, `service`를 참조하지 않기
- DTO ↔ 엔티티 변환은 DTO 쪽(정적 팩토리 `from()` 등)에서 처리
- 여러 도메인이 함께 쓰는 코드만 `global`에 두고, 특정 도메인 로직은 `global`에 넣지 않기

## 보안
- 요청 DTO는 `@Valid` + Bean Validation(`@NotNull`, `@Size` 등)으로 검증
- 엔티티를 응답으로 직접 반환하지 않고 DTO로 변환
- 권한 확인은 서비스 계층에서 수행 (예: 투표 종료는 생성자만 가능)
- 쿼리는 JPA 메서드 / JPQL 파라미터 바인딩만 사용, 문자열 이어붙이기 금지
- 에러 응답·로그에 스택트레이스, 내부 정보, 민감 정보 노출 금지

## 안정성
- 상태를 바꾸는 로직은 서비스 메서드에 `@Transactional`로 묶기
- 동시 요청 시 데이터가 깨질 수 있는 곳(투표 제출, 중복 투표)은 유니크 제약 또는 락으로 방어
- 비즈니스 규칙 위반(종료된 투표에 제출 등)은 공통 예외 + 에러 코드로 처리, `null` 반환으로 넘기지 않기
- 새 기능에는 정상 케이스와 주요 실패 케이스 테스트를 함께 작성

# 절대 금지
- 운영 환경 DB·서버 접근 금지
- 비밀번호, 키 등 민감 정보 커밋 금지 (`application.yaml`에 실제 값 하드코딩 금지)
- `main` 브랜치에 직접 커밋/푸시 금지
- 요청하지 않은 의존성 추가, 패키지 구조 변경, 대규모 리팩토링 금지
- 예상과 다른 연결/에러 발생 시 즉시 중단하고, 그 전에 실행한 작업까지 보고

# 참고 문서
- 컨벤션·패키지 구조 상세: `README.md`
