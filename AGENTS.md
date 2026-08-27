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
Health URL: https://api.soh.thomabio.com/api/actuator/health
VPC: soh-api-dev-vpc shared with development API
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
SOH_TERRAFORM_TFVARS_PROD_HCL
DAEGU_CHAIN_WALLET_ENCRYPTION_KEY_DEV
DAEGU_CHAIN_WALLET_ENCRYPTION_KEY_PROD
```

- `SOH_TERRAFORM_TFVARS_DEV`, `SOH_TERRAFORM_TFVARS_PROD_HCL`에는 각 환경의 `terraform.tfvars.example` 형식인 Terraform HCL만 넣습니다. 운영 workflow는 기존 앱 `.env` Secret과의 이름 충돌을 피하기 위해 `SOH_TERRAFORM_TFVARS_PROD_HCL`을 사용합니다. `SOH_API_ENV_*`의 `.env` 내용이나 DB 비밀번호/JWT secret/token/private key를 넣지 않습니다. Terraform apply workflow는 tfvars 각 줄을 사전 마스킹하고 대문자 앱 환경변수 또는 민감 변수명이 섞이면 `terraform init` 전에 차단합니다. 앱 환경변수 검사는 정상 Terraform 변수 `spring_profile`을 오인하지 않도록 대소문자를 구분해야 합니다.
- `SOH_API_ENV_DEV`, `SOH_API_ENV_PROD`에는 RDS 비밀번호를 넣지 않습니다. 특히 prod workflow는 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_DRIVER_CLASS_NAME`이 `SOH_API_ENV_PROD`에 있으면 배포를 차단합니다. EC2 launch template user-data만 Terraform의 `db_address`, `db_port`, `db_name`, `db_master_user_secret_arn` 값으로 datasource 설정을 생성합니다.
- `SOH_API_ENV_DEV`, `SOH_API_ENV_PROD`는 여러 줄 `KEY=VALUE` 텍스트로 저장합니다. 각 항목은 한 줄에 하나씩 들어가야 하며, 한 줄로 이어 붙이면 Spring이 `SERVER_PORT` 같은 값을 잘못 읽어 시작에 실패할 수 있습니다.
- JWT 서명키는 환경별로 서로 다른 32자 이상의 값을 사용합니다. 운영은 `JWT_ACCESS_KEY_PROD`, `JWT_REFRESH_KEY_PROD` 전용 GitHub Secrets를 생성하면 workflow가 기존 `SOH_API_ENV_PROD`를 교체하지 않고 생성된 `.env`의 `JWT_ACCESS_KEY`, `JWT_REFRESH_KEY`로 덮어씁니다. 저장소에는 JWT 기본키가 없으며 두 값이 없거나 짧으면 배포 workflow가 업로드 전에 차단합니다. 소스나 로그에 한 번이라도 노출된 JWT/AWS 키는 삭제만 하지 말고 해당 서비스에서 즉시 폐기·교체합니다.
- 배포 EC2는 AWS Secrets Manager JDBC driver(`com.amazonaws.secretsmanager.sql.AWSSecretsManagerMySQLDriver`)를 사용합니다. `SPRING_DATASOURCE_URL`은 `jdbc-secretsmanager:mysql://...`, `SPRING_DATASOURCE_USERNAME`은 RDS managed secret ARN, `SPRING_DATASOURCE_PASSWORD`는 빈 값으로 설정됩니다.
- `DAEGU_CHAIN_APP_KEY`와 `DAEGU_CHAIN_TOKEN`은 용도가 다릅니다. `TOKEN_SERVER_BASE_URL`의 지급·회수 프록시는 앱키를 사용하고, 계정 생성·DID·토큰 approve 같은 대구체인 `/mitum/...` 직접 API는 사용자 토큰을 사용합니다. dev/prod 배포 workflow는 두 값을 모두 요구하며 `DAEGU_CHAIN_APP_KEY_DEV`, `DAEGU_CHAIN_APP_KEY_PROD`, `DAEGU_CHAIN_TOKEN_DEV`, `DAEGU_CHAIN_TOKEN_PROD`, `TOKEN_SERVER_BASE_URL_DEV`, `TOKEN_SERVER_BASE_URL_PROD` 별도 Secret이 있으면 `.env`의 같은 키를 덮어씁니다.
- prod 배포 workflow는 운영 토큰 발행/전송 owner 변경을 위해 `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS_PROD`, `DAEGU_CHAIN_TOKEN_OWNER_PRIVATE_KEY_PROD` 별도 Secret이 있으면 `.env`의 `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS`, `DAEGU_CHAIN_TOKEN_OWNER_PRIVATE_KEY`를 덮어씁니다.
- 리워드 지갑 서명키는 `DAEGU_CHAIN_WALLET_ENCRYPTION_KEY`의 32바이트 Base64 AES 키로 AES-256-GCM 암호화해 `user_reward_wallet.wallet_private_key_ciphertext`에만 저장합니다. 운영 workflow는 `DAEGU_CHAIN_WALLET_ENCRYPTION_KEY_PROD` 전용 Secret을 덮어쓰며 값이 없으면 배포를 차단합니다. 이 키를 저장소, 공용 `.env` 문서, 프론트 코드에 넣거나 변경/분실하면 기존 지갑의 토큰 승인을 복호화할 수 없습니다.
- datasource 비밀번호는 앱이 EC2 instance profile 권한으로 RDS Secrets Manager에서 직접 가져옵니다. EC2 IAM role에는 해당 secret에 대한 `secretsmanager:DescribeSecret`, `secretsmanager:GetSecretValue` 권한이 필요합니다.
- DaeguChain/DID 기능에는 `DAEGU_CHAIN_APP_KEY`, `DAEGU_CHAIN_ID`, `DID_SERVER_BASE_URL`, `DID_CREATE_PATH`, `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS`, `DAEGU_CHAIN_TOKEN_SYMBOL`, `DAEGU_CHAIN_TOKEN_DECIMALS`, `USER_REWARD_TOKEN_TRANSFER_ENABLED` 등을 환경별로 확인합니다.
- 모바일·태블릿 다대구 로그인은 `DADAEGU_LOGIN_ENABLED=true`, 발급받은 `DADAEGU_LOGIN_SITE_ID`, PKCS#8 Base64/PEM 형식의 `DADAEGU_LOGIN_RSA_PRIVATE_KEY`, 선택값 `DADAEGU_LOGIN_REQUIRED_VC`(기본 `DaeguMasterVC`)가 모두 필요합니다. RSA 개인키는 백엔드 `.env` Secret에만 두고 Vite 환경변수나 프론트 코드에 넣지 않습니다. 공개 `/login/dadaegu/config`는 준비 여부·site ID·required VC만 반환하며, `/login/dadaegu` 요청의 암호화 콜백 원문은 시스템 로그에서 마스킹합니다.
- 운영 다대구 로그인 값은 공용 `SOH_API_ENV_PROD` 전체를 교체하지 않고 `DADAEGU_LOGIN_ENABLED_PROD`, `DADAEGU_LOGIN_SITE_ID_PROD`, `DADAEGU_LOGIN_RSA_PRIVATE_KEY_PROD`, 선택값 `DADAEGU_LOGIN_REQUIRED_VC_PROD` 전용 Secret으로 덮어씁니다. enabled 값이 `true`이면 site ID와 RSA private key가 모두 존재해야 배포됩니다.
- 다대구 로그인은 외부 DID를 `dadaegu_user_identity`에 SOH 사용자와 별도로 매핑하고 `DaeguMasterVC`의 CI는 원문이 아닌 SHA-256 해시만 저장합니다. 기존 외부 DID/CI 매핑을 우선 사용하고, 최초 연결에 한해 이름·정규화 휴대폰 번호·생년월일이 완전히 일치하는 기존 사용자를 연결합니다. 신규 사용자는 `dadaegu_signup_session`의 10분 유효 일회용 온보딩 토큰으로 `POST /login/dadaegu/signUp`을 호출합니다. 최초 가입 화면은 `realOrganization`, 모든 필수 약관 동의, 구강분석 서비스 신청 여부를 받으며, 계정·내부 DID·리워드 지갑·동의·외부 DID/CI 매핑이 모두 성공해야 로그인 토큰을 발급합니다. 원문 온보딩 토큰은 DB에 저장하지 않고 SHA-256 해시만 저장합니다.
- 다대구 DID/CI 매핑이 삭제되었거나 존재하지 않는 SOH 사용자를 가리키면 고아 매핑을 제거한 뒤 기존 인적정보 일치 계정 연결 또는 신규 온보딩을 계속합니다. 활성 상태로 복구된 기존 사용자는 저장된 약관·구강분석 서비스 신청 상태를 그대로 유지해 정상 로그인하고, 관리자 사용자 삭제 시에는 이후 신규 가입이 막히지 않도록 다대구 매핑도 같은 트랜잭션에서 제거합니다.
- 다대구 최초 가입 화면은 필수 약관 동의와 함께 구강분석 서비스 신청 여부를 받고 `user.oral_analysis_service_enabled`에 저장합니다. 이 값은 `/user/info` 조회·수정 및 로그인 응답에 포함되며, 기존 `null` 사용자는 미신청으로 해석합니다. 사용자가 구강분석 서비스를 신청한 경우에만 PLAQUE/PERIODONTAL 서비스 목록을 응답하고, 미신청이면 빈 목록을 반환합니다.
- 다대구 성별은 추가 입력 없이 VC 값을 백엔드 `M`/`W`로 정규화합니다. 영문 `M/F/W`, `MALE/FEMALE`, `MAN/WOMAN`, 한글 `남/남성/남자`, `여/여성/여자`, 주민번호 성별 코드 `1~8`, 따옴표·공백·괄호가 포함된 표현을 지원합니다.
- 개발 DID/token 서버는 현재 `DID_SERVER_BASE_URL=http://43.201.125.82`, `TOKEN_SERVER_BASE_URL=http://43.201.125.82`를 사용합니다. 배포 API에서 `TOKEN_SERVER_BASE_URL`이 `http://localhost:5000`이면 EC2 자기 자신을 호출해 token list/create/transfer가 connection refused로 실패합니다.
- DID 생성 경로 기본값은 `/did/create`이며 회원가입 DID 생성 요청은 `label`에 사용자 로그인 아이디를 넣어 호출합니다. 회원가입 시 DID 서버가 자체 생성한 DID를 내려주고, 지갑 주소는 DID 응답의 `walletAddress`, `wallet_address`, `accountAddress`, `account_address`, `address` 필드를 우선 사용합니다. DID 응답에 지갑 주소가 없으면 백엔드가 대구체인 계정 생성 API로 지갑 주소를 별도 생성해 저장합니다. 사용자가 입력한 지갑 주소나 DID 문자열 추정값으로 대체하지 않습니다. 아이디만 입력하는 기존 `POST /login/did`는 제거했으며 외부 DID 로그인은 다대구 `/login/dadaegu`만 사용합니다.
- 일반 비밀번호 회원가입과 DID/다대구 회원가입은 모두 DID 연결·리워드 지갑 생성이 완료되어야 성공합니다. 외부 프로비저닝 실패를 `null`로 삼켜 불완전 계정을 남기지 않습니다. 로컬 회원가입은 SOH DID 서버 응답에서 DID와 공개키만 사용하고, DID 응답의 주소·private key를 리워드 지갑에 재사용하지 않습니다. 가입 경로와 무관하게 리워드 지갑은 `/mitum/com/acc_create`의 주소·private key 한 쌍으로 별도 생성하고 `/mitum/com/acc_faucet`으로 체인 계정을 활성화합니다. 기존 지갑 주소와 암호화된 대구체인 서명키가 모두 있으면 재사용하며, 서명키가 없거나 과거 Ed25519 DID private key 형식이면 새 주소·키로 멱등성 보정합니다. 토큰 balance state가 생기기 전 approve는 `P06D504`로 실패하므로 로그인에서는 승인하지 않고, 실제 토큰 지급 성공 직후 해당 contract만 승인하며 회수 직전에도 재확인합니다.
- reward reclaim은 사용자 DID private key를 SOH에서 읽거나 저장하지 않고, token server의 `TOKEN_RECLAIM_PATH`(기본 `/token/retrieve`)로 요청합니다. 저장된 리워드 지갑 주소를 `holder`, 운영 owner 주소를 `sender`·`receiver`로 사용하고 운영 owner private key만 백엔드 Secret에서 읽습니다. 다대구 외부 DID는 회수 요청의 `user_DID`로 보내지 않으며 owner private key는 API 감사 로그에서 마스킹합니다.
- SOH가 대구체인 raw 리워드 지갑을 생성할 때는 응답의 지갑 private key를 AES-256-GCM으로 암호화해 저장하고 평문은 즉시 폐기합니다. 기존 DID 서버가 반환한 64자리 raw hex/`0x` hex 키는 Ed25519 DID 키이므로 `/mitum/token/approve` 지갑키로 변환하거나 재사용하지 않고 새 대구체인 지갑을 생성합니다. `holder_pkey`는 API 감사 로그에서 마스킹합니다. 슈퍼 관리자는 사용자 관리 화면에서 정확한 대상 로그인 ID를 재입력한 경우에만 구강체조 진도·리워드 거래·리워드 지갑을 초기화할 수 있습니다. 계정·기본 인적정보·기관·다대구 연결은 유지되며 다음 로그인 또는 지갑 조회에서 승인 가능한 새 지갑을 생성합니다.

