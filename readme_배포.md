# SOH 운영 배포 순서

이 문서는 SOH 운영 배포를 어디에서 무엇을 실행해야 하는지 정리한 runbook이다.

운영 도메인:

```text
Frontend: https://soh.thomabio.com
Backend API: https://api.soh.thomabio.com/api
Backend health: https://api.soh.thomabio.com/api/actuator/health
```

운영 배포 브랜치:

```text
frontend prod push -> frontend production deploy
api prod push      -> backend production deploy
main push          -> deploy 없음
```

운영 API 인프라 정책:

```text
prod API는 별도 prod VPC를 만들지 않는다.
prod API는 dev VPC soh-api-dev-vpc를 공유한다.
prod API ASG 이름은 soh-api-prod-asg다.
```

## 1. 배포 전 확인

두 저장소를 모두 확인한다.

```powershell
cd C:\Users\ethanj\Documents\GitHub\Senior-Oral-Healthcare-api
git status --short --branch

cd C:\Users\ethanj\Documents\GitHub\Senior-Oral-Healthcare-front
git status --short --branch
```

작업 트리가 지저분하면 배포하지 말고 먼저 어떤 변경인지 확인한다.

```powershell
git diff --check
git diff --name-status
```

GitHub CLI를 쓸 경우 인증 상태를 확인한다.

```powershell
gh auth status
```

## 2. 최초 운영 인프라 구축 순서

운영 API를 처음 띄우거나 `soh-api-prod-asg`가 없으면 앱 배포보다 인프라 구축이 먼저다.

현재 prod deploy가 아래 오류로 실패하면 이 단계가 필요하다.

```text
AutoScalingGroup name not found - null
```

### 2.1 Terraform Plan 실행

GitHub 웹 화면:

```text
GitHub -> ThomastoneCoLTD/Senior-Oral-Healthcare-api
Actions -> Terraform Plan
Run workflow
Branch: prod
Run workflow 클릭
```

CLI:

```powershell
gh workflow run terraform-plan.yml `
  --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api `
  --ref prod
```

결과 확인:

```powershell
gh run list --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api --workflow terraform-plan.yml --limit 3
gh run watch <RUN_ID> --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api --exit-status
```

확인할 것:

```text
Terraform plan (prod) 성공
prod VPC를 새로 만들지 않음
soh-api-dev-vpc를 data source로 참조
api.soh.thomabio.com 사용
```

### 2.2 Terraform Apply Prod 실행

주의: `Terraform Apply Prod` workflow는 plan 뒤에 `terraform apply -auto-approve`를 실행한다. Plan 결과를 먼저 확인하고, 예상하지 못한 VPC/subnet/NAT 삭제가 보이면 실행하지 않는다.

GitHub 웹 화면:

```text
GitHub -> ThomastoneCoLTD/Senior-Oral-Healthcare-api
Actions -> Terraform Apply Prod
Run workflow
Branch: prod
Run workflow 클릭
production-infra environment 승인이 뜨면 승인
```

CLI:

```powershell
gh workflow run terraform-apply-prod.yml `
  --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api `
  --ref prod
```

결과 확인:

```powershell
gh run list --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api --workflow terraform-apply-prod.yml --limit 3
gh run watch <RUN_ID> --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api --exit-status
```

AWS 콘솔에서 확인:

```text
EC2 -> Auto Scaling Groups -> soh-api-prod-asg
EC2 -> Load Balancers -> soh-api-prod-alb
EC2 -> Target Groups -> soh-api-prod-tg
RDS -> Databases -> soh-api-prod-mysql
Route 53 -> thomabio.com -> api.soh record
```

## 3. 백엔드 운영 앱 배포

운영 인프라가 정상일 때 실행한다.

GitHub 웹 화면:

```text
GitHub -> ThomastoneCoLTD/Senior-Oral-Healthcare-api
Actions -> Deploy SOH API Prod
Run workflow
Branch: prod
Run workflow 클릭
```

CLI:

```powershell
gh workflow run deploy-api-prod.yml `
  --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api `
  --ref prod
```

또는 prod 브랜치에 코드를 반영해서 자동 실행한다.

```powershell
cd C:\Users\ethanj\Documents\GitHub\Senior-Oral-Healthcare-api
git fetch --all --prune
git switch prod
git pull --ff-only origin prod

# 검증 완료된 브랜치를 prod에 merge한 뒤 push
git merge --no-ff origin/dev -m "Merge dev into prod"
git push origin prod
```

배포 workflow가 하는 일:

```text
1. api_server 빌드
2. SOH_API_ENV_PROD로 .env 생성
3. s3://denti-backends/soh/prod/app.jar 업로드
4. s3://denti-backends/soh/prod/.env 업로드
5. soh-api-prod-asg Instance Refresh 실행
```

확인:

```powershell
gh run list --repo ThomastoneCoLTD/Senior-Oral-Healthcare-api --workflow deploy-api-prod.yml --branch prod --limit 3
curl -i https://api.soh.thomabio.com/api/actuator/health
```

