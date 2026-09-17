# SOH API 작업 지침

작업 시작 시 이 파일과 git 상태를 확인합니다. 상세 문서는 필요한 주제만 조회합니다.
핵심 규칙과 문서 위치만 80~120줄 이내로 유지하며 변경 이력을 누적하지 않습니다.

## 저장소와 환경

- Backend: `ThomastoneCoLTD/Senior-Oral-Healthcare-api` (현재 workspace).
- Frontend: `ThomastoneCoLTD/Senior-Oral-Healthcare-front` (별도 git 저장소).
- 현재 PC frontend: `C:\Users\ethanj\Documents\GitHub\Senior-Oral-Healthcare-front`.
- 다른 PC에서는 연동 저장소 위치를 확인합니다. frontend 수정 시 해당 저장소 지침도 따릅니다.
- Stack: Java 17, Spring Boot, Gradle Wrapper; 애플리케이션 작업 디렉터리는 `api_server`.
- `prod` push만 운영 배포합니다. `main`, `dev` push는 배포하지 않습니다.
- `main`은 운영 검증 완료 코드를 보관합니다. 요청 없이 브랜치를 전환하거나 병합하지 않습니다.
- AWS dev는 폐기 상태입니다. 남은 dev workflow를 활성화하거나 dev 인프라를 재생성하지 않습니다.

## 운영 상수와 보안

- AWS account: `160885266674`, region: `ap-northeast-2`.
- API base: `https://api.soh.thomabio.com/api`; health: `/api/actuator/health`.
- Frontend: `https://soh.thomabio.com`; API의 `/api` mount 계약을 유지합니다.
- Workflow: `.github/workflows/deploy-api-prod.yml`; ASG: `soh-api-prod-asg`.
- 배포 파일: `s3://denti-backends/soh/prod/app.jar`, `s3://denti-backends/soh/prod/.env`.
- 배포 Secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `SOH_API_ENV_PROD`.
- Terraform Secret: `SOH_TERRAFORM_TFVARS_PROD_HCL`; HCL만 저장하고 앱 env와 비밀값을 섞지 않습니다.
- 실제 AWS 키, DB 비밀번호, JWT secret, 지갑/RSA 개인키, 토큰을 소스·문서·로그에 기록하지 않습니다.
- `.env`, tfvars, state, 빌드 산출물은 커밋하지 않습니다. Terraform 입력은 파싱 전 로그 마스킹을 유지합니다.
- DB 자격증명 원본은 RDS managed Secret입니다. GitHub/S3 env에 정적 datasource 키를 복사하지 않습니다.
- 금지 키: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_DRIVER_CLASS_NAME`.
- JDBC는 `com.amazonaws.secretsmanager.sql.AWSSecretsManagerMySQLDriver`와 `jdbc-secretsmanager:mysql://`을 사용합니다.
- JDBC username은 managed Secret ARN, password는 빈 값입니다. EC2의 Secret 조회 권한은 해당 Secret으로 제한합니다.
- JWT 운영 Secret은 `JWT_ACCESS_KEY_PROD`, `JWT_REFRESH_KEY_PROD`이며 각각 32자 이상, 기본값 없이 유지합니다.
- `DAEGU_CHAIN_WALLET_ENCRYPTION_KEY_PROD`는 32바이트 Base64 AES-256-GCM 키입니다. 기존 지갑 복호화에 필요하므로 임의 교체·분실하지 않습니다.
- 다대구 RSA 키는 backend에만 두고 인증 callback 원문·지갑 `holder_pkey` 등 민감 필드는 마스킹합니다.
- 과거 문서의 인스턴스 수·Secret 회전·장애 해결 상태는 현재 사실이 아닐 수 있으므로 해당 운영 작업 때 재확인합니다.

## 인프라 안전 규칙

- Codex에서 직접 `terraform apply`하지 않습니다. GitHub Actions 또는 검토된 수동 절차를 사용합니다.
- prod가 사용하는 `soh-api-dev-vpc` 등 공유 VPC·서브넷·NAT는 이름에 dev가 있어도 삭제하지 않습니다.
- 운영 RDS `soh-api-prod-mysql`의 승인된 버전은 `8.4.10`입니다. `8.4`로 축약하거나 다운그레이드하지 않습니다.
- `create_route53_record = true`를 유지합니다. false는 운영 API DNS 삭제를 유발합니다.
- prod refresh는 `MinHealthyPercentage=100`, workflow는 `cancel-in-progress: false`를 유지합니다.
- ASG의 명시적 Launch Template 버전은 Terraform 기본 버전과 일치해야 합니다.
- 배포 전 Launch Template/Secrets Manager JDBC 검증을 유지하고 기존 refresh와 중복 실행하지 않습니다.

## 작업 범위와 토큰 절약