## Terraform 및 수동 구축

- Terraform 경로: `infra/terraform`
- Terraform apply는 Codex에서 직접 실행하지 않습니다. GitHub Actions 또는 사람이 검토 후 실행합니다.
- 운영 API는 별도 prod VPC를 만들지 않고 개발 API VPC(`soh-api-dev-vpc`)와 dev public/private app/private DB subnet을 재사용합니다.
- 운영 API 도메인은 `https://api.soh.thomabio.com`이며, 프론트 production `VITE_API_BASE_URL`은 `https://api.soh.thomabio.com/api`입니다.
- 운영 RDS `soh-api-prod-mysql`은 MySQL `8.4.10`이며 Terraform 입력은 `db_engine_version = "8.4.10"`으로 유지합니다. `8.4`처럼 patch를 생략하면 AWS가 더 낮은 patch 버전으로 해석해 `8.4.10 -> 8.4.9` 다운그레이드를 시도할 수 있습니다. prod apply workflow는 다른 버전을 차단합니다.
- 운영 Terraform tfvars는 `create_route53_record = true`로 유지합니다. `false`로 두면 `api.soh.thomabio.com` Route 53 alias record가 삭제됩니다.
- 배포 workflow는 기존 ASG Instance Refresh가 진행 중이면 완료될 때까지 기다린 뒤 새 refresh를 시작합니다. dev는 단일 인스턴스 복구를 위해 `MinHealthyPercentage=0`, prod는 `MinHealthyPercentage=100`을 사용합니다.
- 운영 배포 workflow는 이미 진행 중인 운영 배포를 취소하지 않고 순차 실행되도록 `cancel-in-progress=false`를 유지합니다.
- 운영 launch template은 Terraform이 관리하는 최신 버전을 기본 버전으로 승격합니다. prod 배포 workflow는 ASG가 그 기본 버전을 명시적으로 사용하고 User Data에 Secrets Manager JDBC 설정이 있는지 확인한 뒤에만 S3 업로드와 Instance Refresh를 진행합니다.
- 기존 prod VPC가 Terraform state에 있던 경우 apply 전에 plan에서 VPC/subnet/NAT 삭제가 뜨는지 확인하고, 의도하지 않은 destroy plan은 승인하지 않습니다.
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

