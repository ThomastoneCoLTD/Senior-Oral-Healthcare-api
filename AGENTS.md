# SOH API 작업 인수인계 문서

이 문서는 Codex와 다른 작업자가 어느 PC에서든 `Senior-Oral-Healthcare-api` 저장소를 이어받아 작업할 수 있도록 유지하는 운영 메모입니다. 작업을 시작할 때 항상 이 파일을 먼저 확인하고, 중요한 변경이 생기면 같은 커밋에서 이 파일도 갱신합니다.

## 기본 작업 규칙

- 기능 추가/수정 시 백엔드만 보지 말고 프론트엔드도 함께 확인합니다.
- 현재 PC의 프론트엔드 저장소 경로는 `C:\Users\hana0\workspace\Senior-Oral-Healthcare-front`입니다.
- 다른 PC에서는 프론트엔드 폴더가 다를 수 있으므로 작업 시작 전 workspace에서 `Senior-Oral-Healthcare-front` 저장소 위치를 다시 확인합니다.
- 백엔드와 프론트엔드 저장소는 각각 별도 git 저장소이므로, 양쪽을 수정했다면 양쪽에서 각각 검증, 커밋, 푸시합니다.
- 사용자나 다른 작업자가 만든 변경은 되돌리지 않습니다. 현재 작업과 관련된 파일만 명시적으로 stage 합니다.
- 실제 비밀값, `.env`, Terraform state, DB 비밀번호, AWS 키, JWT secret은 저장소에 커밋하지 않습니다.
- 작업 완료 시 가능한 경우 아래 순서로 진행합니다.

```bash
git status --short
git diff --check
git add <이번 작업 관련 파일>
git commit -m "<명확한 커밋 메시지>"
git push origin <현재 브랜치>
```

## 연동 저장소

백엔드:

```text
경로: C:\Users\hana0\workspace\Senior-Oral-Healthcare-api
GitHub: ThomastoneCoLTD/Senior-Oral-Healthcare-api
주요 브랜치: dev, prod
개발 배포: dev push
운영 배포: prod push
```

프론트엔드:

```text
현재 PC 경로: C:\Users\hana0\workspace\Senior-Oral-Healthcare-front
다른 PC: 작업 시작 전 경로 재확인 필수
GitHub: ThomastoneCoLTD/Senior-Oral-Healthcare-front
주요 브랜치: dev, prod
개발 배포: dev push
운영 배포: prod push
```

## 프로젝트 정보

- 서비스: SOH API
- 앱 디렉터리: `api_server`
- Framework: Spring Boot
- Build tool: Gradle Wrapper
- Java: 17
- Health path: `/api/actuator/health`
- AWS account: `160885266674`
- AWS region: `ap-northeast-2`
- Artifact bucket: `denti-backends`
- Artifact prefix: `soh`

## 브랜치 및 배포 정책

```text
main push -> 배포 없음
dev push  -> 개발 API 배포
prod push -> 운영 API 배포
```

`main`에서 배포되는 workflow를 만들지 않습니다.

개발 API:

```text
Workflow: .github/workflows/deploy-api-dev.yml
Branch: dev
Artifact path: s3://denti-backends/soh/dev/app.jar
Env path: s3://denti-backends/soh/dev/.env
Shared oral-exercise video path: s3://tms-static-hosting/oral-exercise/video/
Shared oral-exercise thumbnail path: s3://tms-static-hosting/oral-exercise/video-thumbnails/
ASG: soh-api-dev-asg
Secret: SOH_API_ENV_DEV
Health URL: https://soh-dev.thomabio.com/api/actuator/health
주의: dev는 단일 API 인스턴스라 instance refresh에서 MinHealthyPercentage 0을 사용합니다.
```

운영 API:

```text
Workflow: .github/workflows/deploy-api-prod.yml
Branch: prod
Artifact path: s3://denti-backends/soh/prod/app.jar
Env path: s3://denti-backends/soh/prod/.env
Shared oral-exercise video path: s3://tms-static-hosting/oral-exercise/video/
Shared oral-exercise thumbnail path: s3://tms-static-hosting/oral-exercise/video-thumbnails/
ASG: soh-api-prod-asg
Secret: SOH_API_ENV_PROD
Health URL: https://soh.thomabio.com/api/actuator/health
```

dev/prod S3 경로를 서로 바꾸지 않습니다.

## Secrets 및 환경변수