- 경로와 심볼을 `rg --files`, `rg -n`으로 찾고 필요한 줄 범위만 읽습니다.
- 큰 파일·전체 로그를 반복 출력하지 않고 `.git`, `.gradle`, `build`, 생성 문서·이력은 기본 탐색에서 제외합니다.
- 확인한 정보는 재사용합니다. 변경이나 실패가 없으면 같은 조회·검증을 반복하지 않습니다.
- API 요청·응답·인증·데이터 계약이 바뀌면 관련 frontend client/화면을 확인합니다.
- 문구·문서만 변경하면 frontend 전체 탐색이나 전체 테스트로 자동 확장하지 않습니다.
- AWS/GitHub 전체 점검은 운영 변경 또는 명시적 점검 요청 때 수행합니다.
- 별개 주제는 짧은 인수 메모와 새 대화 사용을 권장합니다. 요청 없이 새 작업을 만들지 않습니다.
- 사용자 변경을 되돌리지 않고 이번 작업의 파일만 명시적으로 stage합니다.

## 검증

- Java 17 설치 경로와 `JAVA_HOME`을 확인하고 `api_server`에서 Wrapper를 실행합니다.
- 관련 로직: `./gradlew test --tests '<관련 테스트 클래스>'`; Windows는 `.\gradlew.bat`을 사용합니다.
- 빌드 영향 변경: `./gradlew bootJar -x test -x asciidoctor`; 테스트 생략 빌드를 테스트 통과로 보고하지 않습니다.
- 인증·공유 로직·API/DB 계약 변경은 영향 범위 테스트를 확대합니다. 매번 clean·전체 테스트를 강제하지 않습니다.
- Terraform 변경 시 해당 디렉터리의 fmt/validate, workflow 변경 시 YAML과 관련 guard를 검증합니다.
- 문서만 변경: 링크·경로·규칙 충돌·`git diff --check`를 확인하고 앱 build/test는 생략합니다.
- prod push가 배포를 시작하면 최종 workflow 결과를 확인하고, refresh 시작과 완료·health 확인을 구별해 보고합니다.
- 실제 운영 테스트·DB 변경은 작업 범위와 부작용을 확인한 뒤 수행합니다.

## 문서 유지

- README는 설치·운영·배포·기능 계약, 이 파일은 핵심 규칙·문서 위치만 유지합니다.
- 두 파일을 매번 검토하되 관련 사실이 바뀔 때만 수정하고 날짜별 이력을 누적하지 않습니다.
- 코드·설정·인프라·운영 변경: `docs/updates/YYYY-MM-DD_SOH_변경기록_<주제>.docx`.
- 문서만 변경하거나 단순 조사한 결과는 같은 디렉터리의 날짜별 `.md`로 기록할 수 있습니다.
- 전체 누적 인수인계 DOCX는 릴리스 확정 또는 외부 인계 요청 때 생성하며 작은 수정·매 push마다 재작성하지 않습니다.
- 릴리스는 기능 묶음의 검증 완료 또는 운영 인계 시점이며 단순 prod 자동 배포와 구별합니다.
- 릴리스 문서: `docs/handover/YYYY-MM-DD_SOH_외부기업_인수인계_<주제>.docx`.
- 직전 스냅샷 이후 변경기록을 합쳐 전체 기능·코드 지도·API·DB·외부 연동·운영 기준을 갱신합니다.
- 기존 개발정보 및 날짜별 DOCX는 덮어쓰거나 삭제하지 않습니다. 같은 날짜는 주제·순번으로 구분합니다.
- 기록에는 기준 commit, 환경, 변경 위치, API/DB/Secret/배포 영향, 검증, 남은 위험과 rollback을 포함하며 비밀값은 제외합니다.

## 문서 찾아보기

- 설치·배포·기능 계약: [README.md](README.md)에서 관련 제목만 검색합니다.
- 수동 AWS 구축: [readme_수동.md](readme_수동.md); 폐기된 dev 절차는 실행하지 않습니다.
- 과거 구현 이유·미확인 항목: [이전 지침 보관본](docs/history/2026-09-15_AGENTS_archive.md)에서 관련 키워드만 검색합니다.
- 최근 변경은 `docs/updates/`, 외부 인계는 `docs/handover/`의 관련 파일과 이후 변경기록을 참조합니다.
- 코드 진입점은 `api_server/src/main`, 테스트는 `api_server/src/test`에서 관련 Controller/Service/DTO를 찾습니다.
- 최초 건강설문은 `domain/intakeSurvey`와 `template/intake-survey.json`입니다. 기존 사용자도 미제출이면 대상이며 신규 API 배포 후 프론트를 배포합니다. 완료·임시저장 데이터는 보존합니다.
- 기능 수정 전 잠금·진도·토큰 중복 방지·인증 계약을 관련 코드와 기록에서 확인합니다.
- 사용자 삭제 시 토큰 회수 실패를 무시하지 않으며, 지갑 초기화·리워드 상태 변경의 기존 보호 조건을 유지합니다.

## 완료와 Git

관련 코드와 문서를 함께 검증·커밋·푸시합니다. 양쪽 저장소를 수정하면 각각 수행합니다.

```bash
git status --short
git diff --check
git add <이번 작업 관련 파일>
git commit -m "<명확한 메시지>"
git push origin <현재 브랜치>
```

완료 보고에는 결과, 검증, commit/push/배포 상태와 남은 제한만 간결히 적습니다.