- 개발/운영 API datasource 설정을 AWS Secrets Manager JDBC driver 기반으로 정리해 RDS 비밀번호 변경 시 GitHub Secret 수동 갱신이 필요 없도록 했습니다.
- 2026-08-12 운영 RDS의 7일 자동 비밀번호 회전으로 정적 S3 `.env`와 실제 Secret이 불일치해 ASG 교체가 반복된 원인을 확인했습니다. `SOH_API_ENV_PROD`의 datasource 키를 금지하고, launch template 기본 버전과 Secrets Manager JDBC 설정을 배포 전에 검증하도록 보강했습니다.
- 로그인 화면은 일반 사용자, 다대구, 관리자 3개 흐름으로 분리합니다. 일반 사용자 `POST /login`은 아이디와 BCrypt 비밀번호를 검증하며 DID 발급 상태를 요구하지 않습니다. 아이디만 입력하는 기존 `POST /login/did`는 제거했고 다대구 로그인은 `POST /login/dadaegu`를 사용합니다. 관리자 로그인 흐름은 기존대로 유지합니다.
- 일반 사용자 회원가입 `POST /login/signUp`은 비밀번호, 생년월일, 기존 비밀번호 찾기 질문/답변을 저장하면서 기존 DID·지갑 프로비저닝도 함께 수행합니다. 프론트에서는 비밀번호 확인 일치와 아이디 중복 확인을 완료해야 제출합니다.
- 가입 경로별 DID를 분리합니다. 로컬 회원가입(`/login/signUp`, 기존 `/login/signUp/did`)은 SOH DID 서버에서 자체 DID를 생성하고, 다대구 최초 가입(`/login/dadaegu/signUp`)은 자체 DID를 추가 생성하지 않고 다대구 인증 결과의 DID를 리워드 지갑 활성 DID로 저장합니다. 리워드 지갑은 두 가입 경로 모두 DID 키와 분리해 대구체인 `/mitum/com/acc_create`로 생성합니다. 기존 로컬 사용자가 다대구 로그인하면 자체 DID와 서명키가 있는 기존 지갑 주소는 보존하고 리워드 지갑의 활성 DID만 다대구 DID로 전환합니다. 토큰 지급은 DID의 발급처와 무관하게 저장된 지갑 주소를 `/token/transfer`의 `receiver`로 직접 전송합니다. 다대구 DID를 `user_DID`로 보내면 토큰 서버의 로컬 DID DB 조회에서 `user_DID not found`로 체인 전송 전에 실패하므로 지급 요청에는 넣지 않습니다.
- 일반/DID 회원가입은 성별(`M`/`W`)을 필수로 저장합니다. 기존 DID 회원가입 `POST /login/signUp/did`은 호환 목적으로 유지하지만 아이디 전용 로그인은 제공하지 않습니다. 일반 회원가입은 비밀번호 및 비밀번호 찾기 질문/답변을 저장합니다.
- API health check 경로 `/api/actuator/health`를 허용했습니다.
- dev 배포 workflow에서 단일 인스턴스 교체가 가능하도록 ASG instance refresh 설정을 보완했습니다.
- 구강체조 콘텐츠 제목, 영상 URL, 실제 영상 길이를 초기 데이터에 반영했습니다.
- 구강체조 영상은 `s3://tms-static-hosting/oral-exercise/video/`, 썸네일은 `s3://tms-static-hosting/oral-exercise/video-thumbnails/` 아래에서 불러옵니다. 썸네일은 토큰명 기준 PNG 파일을 사용합니다. 예: `optional_video_1.png`, `essential_video_1.png`, `optional_video_7.png`.
- `s3://tms-static-hosting/oral-exercise/...` 형태로 저장된 구강체조 자산 URL은 API 응답에서 `https://tms-static-hosting.s3.ap-northeast-2.amazonaws.com/oral-exercise/...`로 변환합니다.
- TTS API(`/tts/speech`)는 AWS Polly `SynthesizeSpeech` 권한이 필요하며, 로그인 사용자만 호출하도록 둡니다. 구강체조 회차별 시작·완료 안내는 프론트에서 `s3://tms-static-hosting/oral-exercise/tts/구강_운동_AI_MP3_본문_32개/`의 CSV 파일명 기준 고정 MP3를 재생하고, S3 음원 실패 시에만 이 API로 CSV 문장을 합성합니다. 영상 중 토큰 번호 안내는 항상 이 API를 사용합니다. 시작 안내는 영상을 열 때 한 번만 재생하며, 토큰 번호 안내 중에는 영상과 버튼 제한시간을 모두 멈추고, 토큰 수령 뒤에는 시작 안내 없이 영상을 이어서 재생합니다.
- 회원가입(`/login/signUp`, `/login/signUp/did`) 시 토큰 수령용 `walletAddress`를 필수로 받아 `UserRewardWallet`에 함께 저장합니다.
- 회원가입 사용자는 요청 기관값과 무관하게 `tokenadmin` 관리자 계정의 소속 기관 사용자로 저장합니다. `tokenadmin` 계정 또는 소속 기관이 없으면 가입이 실패합니다.
- 회원가입(`/login/signUp`, `/login/signUp/did`)은 사용자가 `대구1`, `대구2`, `대구3` 중 하나를 `realOrganization`으로 필수 선택합니다. 선택값은 기존 `organizationId` 소속 관계와 분리된 `user.real_organization` 컬럼에 저장하며, 사용자 `/user/info`와 관리자 `/admin/user` 목록에서 조회합니다. 기존 사용자 및 관리자 일괄등록 호환을 위해 DB 컬럼 자체는 nullable입니다.
- 일반 관리자 메뉴는 `[사용자 관리]`, `[사용자 통계]`, `[사용자 진도 현황]`, `[DID 리워드 현황]`만 표시하고 슈퍼 관리자 메뉴는 기존 구성을 유지합니다. 슈퍼 관리자의 사용자 관리·통계·진도·DID 리워드 화면은 `organizationId`가 아니라 회원가입 시 선택한 `realOrganization` 기준으로 묶으며, 값이 없는 기존 사용자는 `기관 미지정`으로 표시합니다. 진도 및 DID 리워드 응답에도 `realOrganization`을 포함합니다.
- 기존 `user.find_pwd_question_id`, `user.find_pwd_answer` 컬럼은 비밀번호 찾기 질문과 답변 용도로 유지합니다. `POST /login/find-id`는 질문/답변을 받지 않고 이름, 정규화된 휴대폰 번호, 생년월일이 모두 일치할 때만 사용자 아이디를 반환합니다. 질문 목록은 `/password/questions`에서 조회합니다.
- 운영/개발 앱 시작 시 과거 계정 확인 질문 9개를 ID와 정렬값 `1~9`로 upsert합니다. 신규 DB에서 `find_pwd_question`이 비어 있어 회원가입 질문을 선택할 수 없는 상태를 방지하고 기존 `user.find_pwd_question_id` 참조 의미를 유지합니다.
- 구강체조는 1화 인트로만 처음부터 볼 수 있습니다. 인트로 완료 전에는 필수·상시영상 모두 `available=false`, `videoUrl=null`이며 `/oral-exercise/interactions` 직접 호출도 차단합니다.
- 구강체조 편성은 1화 인트로가 `optional_video_1`, 2~6화 필수영상이 `essential_video_1~5`, 7~12화 상시영상이 `optional_video_2~7`입니다.
- 구강체조 표시 편성은 1화 `Intro`, 2~6화 `Chapter 1~5`, 7~12화 `Chapter 7~12`입니다. 토큰 현황의 필수 슬롯은 `1주 차~5주 차 / Chapter 1~5`, 상시 슬롯은 `1회 차 / Intro`, `2~7회 차 / Chapter 7~12`로 표시하며 내부 `contentSort`와 토큰명 매핑은 변경하지 않습니다.
- 인트로를 완료하면 2화 필수영상과 7~12화 상시영상이 열립니다. 3~6화 필수영상은 바로 이전 필수영상을 시청 완료하면 가입 주차를 기다리지 않고 즉시 열립니다. 잠긴 영상은 `/oral-exercise` 응답에서 `available=false`, `videoUrl=null`이며 `/oral-exercise/interactions` 직접 호출도 같은 선행 완료 조건으로 차단합니다.
- 사용자 로그인 화면은 아이디 찾기와 비밀번호 찾기를 모두 제공합니다. 아이디 찾기는 이름·정규화된 휴대폰 번호·생년월일을 확인하고, 비밀번호 찾기는 아이디·가입 시 질문·답변 확인 후 10분 유효한 일회용 토큰을 발급해 새 비밀번호를 설정합니다. 토큰 원문은 저장하지 않고 SHA-256 해시만 `password_reset_token`에 저장하며 사용·만료 토큰은 재사용할 수 없습니다.
- 구강체조 리워드 지급/회수 흐름을 token server 기반으로 정리했습니다.
- 필수 구강체조 토큰 발급은 영상 완료가 아니라 `/oral-exercise/rewards/button-click` 번호 버튼 성공으로만 처리합니다.
- 필수 구강체조 5개 토큰을 수령하고 리워드 회수/지급 처리까지 끝난 뒤에도 기존 `ORAL_EXERCISE_COIN` 이력을 유지해, 같은 영상을 다시 봐도 `essential_video_1~5` 토큰이 재발급되지 않도록 테스트로 고정했습니다.
- 사용자 리워드 여정은 `COLLECTING`, `READY`, `PARTIAL_RECLAIM`, `COMPLETED` 상태로 계산합니다. 필수 토큰 5개를 한 번 이상 수령했고 현재까지 받은 필수·상시 토큰이 모두 회수된 경우에만 완료이며, 완료 후에는 `/oral-exercise`의 신규 번호 토큰 버튼과 직접 지급 API를 모두 차단합니다. `/user/rewards/transactions`는 거래 이력과 함께 필수 수령·회수 수, 미회수 수, 회수 가능 여부, 완료 여부를 반환하므로 프론트는 이를 상태 원본으로 사용합니다.
- 치은염 검출 화면 및 구강검진 관련 프론트 문구 다국어 처리가 보강되었습니다.
- 프론트에서 리워드 지급 후 버튼이 다시 보이는 문제를 보완했습니다.
- 관리자 페이지에 기관별 사용자 구강체조 영상 진도 및 필수 영상 토큰 수령 현황 조회 탭을 추가했습니다. 관리자 API는 `/admin/user/exercise-progress`를 사용합니다.
- 관리자 페이지에 사용자 DID 계정 발급, 리워드 지갑 생성, 로그인 이력, 필수 입 체조 5개 영상 리워드 지급, 리워드 회수 내역을 한 번에 보는 DID 리워드 현황 탭을 추가했습니다. 관리자 API는 `/admin/user/daegu-reward-status`를 사용합니다.
- 관리자 페이지에 대구체인 기능 사용 로그(사용 기능, 사용자 아이디, 사용일시) 조회 화면을 추가했습니다. 관리자 API는 `/admin/user/daegu-chain-usage-logs`를 사용하며 사용자 로그인, DID 발급, 구강체조 리워드 지급·회수 이력을 통합해 보여줍니다.
- 대구체인 API 신규 호출은 `daegu_chain_api_log` 테이블에 API, 마스킹된 Request/Response, 성공 여부를 저장합니다. `token`, 앱키, private key, pkey, JWT, 비밀번호·secret 계열 필드는 실제 값이 로그에 남지 않아야 합니다. 외부 API가 오류 메시지 본문에 요청의 민감값을 되돌려 보내는 경우에도 저장·전파 전에 요청값 기준으로 다시 마스킹합니다. 배포 전 과거 통합 로그에는 API/Request/Response가 없을 수 있습니다.
- 사용자 로그인 성공 시마다 `user_login_history` 테이블에 이력이 기록됩니다. 배포 이전 과거 로그인 이력은 소급 생성되지 않으며 기존 `userLastLoginDate`는 최근 로그인 일시로 함께 조회됩니다.
- 사용자 구강체조 인트로 영상(`optional_video_1`, 1화)은 영상 내 번호 버튼 성공 시 토큰 수령 대상입니다. 단, 사용자 화면의 리워드 슬롯과 자동 리워드 회수 조건은 기존처럼 필수영상 5개(`essential_video_1~5`)만 반영합니다.
- 구강체조 리워드 토큰 전송 실패 시에도 `TOKEN_TRANSFER_FAILED` 트랜잭션 이력을 남겨 관리자/사용자 조회 및 다음 동일 리워드 요청의 재시도 판단에 사용합니다.
- Windows/CI에서 한글 REST Docs 테스트 결과가 달라지지 않도록 Gradle 테스트 JVM과 Java 컴파일 인코딩을 UTF-8로 고정했습니다. `DentixApplicationTest`는 운영 DB 비밀값 없이 H2 인메모리 DB로 실행하며, 구강검진 결과·타임라인의 분석 유형 및 치은염 응답 필드를 REST Docs에 반영했습니다.
- 문진표 AI 요청은 외부 API 계약에 맞춰 `application/x-www-form-urlencoded`의 `survey` JSON으로 전송합니다. AI 5xx·연결 실패·비정상 응답 시에는 문진 답변에서 A~K 관리 유형을 결정하는 `QuestionnaireFallbackAnalyzer rules-v1`으로 대체하여 운영에서도 제출과 결과 저장을 계속합니다. 프론트는 선택하지 않은 선택 문항도 빈 배열로 제출하고 문항별 최대 선택 수를 제한합니다. 이 변경에는 별도 DB 테이블이 필요하지 않습니다.
- SOH 문진표는 기관 구독 플랜과 무관하게 모든 로그인 사용자가 이용합니다. 문진 제출 시 `organization_subscription` 또는 구독 플랜의 존재 여부를 요구하지 않으며, 사용자 식별을 위한 로그인 인증은 유지합니다.
- 2026-08-18 Denti-K `MIDSIZE` 사용자 구강분석 흐름을 SOH 구조에 맞춰 이식했습니다. 로그인한 SOH 사용자는 구독 플랜과 무관하게 치태·치은염 분석, 설문 기반 및 구강분석 기반 맞춤 콘텐츠를 이용합니다. Denti-K 호환 API는 `POST /gingivitis-analyses`, `GET /gingivitis-analyses/{analysisId}`, `GET /gingivitis-condition`이며 기존 `/oralCheck/gingivitis`도 유지합니다.
- 치태 AI 응답의 `contents_type`, `plaque_contents`를 보존하고 직접 콘텐츠 ID, `content_curation_rule`, 기존 `user_oral_status`·`oral_status_to_contents` 순서로 추천을 계산합니다. 2026-08-18 `content_curation_rule` 테이블 추가 완료 안내를 받았고 동일 엔티티·Repository·Service를 SOH에 이식했습니다. 규칙의 `contents_id`는 SOH 콘텐츠 ID여야 하며, 일치하는 활성 행이 없으면 분석 완료 상태에서도 맞춤 콘텐츠 목록이 비어 있을 수 있습니다. DB schema 외 Secret, AWS, 배포 workflow는 변경하지 않습니다.
- 사용자 콘텐츠 메뉴는 구독 플랜과 무관하게 맞춤 콘텐츠 화면으로 진입하고 화면 내부에서 `맞춤 콘텐츠 / 모든 콘텐츠`를 전환합니다. 구강상태 기록은 플라그 분석을 초록색 치아 아이콘, 치은염 분석을 붉은색 심박 아이콘으로 구분하고 각각의 검출 결과 문구를 표시합니다.

