# SOH API 단위 테스트 결과서

- 대상 시스템: `Senior-Oral-Healthcare-api`
- 테스트 일시: 2026-08-11 22:48 KST
- 대상 브랜치 / 작업 기준 커밋: `prod` / `832ccfaf`
- 테스트 범위: `api_server`의 Gradle/JUnit 전체 테스트
- 최종 판정: **PASS** — 153건 전체 통과

## 1. 요약

| 항목 | 결과 |
|---|---:|
| 테스트 스위트 | 41개 |
| 전체 테스트 | 153건 |
| 통과 | 153건 |
| 실패 | 0건 |
| 오류 | 0건 |
| 건너뜀 | 0건 |
| 통과율 | 100.00% |
| 테스트 실행시간(XML 합계) | 6.169초 |
| 전체 Gradle 실행시간 | 52초 |

초기 실패 3건의 원인을 수정한 뒤 전체 테스트를 다시 실행했다. ApplicationContext 테스트는 운영 DB 비밀값 없이 H2 인메모리 DB로 독립 실행되며, 구강검진 응답에 추가된 필드가 REST Docs 명세와 동기화됐다. 문자 인코딩도 Gradle 설정에서 UTF-8로 고정했다.

## 2. 실행 환경과 명령

| 구분 | 값 |
|---|---|
| 운영체제 | Windows / PowerShell |
| Java | Eclipse Temurin 17.0.17+10 |
| Gradle | Wrapper 8.7 |
| Spring Boot | 3.1.0 |
| 문자셋 | Gradle 테스트 JVM 및 Java 컴파일 UTF-8 고정 |
| 작업 디렉터리 | `api_server` |

최종 전체 실행 명령:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean test --no-daemon --console=plain
```

현재 PC에서는 `JAVA_HOME`에 실제 설치된 Eclipse Temurin 17 경로를 지정해 실행했다.

## 3. 스위트별 결과

| 구분 | 스위트 | 테스트 | 통과 | 실패 | 판정 |
|---|---|---:|---:|---:|---|
| 전체 | 41개 전체 스위트 | 153 | 153 | 0 | PASS |
| 환경 수정 확인 | `DentixApplicationTest` | 1 | 1 | 0 | PASS |
| 문서 수정 확인 | `OralCheckControllerTest` | 5 | 5 | 0 | PASS |

## 4. 실패 3건 수정 내역

### 4.1 ApplicationContext 로딩 실패

- 테스트 런타임에 H2 의존성을 추가했다.
- `DentixApplicationTest`에 H2 인메모리 datasource를 명시했다.
- MySQL 전용 스키마 생성 구문과 초기 데이터 실행기의 영향을 피하도록 Hibernate DDL 생성을 비활성화하고 DB 초기화 컴포넌트 4개를 mock 처리했다.
- 결과: `DentixApplicationTest.contextLoads()` 통과.

### 4.2 구강 검진 결과 상세 REST Docs 실패

- 테스트 응답에 `oralCheckAnalysisType` 값을 명시했다.
- `oralCheckAnalysisType`, `gingivitisUpCheck`, `gingivitisDownCheck`, `gingivitisAllTeethCheck`, `gingivitisImageName` 필드를 응답 문서에 추가했다.
- null 가능 필드는 `optional()`로 선언했다.
- 결과: 상세 조회 테스트 및 REST Docs 스니펫 생성 통과.

### 4.3 구강 상태 타임라인 REST Docs 실패

- 테스트 응답에 `oralCheckAnalysisType` 값을 명시했다.
- `response.dailyList[].detailList[].oralCheckAnalysisType` 필드를 응답 문서에 추가했다.
- 결과: 타임라인 조회 테스트 및 REST Docs 스니펫 생성 통과.

## 5. 재검증 이력

| 실행 | 전체 | 통과 | 실패 | 비고 |
|---|---:|---:|---:|---|
| 최초 전체 실행 | 153 | 143 | 10 | Windows 기본 문자셋 영향 포함 |
| UTF-8 적용 후 | 153 | 150 | 3 | 실제 수정 대상 식별 |
| 수정 후 최종 전체 실행 | 153 | 153 | 0 | 최종 PASS |

Gradle 테스트 JVM에 `-Dfile.encoding=UTF-8`을 적용하고 Java 컴파일 인코딩도 UTF-8로 지정해 Windows와 CI의 한글 REST Docs 처리 차이를 제거했다. 최종 실행은 별도 `JAVA_TOOL_OPTIONS` 없이 성공했다.

## 6. 빌드 경고

- Lombok `@Builder`가 필드 초기값을 무시할 수 있다는 경고 12건이 발생했다.
- 관련 파일: `User`, `SubscriptionUsage`, `SubscriptionPlan`, `Organization`, `OrganizationSubscriptionHistory` 엔티티
- 일부 deprecated API 및 unchecked/unsafe operation 사용 안내가 발생했다.
- Gradle 9.0과 호환되지 않는 deprecated Gradle 기능 사용 경고가 발생했다.

경고는 테스트 실패나 배포 차단 사유는 아니지만, 기본값이 필요한 필드에는 `@Builder.Default` 적용 여부를 검토하고 Gradle 9 전환 전에 `--warning-mode all`로 상세 원인을 정리하는 것이 바람직하다.

## 7. 판정 및 배포 적합성

최종 테스트 게이트는 **PASS**다. 전체 153건이 통과했고 실패·오류·건너뜀은 없다. 이번 수정 범위의 단위 테스트 및 REST Docs 검증 기준으로 배포 가능 상태다.

단, 실제 RDS·AWS·DID/토큰 외부 서버 연동은 이번 단위 테스트 범위가 아니므로 배포 후 운영 헬스 체크와 GitHub Actions 결과를 별도로 확인한다.

## 8. 범위 및 근거 자료

- 포함: 백엔드 Java 컴파일, 테스트 컴파일, JUnit 153건, Spring MVC/REST Docs 테스트
- 제외: 프론트엔드 테스트, 실제 RDS 연동, AWS Polly/S3, DID·토큰 외부 서버, 배포 환경 E2E, 성능·보안 테스트
- 코드 커버리지: JaCoCo 등 커버리지 도구가 구성되어 있지 않아 측정하지 않음
- Gradle HTML 결과: `api_server/build/reports/tests/test/index.html`
- JUnit XML 결과: `api_server/build/test-results/test/TEST-*.xml`

`build` 아래 근거 자료는 `clean test` 실행 시 다시 생성되는 로컬 산출물이며 저장소 커밋 대상은 아니다.