필수 GitHub Secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
SOH_API_ENV_DEV
SOH_API_ENV_PROD
SOH_TERRAFORM_TFVARS_DEV
SOH_TERRAFORM_TFVARS_PROD
```

- `SOH_API_ENV_DEV`, `SOH_API_ENV_PROD`에는 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`가 들어가야 합니다.
- dev 배포 workflow는 별도 Repository Secret `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`가 있으면 `SOH_API_ENV_DEV` 안의 같은 키를 배포 시 덮어씁니다. RDS 비밀번호만 급히 교체할 때는 해당 별도 Secret을 수정한 뒤 dev workflow를 재실행할 수 있습니다.
- DaeguChain 토큰 API는 `DAEGU_CHAIN_APP_KEY` 또는 `DAEGU_CHAIN_TOKEN`이 없으면 실패합니다. dev/prod 배포 workflow는 `DAEGU_CHAIN_APP_KEY_DEV`, `DAEGU_CHAIN_APP_KEY_PROD`, `DAEGU_CHAIN_TOKEN_DEV`, `DAEGU_CHAIN_TOKEN_PROD`, `TOKEN_SERVER_BASE_URL_DEV`, `TOKEN_SERVER_BASE_URL_PROD` 별도 Secret이 있으면 `.env`의 같은 키를 덮어씁니다.
- datasource 비밀번호는 RDS Secrets Manager 값에서 가져오고, 저장소 파일에 실제 값을 쓰지 않습니다.
- DaeguChain/DID 기능에는 `DAEGU_CHAIN_APP_KEY`, `DAEGU_CHAIN_ID`, `DID_SERVER_BASE_URL`, `DID_CREATE_PATH`, `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_TEMPLATE_ID`, `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_VALID_DAYS`, `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS`, `DAEGU_CHAIN_TOKEN_SYMBOL`, `DAEGU_CHAIN_TOKEN_DECIMALS`, `USER_REWARD_TOKEN_TRANSFER_ENABLED` 등을 환경별로 확인합니다.
- 개발 DID 생성 서버는 현재 `DID_SERVER_BASE_URL=http://43.201.125.82`를 사용합니다.
- DID 생성 경로 기본값은 `/did/create`이며 회원가입 DID 생성 요청은 `label`에 사용자 로그인 아이디를 넣어 호출합니다. 회원가입 시 DID 서버가 자체 생성한 DID를 내려주고, 지갑 주소는 DID 응답의 `walletAddress`, `wallet_address`, `accountAddress`, `account_address`, `address` 필드를 우선 사용합니다. DID 응답에 지갑 주소가 없으면 백엔드가 대구체인 계정 생성 API로 지갑 주소를 별도 생성해 저장합니다. 사용자가 입력한 지갑 주소나 DID 문자열 추정값으로 대체하지 않습니다. 로그인 credential은 DID 서버 `/did/issue-vc`에서 VC-JWT로 발급하고 로그인 시 `/did/verify-vc`로 검증합니다.
- reward reclaim은 사용자 DID private key를 SOH에서 읽거나 저장하지 않고, token server를 통해 `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS`로 회수합니다.

## Terraform 및 수동 구축

- Terraform 경로: `infra/terraform`
- Terraform apply는 Codex에서 직접 실행하지 않습니다. GitHub Actions 또는 사람이 검토 후 실행합니다.
- 수동 AWS 콘솔 구축 문서는 `readme_수동.md`를 확인합니다.
- CI/CD, Terraform, 브랜치 정책, AWS 상수, GitHub Secrets, S3 경로, ASG 이름, CloudFront/API 라우팅, 배포 명령이 바뀌면 `README.md`와 `AGENTS.md`를 함께 갱신합니다.

## 검증 명령

관련 변경 후 가능한 검증:

```bash
python -c "import glob, yaml; [yaml.safe_load(open(f, encoding='utf-8-sig')) for f in glob.glob('.github/workflows/*.yml')]; print('YAML OK')"
terraform fmt -recursive infra/terraform
cd infra/terraform/environments/dev && terraform init -backend=false && terraform validate
cd ../prod && terraform init -backend=false && terraform validate
cd api_server && ./gradlew clean bootJar -x test -x asciidoctor
```