## 남은 확인 및 할 일

- dev 배포 후 `https://soh-dev.thomabio.com/api/actuator/health`가 `UP`인지 확인합니다.
- 프론트 dev 배포 후 인트로 미완료 상태에서는 다른 영상이 모두 잠기고, 인트로 완료 직후 2화 필수영상과 7~12화 상시영상이 열리며, 이후 3~6화 필수영상은 이전 필수영상 완료 직후 열리는지 실제 화면에서 확인합니다.
- 변경된 S3 MP4 기준 backend 기본 길이는 1화 113초, 2화 211초, 5화 426초, 8화 168초, 10화 130초, 11화 171초입니다. 나머지는 3화 176초, 4화 172초, 6화 232초, 7화 176초, 9화 163초, 12화 167초입니다.
- 연결된 영상 URL이 있는 콘텐츠는 `tms-static-hosting` 정적 S3 HTTPS URL이 정상 로드되는지 확인합니다.
- 치은염 검출이 실제 이미지 업로드/분석 결과까지 정상 동작하는지 end-to-end로 확인합니다.
- 운영 배포 전 `content_curation_rule`의 S/G/A/D·A~K·치태 등급별 활성 행과 SOH `contents_id` 참조를 확인하고, 실제 사용자 계정으로 치태·치은염 업로드, 결과 소유권 차단, 결과별 맞춤 콘텐츠 순서를 확인합니다.
- 비밀번호 변경 버튼은 프론트 사용자 화면에서 제거되어야 하며, 관리자 비밀번호 기능은 관리자 계정용으로 유지합니다.
- 과거 정적 DB 비밀번호가 남은 S3 객체 version과 GitHub Actions 로그가 보존 정책에 따라 언제 삭제되는지 확인합니다. 현재 Secret은 RDS 관리형 회전 대상이며 실제 값은 문서나 저장소에 기록하지 않습니다.
- 기존 `LOCAL_RECORDED` 상태의 구강체조 리워드 데이터를 실제 토큰 회수 대상으로 볼지 운영 정책을 결정합니다.
- 운영 환경에서 과거 DID·지갑 미완성 로컬 가입 계정으로 리워드 버튼을 눌러 DID·지갑 자동 복구와 실제 토큰 전송이 성공하는지 확인합니다.
- 2026-08-25 이전 대구체인 승인 실패 감사 로그의 `response_payload`에 외부 API가 되돌려준 `holder_pkey` 원문이 남은 행이 있는지 운영 DB에서 확인하고, 발견 시 해당 지갑키를 노출된 키로 간주해 지갑 교체·잔액 이전 가능성을 검토한 뒤 보존 정책에 따라 과거 로그를 정리합니다. 실제 키는 문서나 작업 로그에 복사하지 않습니다.