AWS 콘솔 확인:

```text
EC2 -> Auto Scaling Groups -> soh-api-prod-asg -> Instance refresh
EC2 -> Target Groups -> soh-api-prod-tg -> Targets -> healthy
S3 -> denti-backends -> soh/prod/app.jar
S3 -> denti-backends -> soh/prod/.env
```

## 4. 프론트엔드 운영 배포

프론트 production env는 아래 값이어야 한다.

```text
VITE_API_BASE_URL=https://api.soh.thomabio.com/api
VITE_APP_DOMAIN=https://soh.thomabio.com
VITE_APP_ENV=production
```

로컬 검증:

```powershell
cd C:\Users\ethanj\Documents\GitHub\Senior-Oral-Healthcare-front
npm ci
npm run build
```

GitHub 웹 화면:

```text
GitHub -> ThomastoneCoLTD/Senior-Oral-Healthcare-front
Actions -> Deploy SOH Production Server from prod branch
Run workflow
Branch: prod
Run workflow 클릭
```

CLI:

```powershell
gh workflow run deploy-soh-production-server.yml `
  --repo ThomastoneCoLTD/Senior-Oral-Healthcare-front `
  --ref prod
```

또는 prod 브랜치에 코드를 반영해서 자동 실행한다.

```powershell
cd C:\Users\ethanj\Documents\GitHub\Senior-Oral-Healthcare-front
git fetch --all --prune
git switch prod
git pull --ff-only origin prod

# 검증 완료된 브랜치를 prod에 merge한 뒤 push
git merge --no-ff origin/dev -m "Merge dev into prod"
git push origin prod
```

배포 workflow가 하는 일:

```text
1. npm ci
2. npm run build
3. dist를 s3://thomabio-soh-frontend에 sync
4. CloudFront E3BD44T0U5EBYT invalidation
```

확인:

```powershell
gh run list --repo ThomastoneCoLTD/Senior-Oral-Healthcare-front --workflow deploy-soh-production-server.yml --branch prod --limit 3
```

브라우저 확인:

```text
https://soh.thomabio.com
개발자도구 Network에서 API 요청이 https://api.soh.thomabio.com/api 로 나가는지 확인
```

## 5. 권장 전체 순서

최초 운영 구축:

```text
1. API Terraform Plan prod 성공 확인
2. API Terraform Apply Prod 실행
3. AWS 콘솔에서 soh-api-prod-asg 존재 확인
4. API Deploy SOH API Prod 실행
5. https://api.soh.thomabio.com/api/actuator/health 확인
6. Frontend Deploy SOH Production Server 실행
7. https://soh.thomabio.com 화면 확인
8. 로그인, 관리자, 구강체조, 치은염 업로드 주요 기능 확인
```

일반 코드 배포:

```text
1. dev에서 기능 검증
2. backend prod 배포
3. backend health 확인
4. frontend prod 배포
5. frontend 화면과 API 호출 확인
```

프론트만 바뀐 경우:

```text
1. npm run build
2. frontend prod workflow 실행
3. https://soh.thomabio.com 확인
```

백엔드만 바뀐 경우:

```text
1. API prod workflow 실행
2. ASG instance refresh 확인
3. health endpoint 확인
4. 프론트에서 관련 기능 확인
```

## 6. 장애 대응

`soh-api-prod-asg`를 찾을 수 없음:

```text
원인: prod 인프라가 아직 생성되지 않았거나 ASG 이름이 다름
조치: Terraform Plan -> Terraform Apply Prod -> ASG 생성 확인 -> Deploy SOH API Prod 재실행
```

API health가 실패:

```text
1. Target Group soh-api-prod-tg target 상태 확인
2. EC2 Session Manager 접속
3. sudo cat /var/log/userdata.log
4. sudo systemctl status soh-api-prod
5. tail -f /var/www/soh-api/app.log
6. SOH_API_ENV_PROD의 DB/JWT/DaeguChain/DID 값 확인
```

프론트 화면은 뜨지만 API가 실패:

```text
1. .env.production의 VITE_API_BASE_URL 확인
2. 브라우저 Network에서 호출 URL 확인
3. API CORS allowed origin에 https://soh.thomabio.com 포함 여부 확인
4. api.soh.thomabio.com 인증서와 Route 53 record 확인
```

Terraform Apply Prod 실행 전:

```text
기존 prod VPC/subnet/NAT 삭제 plan이 보이면 중단
prod는 dev VPC 공유 정책이므로 별도 prod VPC 생성/삭제가 없어야 정상
```

## 7. 배포 후 기록

배포가 끝나면 아래를 기록한다.

```text
배포 일시:
Frontend commit:
Backend commit:
Terraform apply 여부:
API health 결과:
Frontend 확인 URL:
문제/조치:
```

문서 유지 규칙:

```text
배포 브랜치, GitHub Actions workflow, AWS 리소스 이름, 도메인, VPC 정책, env 값이 바뀌면 README.md, AGENTS.md, readme_배포.md를 같이 갱신한다.
```