Windows에서 Java 17을 명시해야 할 때:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean bootJar -x test -x asciidoctor
```

현재 작업 PC에서는 이전에 `JAVA_HOME`이 존재하지 않는 JDK 경로를 가리켜 Gradle이 실행 전 실패한 적이 있습니다. 빌드 전 Java 17 설치 경로를 확인합니다.

## 지금까지 진행한 주요 작업

- 개발 API datasource 설정을 `SPRING_DATASOURCE_*` 환경변수 기반으로 정리했습니다.
- 사용자 로그인은 비밀번호 검증이 아니라 DID 흐름으로 처리되도록 변경했습니다.
- 사용자 로그인 실패 메시지에서 “비밀번호 확인” 문구가 나오지 않고 DID 관련 오류로 나오도록 조정했습니다.
- API health check 경로 `/api/actuator/health`를 허용했습니다.
- dev 배포 workflow에서 단일 인스턴스 교체가 가능하도록 ASG instance refresh 설정을 보완했습니다.
- dev 배포 workflow에서 별도 datasource GitHub Secret 값이 있으면 `SOH_API_ENV_DEV`의 datasource 값을 덮어쓰도록 보완했습니다.
- 구강체조 콘텐츠 제목, 영상 URL, 실제 영상 길이를 초기 데이터에 반영했습니다.
- 구강체조 영상은 `s3://tms-static-hosting/oral-exercise/video/`, 썸네일은 `s3://tms-static-hosting/oral-exercise/video-thumbnails/` 아래에서 불러옵니다. 썸네일은 토큰명 기준 PNG 파일을 사용합니다. 예: `optional_video_1.png`, `essential_video_1.png`, `optional_video_7.png`.
- `s3://tms-static-hosting/oral-exercise/...` 형태로 저장된 구강체조 자산 URL은 API 응답에서 `https://tms-static-hosting.s3.ap-northeast-2.amazonaws.com/oral-exercise/...`로 변환합니다.
- TTS API(`/tts/speech`)는 AWS Polly `SynthesizeSpeech` 권한이 필요하며, 로그인 사용자만 호출하도록 둡니다.
- 회원가입(`/login/signUp`, `/login/signUp/did`) 시 토큰 수령용 `walletAddress`를 필수로 받아 `UserRewardWallet`에 함께 저장합니다.
- 회원가입 사용자는 요청 기관값과 무관하게 `tokenadmin` 관리자 계정의 소속 기관 사용자로 저장합니다. `tokenadmin` 계정 또는 소속 기관이 없으면 가입이 실패합니다.
- 구강체조 선택/상시영상은 처음부터 볼 수 있도록 `available`, `currentWeekContent`, `week` 응답 값을 조정했습니다.
- 구강체조 편성은 1화 인트로가 `optional_video_1`, 2~6화 필수영상이 `essential_video_1~5`, 7~12화 상시영상이 `optional_video_2~7`입니다.
- 2~6화 필수영상은 가입 주차에 따라 한 주에 하나씩 열리고, 1화 및 7~12화 상시영상은 계속 열려 있어야 합니다.
- 사용자 비밀번호 찾기/재설정 API(`/login/find-password`, `/login/password`)와 관련 DTO/서비스/문서 테스트를 제거했습니다.
- 구강체조 리워드 지급/회수 흐름을 token server 기반으로 정리했습니다.
- 필수 구강체조 토큰 발급은 영상 완료가 아니라 `/oral-exercise/rewards/button-click` 번호 버튼 성공으로만 처리합니다.
- 필수 구강체조 5개 토큰을 수령하고 리워드 회수/지급 처리까지 끝난 뒤에도 기존 `ORAL_EXERCISE_COIN` 이력을 유지해, 같은 영상을 다시 봐도 `essential_video_1~5` 토큰이 재발급되지 않도록 테스트로 고정했습니다.
- 치은염 검출 화면 및 구강검진 관련 프론트 문구 다국어 처리가 보강되었습니다.
- 프론트에서 리워드 지급 후 버튼이 다시 보이는 문제를 보완했습니다.

## 남은 확인 및 할 일

- dev 배포 후 `https://soh-dev.thomabio.com/api/actuator/health`가 `UP`인지 확인합니다.
- 프론트 dev 배포 후 1화 인트로와 7~12화 상시영상 전체가 잠금 없이 열리고, 2~6화 필수영상은 주차별로 열리는지 실제 화면에서 확인합니다.
- 7~12화의 backend 기본 길이는 실제 S3 MP4 메타데이터 기준으로 7화 176초, 8화 171초, 9화 163초, 10화 133초, 11화 172초, 12화 167초입니다.
- 연결된 영상 URL이 있는 콘텐츠는 `tms-static-hosting` 정적 S3 HTTPS URL이 정상 로드되는지 확인합니다.
- 치은염 검출이 실제 이미지 업로드/분석 결과까지 정상 동작하는지 end-to-end로 확인합니다.
- 비밀번호 변경 버튼은 프론트 사용자 화면에서 제거되어야 하며, 관리자 비밀번호 기능은 관리자 계정용으로 유지합니다.
- 기존에 채팅이나 과거 커밋에 노출된 DB 비밀번호가 있다면 RDS/Secrets Manager에서 회전하는 것을 권장합니다.
- 기존 `LOCAL_RECORDED` 상태의 구강체조 리워드 데이터를 실제 토큰 회수 대상으로 볼지 운영 정책을 결정합니다.

## 최근 동기화 상태

2026-07-10 기준으로 `git pull origin dev`를 수행했습니다.

- 백엔드 `Senior-Oral-Healthcare-api`: `a61b52b1`
- 프론트엔드 `Senior-Oral-Healthcare-front`: `e2dc94c`