## 최근 동기화 상태

2026-08-27 OWASP 보안 기준에 맞춰 인증·인가 및 의존성을 보강했습니다.

- refresh token은 로그인·다대구 자동가입·자동 로그인 성공 시 `SOH_REFRESH_TOKEN`이라는 `HttpOnly`, `Secure`, `SameSite=Lax` 쿠키로 발급하고 JSON 응답에서는 제외합니다. access token만 응답하며 프론트는 메모리에만 유지합니다. `/login/access-token`은 쿠키를 우선 사용하되 기존 `RefreshToken` 헤더도 호환하며, `/auth/session`은 로그인 역할·슈퍼관리자 여부와 최소 프로필을 반환하고 `/auth/logout`은 DB refresh token과 쿠키를 함께 폐기합니다.
- 인증 쿠키 설정은 `AUTH_REFRESH_COOKIE_NAME`, `AUTH_COOKIE_PATH`, `AUTH_COOKIE_SECURE`, `AUTH_COOKIE_SAME_SITE`로 재정의할 수 있습니다. 기본 경로는 `/api`, Secure는 `true`, SameSite는 `Lax`이며 로컬 HTTP 테스트에서만 `AUTH_COOKIE_SECURE=false`를 명시합니다.
- `/superadmin/**` API는 `ROLE_SUPER_ADMIN`만 접근할 수 있으며 `/admin/**` 일반 관리자 규칙보다 먼저 평가합니다.
- 관리자 가입 `POST /admin/account`만 공개 상태를 유지하고 관리자 계정 목록·삭제·비밀번호 초기화는 슈퍼 관리자만, 청구서 엑셀·사용자 일괄등록 양식은 로그인한 관리자만 접근하도록 보안 체인을 정리했습니다.
- 컨트롤러별 전체 허용 CORS를 제거하고 `CORS_ALLOWED_ORIGINS`의 명시적 origin 및 필요한 요청 헤더만 허용합니다.
- Authorization은 RFC 6750 `Bearer <token>` 형식만 허용하고 요청 파싱·검증 예외의 내부 메시지 및 오류 로그 상세가 응답·DB 로그에 노출되지 않도록 정리했습니다.
- Spring Boot 3.5.16, JJWT 0.13.0, Apache POI 5.5.1, org.json 20260814, Commons Codec 1.22.1로 갱신하고 사용하지 않는 지원 종료 AWS SDK for Java v1 의존성을 제거했습니다.
- JWT 서명키의 저장소 기본값을 제거하고 `JWT_ACCESS_KEY`, `JWT_REFRESH_KEY`를 배포 필수값으로 검사합니다. 과거 이력에 존재했던 AWS/JWT 형식 키는 별도 폐기·교체 대상입니다.

