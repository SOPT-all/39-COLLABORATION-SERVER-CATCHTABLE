# Catchtable Server

캐치테이블의 매장 선택 경험을 개선하는 SOPT 합동세미나 백엔드 프로젝트입니다.



## 🛠️ 기술 스택

| 분류    | 기술                     | 버전    |
| ----- | ---------------------- | ----- |
| 언어    | Java                   | 21    |
| 프레임워크 | Spring Boot            | 4.1.0 |
| DB    | MySQL                  | —     |
| 인프라   | AWS                    | —     |
| 컨테이너  | Docker, Docker Compose | —     |
| CI/CD | GitHub Actions         | —     |
| 협업    | GitHub, Notion         | —     |



## 📌 구현 범위

- 매장 상세 조회
- 투표 생성
- 현재 사용자가 생성한 진행 중 투표 조회
- 투표 현황 조회
- 투표 제출
- 생성자의 투표 종료



## 📂 패키지 구조

도메인별로 패키지를 구성하고, 각 도메인 내부를 역할에 따라 구분합니다.

아래는 개발 시 사용할 예정 구조입니다. 빈 패키지는 Git에 포함되지 않으므로 실제 코드 작성 시 생성합니다.

```text
org.sopt.catchtable
├── CatchtableApplication.java
├── global
│   ├── config
│   ├── response
│   └── exception
├── user
│   ├── repository
│   └── domain
├── store
│   ├── controller
│   ├── service
│   ├── repository
│   ├── domain
│   └── dto
└── vote
    ├── controller
    ├── service
    ├── repository
    ├── domain
    └── dto
```

| 폴더명 | 설명 |
| --- | --- |
| `global` | 여러 도메인에서 사용하는 공통 코드 |
| `global/config` | CORS 등 공통 설정 |
| `global/response` | 공통 응답 객체 `BaseResponse` 및 성공 코드 |
| `global/exception` | 에러 코드, 공통 예외, `GlobalExceptionHandler` |
| `user` | 시연용 사용자 관련 코드. 현재 사용자 전용 API가 없어 `domain`, `repository`만 구성 |
| `store` | 매장 상세 및 관련 정보 조회 코드 |
| `vote` | 투표 생성·조회·참여·종료 관련 코드 |
| 각 도메인의 `controller` | HTTP 요청을 받고 응답을 반환 |
| 각 도메인의 `service` | 서비스 규칙 및 처리 흐름 구현 |
| 각 도메인의 `repository` | DB 저장·조회 |
| 각 도메인의 `domain` | 엔티티 및 관련 Enum |
| 각 도메인의 `dto` | API 요청·응답 객체 |
| `main/resources` | 애플리케이션 설정 및 리소스 |
| `test` | 테스트 코드 |


## 🌿 브랜치 전략

`main`에서 작업 브랜치를 생성하고, 작업 완료 후 PR을 통해 `main`에 병합합니다. `develop` 브랜치는 사용하지 않습니다.

| 브랜치        | 용도         |
| ---------- | ---------- |
| `main`     | 기능 통합 및 배포 |
| `타입/#이슈번호` | 개별 작업      |

- 브랜치 예시: `feat/#1`, `bug/#2`, `chore/#3`
- 작업 시작 전 관련 이슈를 생성합니다.
- 최신 `main`을 기준으로 작업 브랜치를 생성합니다.



## 📝 커밋/PR 컨벤션

커밋 메시지와 PR 제목은 `타입: 작업 내용` 형식으로 작성합니다.

| 타입            | 설명             | 예시                         |
| ------------- | -------------- | -------------------------- |
| ✨ `feat`      | 새로운 기능 추가      | `feat: 투표 생성 API 구현`       |
| ♻️ `refactor` | 리팩토링           | `refactor: 투표 검증 로직 분리`    |
| 🐛 `bug`      | 버그 수정          | `bug: 종료된 투표에 제출되는 오류 수정`  |
| 📝 `docs`     | 문서 수정          | `docs: README 배포 방법 추가`    |
| ✅ `test`      | 테스트 코드 추가/수정   | `test: 투표 종료 테스트 추가`       |
| 📦 `build`    | 빌드 시스템, 의존성 설정 | `build: Validation 의존성 추가` |
| 🔨 `chore`    | 그 외 잡무         | `chore: 불필요한 주석 제거`        |



## 🤝 코드 리뷰 및 머지 규칙

- PR 생성 시 다른 서버 팀원 모두를 리뷰어로 지정합니다.
- 리뷰는 요청 후 최대 2일 이내에 진행합니다.
- 최소 1명의 승인을 받은 후 PR 작성자가 머지합니다.
- 머지 후 작업 브랜치는 삭제하지 않습니다.
- PR 본문에 관련 이슈와 변경 내용, 검증 결과를 작성합니다.



## 💻 네이밍 규칙

| 대상  | 규칙               | 예시                    |
| --- | ---------------- | --------------------- |
| 클래스 | PascalCase       | `UserRepository`      |
| 메서드 | camelCase        | `getUserInfo()`       |
| 변수  | camelCase        | `userName`            |
| 상수  | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`     |
| 패키지 | 소문자              | `org.sopt.catchtable` |



## ⚙️ 초기 설정 안내

- Java 21을 사용합니다.
- Gradle Wrapper를 사용하여 빌드합니다.
- 로컬 실행 시 Docker가 켜져 있어야 합니다. `compose.yaml`의 MySQL이 자동으로 실행·연결됩니다.
- 비밀번호 등 민감한 정보는 저장소에 커밋하지 않습니다.
- 공통 응답·예외 처리는 서버 킥오프에서 합의한 명세를 기준으로 별도 구현합니다.



## 🚀 배포

`main`에 머지되면 GitHub Actions가 Docker 이미지를 빌드해 Docker Hub에 올리고, EC2에서 새 이미지로 교체 실행합니다.

EC2의 `~/catchtable.env`에 아래 환경변수를 설정합니다.

| 변수 | 설명 | 예시 |
| --- | --- | --- |
| `DB_URL` | RDS 접속 URL | `jdbc:mysql://<엔드포인트>:3306/catchtable` |
| `DB_USERNAME` | DB 사용자 | — |
| `DB_PASSWORD` | DB 비밀번호 | — |
| `JPA_DDL_AUTO` | 테이블 자동 변경 방식 | `validate` |

> `JPA_DDL_AUTO`를 설정하지 않으면 기본값 `update`가 적용되어 서버 시작 시 운영 DB 테이블 구조가 변경되므로, `JPA_DDL_AUTO=validate`로 설정하고 이후 엔티티를 변경할 때는 운영 DB에 직접 DDL을 적용한 뒤 배포합니다.