2026-08-26 양치질 기록 기능을 폐기했습니다.

- `/user/brushing`, `/toothBrushing` API와 `domain/toothBrushing` 구현을 제거하고, 구강상태 타임라인·대시보드 응답에서 양치 기록 및 통계 필드를 제거했습니다.
- 앱 시작 시 `DROP TABLE IF EXISTS tooth_brushing`을 실행해 기존 양치 기록 테이블과 데이터를 삭제합니다. 다른 사용자·문진·구강분석·리워드 테이블은 삭제하지 않습니다.
- 문진표의 양치 습관 문항, 분석 규칙, 양치 교육 콘텐츠는 독립적인 서비스 데이터이므로 유지합니다.

2026-08-25 로컬 가입 DID 키와 리워드 지갑 서명키를 분리했습니다.

- 로컬 가입은 SOH DID 서버의 DID·공개키만 저장하고 리워드 지갑은 다대구 가입과 동일하게 `/mitum/com/acc_create`와 `acc_faucet`으로 별도 생성합니다.
- 기존 로컬 지갑의 64자리 raw hex/`0x` hex 키는 접두사 문제가 아니라 Ed25519 DID 키이므로 비호환 지갑으로 판정해 새 `/mitum/com/acc_create` 지갑으로 교체합니다. 이미 구 지갑으로 토큰 지급이 완료된 테스트 사용자는 자동 중복 지급하지 않으며 슈퍼 관리자 테스트 데이터 초기화 후 새 지갑으로 다시 수령해야 합니다. 비호환 지갑의 회수 요청은 외부 approve를 호출하지 않고 초기화·재지급 안내 오류로 차단합니다.
- 대구체인 오류 응답이 `holder_pkey` 등 요청 민감값을 메시지 안에 되돌려 보내더라도 API 예외와 감사 로그에 원문이 남지 않도록 재마스킹합니다.

2026-08-24 신규 리워드 지갑의 체인 활성화 순서를 보강했습니다.

- `/mitum/com/acc_create` 응답 주소는 아직 체인 계정이 아니므로 `acc_faucet`으로 활성화합니다. 활성화 후에도 특정 토큰을 받은 적이 없으면 contract token balance state가 없어 approve가 `B0593/P06D504`로 실패합니다.
- 로컬 가입, 다대구 가입, 과거 누락 지갑 자동복구는 로그인 중 토큰 approve를 하지 않습니다. 지갑 서명키를 AES-256-GCM으로 암호화해 저장하고, 실제 토큰 지급으로 contract balance state가 생긴 직후 해당 contract를 승인합니다. 회수 직전에도 같은 승인을 재확인한 뒤 `transfer_from`을 호출합니다.

2026-08-24 다대구 로그인 중 대구체인 API 실패 로그 보존을 보강했습니다.

- `user_login_history`는 로그인 성공 이력만 저장하므로 지갑 생성·회수 권한 승인 단계에서 실패한 요청은 로그인 이력에 남지 않습니다.
- 실패한 대구체인 API 감사 로그는 바깥 로그인 트랜잭션이 롤백되어도 보존되도록 `REQUIRES_NEW` 트랜잭션으로 저장합니다. HTTP 오류뿐 아니라 HTTP 200의 `state=OOPS/ERROR/FAIL` 승인 응답도 state, msg, rcode를 예외 메시지에 포함해 원인을 확인할 수 있게 했습니다.

2026-08-24 다대구 로그인 계정의 리워드 토큰 회수를 외부 DID 조회와 분리했습니다.

- 회수는 `/token/transfer`에 `user_DID`를 보내지 않고 회수 전용 `/token/retrieve`(`transfer_from`)를 사용합니다. 가입 경로와 무관하게 저장된 지갑 주소를 `holder`로 보내므로 토큰 서버 로컬 DID DB에 없는 다대구 `did:key`도 회수 흐름에 영향을 주지 않습니다.
- 운영 owner 주소·private key를 각각 `sender`·`sender_pkey`로 사용하고 owner 주소로 회수하며, private key는 기존 감사 로그 민감 필드 마스킹 대상입니다. 토큰 서버의 HTTP 200 `state=OOPS` 응답도 실패로 처리합니다.
- `ExternalTokenClientTest`, `UserRewardReclaimServiceTest`를 통과했습니다.
- 체인 `transfer_from`이 요구하는 사전 allowance를 새 raw 지갑 생성 시 토큰별 `approve`로 설정하도록 보강했습니다. 사용자 지갑 private key는 저장하지 않고 승인 호출 중에만 사용하며 `holder_pkey` 감사 로그를 마스킹합니다.
- 운영 다대구 재로그인 중 `B0701 유효하지 않은 사용자 토큰`이 발생한 원인은 직접 `/mitum/...` API에도 앱키를 우선 넣던 인증값 혼용이었습니다. 직접 API 서비스는 `DAEGU_CHAIN_TOKEN`을 우선 사용하고, 지급·회수 프록시 `ExternalTokenClient`만 `DAEGU_CHAIN_APP_KEY`를 우선 사용하도록 분리했습니다.
- 슈퍼 관리자 사용자 관리 화면에 사용자 정보 초기화를 추가했습니다. `POST /admin/user/test-data/reset`은 로그인 ID 재입력을 검증하고 `TOKEN_TRANSFERRED` 리워드를 운영 owner 지갑으로 모두 회수한 뒤에만 구강체조 진도·SOH 리워드 거래·리워드 지갑을 삭제합니다. 한 건이라도 회수에 실패하면 DB 초기화를 중단하고 성공한 회수 이력을 보존해 재시도 시 중복 회수를 방지합니다. 계정·기본 인적정보·기관·다대구 연결은 보존합니다.

2026-08-25 사용자 정보 초기화 전 체인 토큰 회수를 추가했습니다.

- 슈퍼 관리자 초기화는 실제 전송 완료된 리워드를 `/token/retrieve`로 먼저 회수하며, 전부 성공한 경우에만 진도·거래·지갑 DB 행을 삭제합니다.
- 기존 로컬 가입 지갑은 SOH에 잘못 저장된 Ed25519 DID 키를 사용하지 않습니다. DID 서버가 자체 키 저장소에서 해당 주소의 체인 지갑키를 읽어 공식 대구체인 `/token/approve`에만 전달하고 owner allowance를 승인한 뒤 `transfer_from`을 수행합니다. 키는 API 응답·감사 로그에 노출하지 않습니다.

2026-08-24 토큰 현황 Chapter 표시와 인트로 선행 차단을 확정했습니다.

- 표시명은 1화 `Intro`, 필수 2~6화 `Chapter 1~5`, 상시 7~12화 `Chapter 7~12`로 사용합니다. 내부 토큰은 기존 `essential_video_1~5`, `optional_video_1~7`을 유지합니다.
- 인트로 완료 전에는 필수·상시영상의 URL 응답과 진도 저장을 서버에서도 차단하고, 인트로 완료 후 상시영상 전체 및 첫 필수영상을 열도록 테스트로 고정합니다.

2026-08-21 다대구 DID 사용자의 리워드 지급을 토큰 서버 로컬 DID 조회와 분리했습니다.

- 다대구 DID는 로그인·가입 식별 정보로 유지하고, `/token/transfer` 지급 요청은 가입 경로와 무관하게 저장된 지갑 주소를 `receiver`로 직접 사용합니다.
- `ExternalTokenClientTest`, `UserRewardServiceTest`, `UserRewardReclaimServiceTest` 및 배포용 `bootJar` 빌드를 통과했습니다.

2026-08-21 다대구 CI 매칭과 기존 로컬 사용자 지갑 재사용을 보강했습니다.

- 다대구 `DaeguMasterVC`의 CI를 복호화 직후 SHA-256으로 해시하고, 외부 DID와 CI 해시가 동일 SOH 사용자에 연결된 경우만 자동 로그인합니다. 기존 사용자 최초 연결은 이름·생년월일·정규화 휴대폰 번호 완전 일치를 fallback으로 사용합니다.
- 기존 로컬 사용자의 다대구 연결 시 이미 발급된 내부 DID·리워드 지갑을 그대로 재사용하고, 누락된 DID 또는 지갑만 생성하도록 프로비저닝을 멱등 처리했습니다.
- 다대구 운영 응답의 성별 표현 차이로 로그인에 실패하지 않도록 제공값을 정규화해 사용자 `M`/`W` 형식으로 저장합니다.

2026-08-21 다대구 기존 사용자 자동 로그인과 신규 사용자 최소입력 자동가입 흐름을 구현했습니다.

- 백엔드 `Senior-Oral-Healthcare-api`: 현재 `prod`, `/login/dadaegu` 신규/기존 분기, `/login/dadaegu/signUp`, 10분 일회용 해시 세션, 외부 DID 매핑, 내부 DID·지갑 롤백 가입을 반영하고 관련 서비스 및 컨트롤러 테스트를 통과했습니다.
- 프론트엔드 `Senior-Oral-Healthcare-front`: 현재 `prod`, 신규 인증 시 기관 선택·필수 약관 전용 화면으로 이동하고 가입 완료 즉시 자동 로그인하도록 반영했으며 production build와 변경 파일 ESLint를 통과했습니다.
- 운영 Secret의 실제 값은 문서에 기록하지 않습니다. prod 배포 후 health, 프론트 200, 모바일 화면 smoke 및 승인된 다대구 계정 실사용 확인이 필요합니다.

2026-08-21 PC/모바일·태블릿 로그인 탭 분기, 실제 다대구 인증 콜백, 로컬 가입 지갑 프로비저닝을 보강했습니다.

- 백엔드 `Senior-Oral-Healthcare-api`: 현재 `prod`, 기준 `dd1c984f`, 다대구 RSA 콜백 복호화·기존 사용자 매칭·SOH 토큰 발급 API 추가, 가입 시 DID·지갑 생성 실패를 가입 실패로 처리하고 과거 미완성 계정은 리워드 요청 시 재프로비저닝하도록 수정, 관련 단위 테스트 통과
- 프론트엔드 `Senior-Oral-Healthcare-front`: 현재 `prod`, 기준 `c3d89d3`, PC는 사용자·관리자 로그인만, 모바일·태블릿은 사용자·다대구·관리자 로그인을 표시하고 다대구 앱 호출·Android/iOS 결과 확인 흐름을 연동, production build 통과
- 운영 다대구 `siteId`·RSA 개인키가 첨부 자료에 없어 환경 Secret 설정 전에는 다대구 버튼이 비활성화됩니다. 실제 운영 다대구 앱/DID/token 서버 호출과 prod 배포 smoke는 수행하지 않았습니다.

2026-08-18 기준 Denti-K MIDSIZE 구강분석 화면·API·맞춤 콘텐츠 노출을 SOH의 모든 로그인 사용자 대상으로 이식했습니다.

- 백엔드 `Senior-Oral-Healthcare-api`: 현재 `prod`, 기준 `93903c9d`, 구강검진·콘텐츠 관련 단위/컨트롤러 테스트 및 Java compile 통과
- 프론트엔드 `Senior-Oral-Healthcare-front`: 현재 `prod`, 기준 `c0d579b`, production build·Vitest·ESLint 오류 0 통과
- 실제 외부 AI 호출과 prod 배포 smoke는 수행하지 않았으며, Denti-K 전용 curation 데이터 부재는 운영 확인 항목으로 남깁니다.

2026-08-18 기준 필수 입체조 영상의 주차·선행 완료 잠금을 API 응답과 시청 이력 저장 양쪽에 적용했습니다.

- 백엔드 `Senior-Oral-Healthcare-api`: 현재 `prod`, 구강체조 서비스 단위 테스트 통과
- 프론트엔드 `Senior-Oral-Healthcare-front`: 현재 `prod`, 공통 로고·입체조 UI·구강상태 기본 날짜 변경 작업 연동

2026-08-14 기준 `git pull origin prod` 후 관리자 권한별 메뉴와 실가입기관별 현황 표시 작업을 진행했습니다.

- 백엔드 `Senior-Oral-Healthcare-api`: 현재 `prod`, 기준 `845b91da`
- 프론트엔드 `Senior-Oral-Healthcare-front`: 현재 `prod`, 기준 `37d337d`
