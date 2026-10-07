# Senior-Oral-Healthcare API

SOH API is a Spring Boot API server. The current project lives under `api_server` and uses Gradle Wrapper with Java 17.

## Security Boundaries and Deployment Prerequisites

Public `POST /admin/account` accepts an application and creates a `PENDING` administrator without tokens or institution access. `POST /admin/account/{adminId}/approve` requires the current super administrator and records approval time and approver. Pending accounts cannot log in, mint or refresh tokens, or use administrator APIs. Approval grants ordinary administrator access only; administrators without an institution then use the existing institution registration flow. Existing accounts with a null approval status retain their previous access for compatibility; this is not a new identity verification, and their approval audit fields remain empty. The super administrator's institution page lists applicants before existing administrators and shows an approval button. Approval is idempotent. See `docs/updates/2026-10-06_SOH_변경기록_기관관리자승인.docx` and the reference-only `docs/db/add-admin-approval.sql`.

`/superadmin/**`, `/admin/daegu-chain/**`, and AWS metrics require `SUPER_ADMIN`. Authorities and approval status are resolved from the current administrator record. User, organization, billing detail and export operations enforce organization ownership; payment and billing status changes require a super administrator. General-user questionnaire results require their owner. Deploy and finish the backend refresh before deploying the approval frontend. Do not roll back to an application that ignores pending approval while new pending accounts exist. Neither repository has been pushed for this change. SMTP credentials are unnecessary while mail is disabled as described below.

Reward-wallet connection accepts an empty request and retains or provisions the server wallet. Caller-supplied wallet addresses and DIDs are rejected. Confirmed token receipts, recovery checkpoints and duplicate protection remain in place. Reclaim approval is requested when reclaiming, for the exact amount; existing on-chain approvals require a separate review.

Photograph uploads accept validated JPEG/PNG content up to 10 MiB and 25 million pixels. Object names use UUIDs and server-detected MIME types. Excel bulk registration accepts up to 5 MiB and 1,000 data rows. Standard paginated queries are capped at 100 records. These limits can reject formerly accepted large files or oversized page requests. Local request limits are 240 standard password-login requests per IP per minute, 30 administrator applications per IP per minute, 30 image-analysis requests per authenticated identity per minute, and 120 TTS requests per identity per minute. Responses exceeding these limits use HTTP 429 and `Retry-After: 60`. Limits are per application instance and do not replace shared account-level abuse protection.

New access tokens are bound to their stored refresh session. Logout, password reset and a new login invalidate those session-bound tokens. Existing tokens without a session claim remain compatible while their stored refresh session is valid; their revocation fallback uses the login timestamp and has second-level granularity. Auto-login preserves the existing refresh token and cannot extend its expiry indefinitely. Access/refresh lifetimes and the refresh response format remain unchanged for this rollout. Short access lifetimes and refresh rotation need a coordinated frontend update.

Mail is disabled by default (`SOH_MAIL_ENABLED=false`, including when unset). Disabled mail registers no overuse billing email listener, skips direct email-service calls and excludes SMTP from health checks. Billing calculation, persistence, duplicate protection and lookup remain available without a mail sender or key. SMTP configuration or a new GitHub mail Secret is not a deployment prerequisite in this mode. To explicitly enable billing notices later, set `SOH_MAIL_ENABLED=true` and provide `SENDGRID_API_KEY` (or supported `SPRING_MAIL_*` overrides); the existing notice and mail health check then apply. No source-code credential fallback is available. Review whether the exposed old SendGrid key is shared before revoking it at the provider; removing it from the application does not invalidate it or clean historical Git commits and logs. See `docs/updates/2026-10-06_SOH_변경기록_미사용메일비활성화.docx`.

SQL and SMTP debug output are disabled. Audit payloads redact credentials, recovery answers and personal fields, and omit raw chain proxy payloads. Historical log retention and previously exposed credentials require separate operational review.

DaDaegu verification, SMS/OTP-dependent signup and recovery changes are excluded at the user's request. Existing HTTP DID/token relay endpoints, key rotation, private-key signing relocation, production schema migrations, OIDC, and Terraform infrastructure changes require separate operational work. This patch does not change those settings or run a production token transfer. The remediation matrix is in `docs/updates/2026-10-06_SOH_변경기록_OWASPv2_보안보완.docx`; do not interpret partially addressed items as fully remediated. The proposed Dependabot configuration needs the repository's default branch and GitHub settings to enable updates; it does not automatically merge or deploy changes.

## First Login Health Survey

Template `2026-09-30-v3` appends only sections 1–4 from `1. 삼킴기능 사전-사후 통합 자가평가문진표_v2(중복제거).docx` to the existing seven sections: supplementary swallowing symptoms (8; 6 questions / 30 points), repeated swallowing ability (9; 6 / 30), tongue function (10; 6 / 30), and oral-health awareness (11; 7 / 35). There are 11 sections and 72 questions in total. The 25 new answers are required single choices stored as 1–5, independently summed without reversal or combining scales. Existing EAT-10 stored values remain 0–4. Researcher before/after interpretation and safety-record sections are not included; no pre/post assessment workflow or medical diagnosis is generated.

Scored sections expose nullable `scoreMax`; dental section 7 has no total. `surveyScores` uses section-number keys 1–6 and 8–11. Existing answers, completion status and first submission date are not rewritten. Existing completed users remain completed, but explicit edits require the newly added answers. Legacy v1/v2 drafts may still be saved; submitting or editing with an old template version asks the client to save a draft and reopen the survey. Revision protection remains in place. DTO limits are 72 answers and tab 1–11; template validation also checks actual section/question bounds.

For this expansion, deploy the frontend that accepts both seven- and eleven-section templates first, then deploy the v3 API template. No new endpoint is required; the frontend tolerates absent `scoreMax` from the earlier API. This avoids breaking the previous frontend's seven-section guard during rollout. Once v3 answers exist, do not revert to the old 47-answer API or seven-only frontend; preserve the expanded data contract and use a compatible corrective deployment. Existing setup instructions below for the original status/admin endpoints still require API-first deployment.

FRAIL question 3 displays the distance in metres without the parenthesized yards; SARC-F question 1 displays kilograms without the parenthesized pounds. Question keys, options, scoring and template version are unchanged. User section clearing uses draft replacement for unfinished surveys; completed edits are saved explicitly after required-answer validation.

General users, including existing and DaDaegu accounts, see the separate eleven-section health survey when they have not submitted it. They may skip it for the current browser-tab login and enter user pages without answering; this does not mark the survey completed. The user menu includes 문진표 after 토큰 현황 for reopening drafts or viewing and editing completed answers. This does not replace the existing oral questionnaire or depend on organization subscriptions or oral-analysis enrollment. Admin accounts are excluded.

- `GET /user/intake-survey/status`: server-owned required/completed status; no row or an unfinished draft means required.
- `GET /user/intake-survey`: versioned template, saved answers, current tab, revision, and completion state.
- `PUT /user/intake-survey/draft`, `POST /user/intake-survey/submit`: `{version, surveyAnswers, currentTab, revision}`. The user is resolved from the authenticated `ROLE_USER` token, never from the payload. Submission validates all required visible questions.
- Template: `api_server/src/main/resources/template/intake-survey.json`, transcribed from the supplied `SOH_항목추가.pdf` (47 questions across EAT-10, EDSQ, FRAIL, aspiration, SARC-F, MNA-SF, and dental visits). Dental frequency is required only for recent visitors; last-visit period and no barriers may be left blank.
- Version `2026-09-19-v2` introduced EAT-10 choices as 1–5 while preserving stored values 0–4 and score totals. Parenthesized English in titles/questions is removed; the FRAIL disease list is supplied separately in `help`. Dental `dental_3` now accepts period values 1–6 (1 month, 6 months, 1 year, 18 months, 2+ years, other); valid legacy `YYYY-MM` answers remain accepted without automatic time-based conversion. Completed answers are not migrated.
- New table: `user_intake_survey`, one row per user, with draft answers, ten separate score totals, template version, current tab, revision, update/completion timestamps. Production `ddl-auto=update` creates it; reference SQL is `docs/db/add-user-intake-survey.sql`. No existing user data is backfilled or deleted.
- User draft saves and administrator draft edits persist the ten per-section score totals in the existing `scores_json` field. Draft totals sum answered items only (unanswered scales are zero), remain provisional while `completed=false`, and are recalculated on every save, including changes and clearing. EAT-10 retains stored 0–4 scores despite displayed 1–5 choices; dental option IDs are not summed. Existing drafts gain totals on their next successful save, with no data backfill, schema or Secret change. Completed totals, first submission time, locking and revision checks are preserved.
- `PUT /user/intake-survey` edits an already completed survey using the same payload. All visible required answers are validated, scores are recalculated, and the first `completedAt` is preserved. Incomplete surveys use draft/submit instead. User-row locking serializes writes; revisions reject stale drafts and completed edits. Existing draft/submit endpoints retain their protection against late drafts and repeated submissions. Health answers and scores are masked in API audit logs. No medical diagnosis is generated.

### Super-admin survey management

- `GET /admin/intake-surveys`: paginated general-user list, including users without a survey. Query parameters: `organization` (exact trimmed `realOrganization`; omitted = all, empty = unassigned), `keyword` (name/login ID), `status` (`ALL`, `NOT_STARTED`, `DRAFT`, `COMPLETED`), zero-based `page`, `size` (1–100, default 20). Response contains `users`, institution options, and page totals. Deleted users are excluded.
- `GET /admin/intake-surveys/{userId}` returns the same eleven-section template and saved state. `PUT` accepts the existing `{version, surveyAnswers, currentTab, revision}` contract. All three endpoints require `ROLE_SUPER_ADMIN`; normal admins and users are denied.
- Administrator edits preserve submission state and original completion time. Completed surveys require all visible mandatory answers and recalculate ten scores; unfinished surveys remain required for the user. User-row locks and revisions prevent stale overwrites. Request/response health answers remain masked by the existing audit logger.
- No schema, Secret, or infrastructure changes. Deploy API before the `/superadmin/intake-surveys` frontend page. Tests: `gradlew test --tests '*IntakeSurvey*'` (query integration uses an isolated H2-compatible test schema).

Deploy the backend and confirm its workflow/health before deploying the frontend, which requires the new status API. Roll back the frontend first if needed and retain the survey table and its data. Secrets, AWS resources, and workflows do not change.

```text
Build tool: Gradle Wrapper
Java: 17
Application directory: api_server
Health check: /api/actuator/health
AWS region: ap-northeast-2
Artifact bucket: denti-backends
Artifact prefix: soh
```

`main` is not a deployment branch. It is only for final reviewed code.

## Oral Exercise Access Policy

### Super-admin transaction recovery and institution filters

Super administrators can open **토큰 관리 → 토큰 지급 내역 → 거래 확인·복구** on a review-required payout. Enter the provider's `fact_hash`, verify the receipt, review the sender/receiver/contract/amount/block height, then confirm recovery. This only completes the existing payout record and credits internal points once; it never sends tokens again. Balance changes alone cannot recover a payout. Missing submission checkpoints or an unavailable/mismatched proof keep the payout unresolved.

- `POST /admin/daegu-chain/token/reward-transfers/{transactionId}/recovery/preview` and `/confirm` accept `{factHash}` and require the current approved super administrator. Confirm fetches fresh proof and rechecks the current wallet/payout and privileges; preview changes no payout or point balance.
- Receipt verification binds the exact hash, configured owner, recipient, contract, amount, integral height and successful `in_state`. Block height and `proposed_at` must agree with the receipt and be no earlier than the submission time minus five minutes of clock skew. Repeated confirmation of the same completed payout returns its existing result.
- Automatic completion and manual recovery both claim a unique hash in the additive `reward_transfer_recovery_evidence` table before crediting points, in the same transaction. The claim records the payout, confirmation time, height and administrator for manual recovery (`admin_id=NULL` for automatic completion). Claims survive user/reward reset or deletion. A hash conflict keeps the other payout under review. Historical hashes in existing payouts are also checked. Production `ddl-auto=update` creates the table; reference-only SQL is `docs/db/add-reward-transfer-recovery-evidence.sql`.
- The seven super-admin pages **사용자 관리, 사용자 진도현황, DID 리워드 현황, 영상시청이력, 토큰 실패이력, 기관별 설문 현황, 대구체인 사용 로그** offer an institution dropdown. It uses the complete trimmed signup institution (`realOrganization`); all is the default and empty/null/blank is unassigned. Similar prefixes do not match. Search and existing authorization remain in place; institution changes reset paginated lists.
- `GET /admin/user/organizations` returns all distinct non-deleted signup institutions, independent of user-list pagination/search, and requires the current super administrator. `GET /admin/user` accepts optional `realOrganization` (omitted=all, empty=unassigned) before page/count; normal administrators retain their existing institution scope. Chain usage-log rows additionally include `realOrganization`. Existing history/survey institution APIs retain their contracts.
- Deploy the backend and verify the additive table, ASG refresh and health before the frontend. No Secrets, chain settings, infrastructure or existing payout backfill change. Preserve the evidence table during rollback and use a version that honors receipt claims; never resend a review-required payout based only on balance. Local mocks verify the UI; actual provider proof recovery needs its real hash and a separately authorized operational action. See `docs/updates/2026-10-07_SOH_변경기록_거래증명복구_기관필터.docx`.

### Viewing history and token failure follow-up

- Number challenges offer two choices (1 and 2), highlight the requested correct number, and allow a wrong choice to be corrected within the original 30-second deadline. Only a correct answer submits the reward request; wrong attempts remain visible in history. Viewing reaches completion at 90% and may continue to the end. The first accepted threshold event counts as COMPLETE; the later ended event does not add another completed session. Previously stored progress of at least 90% also unlocks subsequent videos without rewriting historical session counts.
- Token-enabled button requests must send `acceptsDeferred: true`. They persist a durable `TOKEN_TRANSFER_PENDING` request and return amount 0 until receipt is confirmed. Old open clients receive a refresh instruction before any reward write. Workers commit a `TOKEN_TRANSFER_CHECKING` checkpoint before calling the token server, outside DB locks, with at most four concurrent jobs per instance. The frontend refreshes active transfers every six seconds without restarting playback.
- Recovery validates the exact fact hash, sender, recipient, contract, amount, block height and `in_state` from the production `data.receipt` or transaction lookup `data.trx_info`. Confirmed receipts become `TOKEN_TRANSFERRED` and credit points once. Only pre-submission connection failures or proven chain rejection permit another transfer (three automatic attempts). Balance alone, timeout, HTTP error, malformed response or an unknown upstream result never authorize resubmission. Missing evidence enters `TOKEN_TRANSFER_REVIEW`; historical failures without submission evidence are displayed as review and are never automatically resent. The quest1 failure has no fact hash and requires provider confirmation.
- Pending/checking/review rewards are excluded from received counts and block product collection, wallet replacement, test reset and deletion until settled. Completed transfers remain credited if a later reclaim approval fails; reclaim rechecks approval. Both external clients use a 10-second connection and 25-second response timeout. Receipt/approval latency is external; asynchronous handling improves UI responsiveness rather than guaranteeing faster chain settlement. Recovery metadata adds seven nullable columns and three status values through production schema update, with no Secret or infrastructure change. Deploy backend and complete ASG refresh before frontend; do not downgrade to an app that cannot read new status values.

- `GET /oral-exercise/history` returns the authenticated user's per-video completed session count, failure reasons, receipt state and `retryRequired`. `POST /oral-exercise/failures` accepts `{contentId, sessionId, reason}` for `TOKEN_WRONG` or `TOKEN_TIMEOUT`; `TOKEN_FAILED` is recorded by the reward controller after a failed server call.
- A failure requires a previously accepted VIEW/PLAY in the same user/content/session. User-row locking makes duplicate failure reports idempotent. Failures do not change viewing progress or token balances.
- Completion counts use distinct session IDs with a completed COMPLETE event, never the legacy `viewCount` (which counts progress updates). A new player opening creates a new session; pause/resume and duplicate requests do not create extra completed views.
- Three or more failed sessions for the same active video prompt replay on the next login. Receipt of that video's token or completion of the reward journey suppresses the reminder while preserving history. Three or more completed views show sourced topic-specific health information in the frontend.
- Super administrators alone can access `GET /admin/oral-exercise-history/users?keyword=&page=0&size=20`, `/users/{userId}` and paginated `/users/{userId}/{contentId}?page=0`. Search supports name, login ID and registered institution; member and event pages are bounded.
- The frontend separates `/superadmin/exercise-history` (viewing) and `/superadmin/token-failure-history` (failures). Both use `GET /admin/oral-exercise-history/organizations` for distinct, trimmed institutions of non-deleted members with history, independent of search and pagination. On `/users`, optional `organization` matches the entire trimmed `realOrganization`; omit it for all institutions or send an empty string for unassigned members. `kind=FAILURES` restricts members to recorded token failures before pagination. Event detail accepts `kind=VIEWING` (VIEW/COMPLETE), `FAILURES` (TOKEN_WRONG/TOKEN_TIMEOUT/TOKEN_FAILED) or the backward-compatible default `ALL`. Counts and total pages are calculated after filtering. Past failures remain visible after receipt; recording, reward and replay reminder rules are unchanged. Deploy the API before the updated frontend; no DB, Secret or infrastructure change is needed for these filters.
- Existing `oral_exercise_interaction_log` is retained with three new event values and index `idx_exercise_log_user_content_session`. Production `ddl-auto=update` applies the additive schema change. No secrets or infrastructure resources change.
- Old sessions are counted only where recorded; repeated playback within one historical page may be undercounted. Historical failure reasons cannot be reconstructed. Existing completed progress still identifies watched videos without token receipt.
- Deploy and verify the backend before the frontend. Roll back the frontend first and retain history rows and the additive index. Actual token transfer and iPad Safari playback require separate authorized operational checks.

- Local signup (`POST /login/signUp`, compatibility `/login/signUp/did`) accepts optional `oralAnalysisServiceEnabled`. Only `true` opts in; omitted/null/false opts out. The existing user column and login/profile response contract are reused; no database migration or new secret is required.
- Deploy the API before the frontend signup checkbox so the selected value is persisted. The frontend calls the final collection action `상품 수령`; reward API names and transfer/reclaim semantics are unchanged.

- Only the intro is available before the user completes it. After intro completion, the first core video and all always-open videos become available.
- Each later core video opens as soon as the immediately preceding core video is completed, without waiting for another signup week.
- Locked responses keep thumbnail metadata but omit `videoUrl`.
- `POST /oral-exercise/interactions` enforces both the intro gate and the core previous-completion rule, so a client cannot bypass the sequence by calling the interaction API directly.
- Exercise titles and token-status labels use `Intro`, core `Chapter 1` through `Chapter 5`, and always-open `Chapter 7` through `Chapter 12`. The internal content sort and token identifiers remain unchanged.

## Retired Brushing Feature

- The standalone brushing-record feature and its `/user/brushing` and `/toothBrushing` APIs are retired.
- Oral-status and dashboard responses no longer contain brushing-record sections or statistics.
- Application startup drops only the retired `tooth_brushing` table with `DROP TABLE IF EXISTS`; questionnaire answers and educational content about brushing remain supported.

## Infrastructure Overview

Terraform creates the AWS infrastructure instead of manual console setup:

- VPC, public subnets, private app subnets, and private DB subnets.
- NAT Gateway and S3 Gateway Endpoint.
- ALB security group, EC2 security group, and RDS security group.
- EC2 instance role/profile with S3 artifact read permission and optional SSM.
- ALB, HTTPS listener, target group, optional HTTP to HTTPS redirect, optional Route 53 alias.
- RDS MySQL instance in private DB subnets with AWS-managed master password in Secrets Manager.
- Launch Template with Amazon Linux 2023, Java 17, AWS CLI, and User Data.
- Auto Scaling Group in private app subnets with target group attachment and rolling instance refresh.

Terraform files live in:

```text
infra/terraform
```

Deployment workflow files live in:

```text
.github/workflows
```

## Manual AWS Console Setup Guide

If this environment must be built manually from the AWS web console, use:

```text
readme_수동.md
```

That file documents the AWS Console steps from zero setup through S3 artifacts, VPC, IAM, ALB, ASG, Route 53, CloudFront `/api/*` routing, and release verification.
It also covers the manual RDS MySQL setup that Terraform normally creates.

## Branch and Deployment Policy

```text
main push -> no deploy

dev push  -> no deploy (retired; do not re-enable dev workflows)
prod push -> production API artifact upload and prod ASG instance refresh
```

Do not add a main branch deployment workflow.

AWS dev was retired on 2026-08-05. Remaining dev workflow/Terraform files are historical, not an instruction to deploy. Do not enable them or recreate dev resources. Production still uses shared networking, including the dev-named VPC; do not delete shared resources.

## Agent Handoff

Read AGENTS.md and git status first on any PC. Locate the separate frontend repository before cross-repository work. Search only the relevant sections of the following documents:

```text
README.md
AGENTS.md
readme_수동.md
```

Review README.md and AGENTS.md for each change, but edit them only when the relevant facts or rules change. Keep AGENTS.md around 80-120 lines with core rules and links, not cumulative history. Update affected setup, API contracts, CI/CD, Terraform, Secrets, routing, and manual procedures in the same commit.

### Documentation Maintenance

- Code, configuration, infrastructure, or operational changes: create a dated `docs/updates/YYYY-MM-DD_SOH_변경기록_<topic>.docx`.
- Documentation-only changes or simple investigations may use a dated Markdown change record in the same directory.
- Create a cumulative `docs/handover/YYYY-MM-DD_SOH_외부기업_인수인계_<topic>.docx` only at release sign-off or an explicit external handover request, not for every small fix or push. A release is a verified feature batch or operational handover, not every automatic prod deployment.
- Merge change records since the previous snapshot into the full feature/code map and API, DB, integration, deployment, and operations guidance. Preserve existing development-information and dated DOCX files.
- Record baseline commit, environment, changed locations, API/DB/Secret/deployment impact, verification, remaining risks, and rollback. Never include secret values.
- Historical instructions are preserved in [the AGENTS archive](docs/history/2026-09-15_AGENTS_archive.md). Search it by topic only; its former rules and dated status do not override current AGENTS.md.
- Use bounded file reads and focused tests. Documentation-only work needs link/path checks and `git diff --check`, not an application build or a full cross-repository audit.
- When prod push starts a deployment, check its final result and distinguish workflow success from ASG refresh completion and actual health verification.

After a successful update, commit and push when possible:

```bash
git status --short
git diff --check
git add <changed files>
git commit -m "<clear summary>"
git push origin <current-branch>
```

Do not commit real `.env`, `terraform.tfvars`, Terraform state, build outputs, or local IDE files.

## New Project Startup Checklist

Use this checklist whenever starting a new SOH-style project or moving this project to a new environment.

1. Confirm repositories and branches.
   - Confirm API and frontend repository names.
   - Decide which branches deploy each environment.
   - Confirm `main` is not a deployment branch unless intentionally changed.
   - Write unusual branch mappings explicitly in `README.md` and `AGENTS.md`.

2. Confirm build systems.
   - Frontend: confirm package manager, lockfile, build command, and output directory.
   - API: confirm Gradle/Maven, Java version, app directory, JAR output, and health path.
   - Add workflow build commands that match the repository layout.

3. Prepare GitHub Secrets.
   - Register AWS deployment credentials only as GitHub Secrets.
   - Register environment-specific app secrets such as API `.env` content.
   - Do not commit real `.env`, AWS keys, DB passwords, JWT secrets, or private tokens.

4. Prepare AWS bootstrap resources.
   - Create or choose the Terraform state S3 bucket.
   - Replace Terraform backend placeholders.
   - Confirm S3 artifact bucket region.
   - Prepare ACM certificate ARNs in the target ALB region.
   - Confirm Route 53 hosted zone ownership if DNS will be created by Terraform.
   - Confirm RDS engine, instance class, subnet group, deletion protection, backup retention, and Secrets Manager password handling.

5. Prepare CI/CD.
   - Ensure no workflow deploys from `main`.
   - Ensure dev workflows write only dev artifact paths and refresh only dev ASGs.
   - Ensure prod workflows write only prod artifact paths and refresh only prod ASGs.
   - Keep deploy target names, S3 paths, CloudFront IDs, and ASG names guarded in workflows.

6. Prepare AWS IAM.
   - GitHub Actions IAM user needs artifact upload, ASG refresh, and Terraform plan/apply permissions.
   - EC2 instance roles should read only their own `app.jar` and `.env`. Oral-exercise videos are served from `s3://tms-static-hosting/oral-exercise/video/`, and thumbnails from `s3://tms-static-hosting/oral-exercise/video-thumbnails/`.
   - EC2 User Data must use the instance profile, not long-lived AWS access keys.

7. Prepare CloudFront/API routing.
   - Add `/api/*` behavior to the frontend CloudFront distribution.
   - Disable caching for API behavior.
   - Forward Authorization, Content-Type, query strings, and required headers.
   - Ensure SPA fallback does not rewrite `/api/*` errors to `index.html`.

8. Validate before release.
   - Run YAML parsing checks for GitHub Actions.
   - Run Terraform fmt and validate where Terraform CLI is available.
   - Run local frontend/API builds where possible.
   - Check for forbidden legacy values such as old S3 paths, old regions, OIDC settings, or main-branch deploy triggers.

9. Commit and push.
   - Update `README.md` and `AGENTS.md` together.
   - Run `git diff --check`.
   - Commit a clear summary.
   - Push to the current branch when possible.

10. Confirm deployment.
    - Check uploaded S3 artifacts.
    - Confirm ASG Instance Refresh started.
    - Check EC2 User Data and systemd logs.
    - Check internal and external health endpoints.

## Terraform Modules

Created modules:

```text
infra/terraform/modules/network
infra/terraform/modules/security
infra/terraform/modules/iam
infra/terraform/modules/alb
infra/terraform/modules/rds
infra/terraform/modules/launch_template
infra/terraform/modules/autoscaling
```

Created environments:

```text
infra/terraform/environments/dev
infra/terraform/environments/prod
```

## Terraform Bootstrap

Before GitHub Actions apply can work, do this once:

1. Create or choose a Terraform state S3 bucket, for example `thomabio-terraform-state`.
2. Replace `<TERRAFORM_STATE_BUCKET>` in both backend files.
3. For local apply, copy each `terraform.tfvars.example` to `terraform.tfvars` and fill real values.
   For GitHub Actions apply, store the filled tfvars content in `SOH_TERRAFORM_TFVARS_DEV` and `SOH_TERRAFORM_TFVARS_PROD_HCL`.
4. Replace `certificate_arn` with an ACM certificate ARN in `ap-northeast-2`.
5. Review the RDS values. Current examples use EC2 `t3.medium` and RDS `db.t3.small`.
6. Confirm the artifact bucket region:

```bash
aws s3api get-bucket-location --bucket denti-backends
```

DynamoDB lock table is optional and can be added to the backend later.

## Dev Infrastructure

Historical reference only: dev was retired. Do not run the commands below to recreate it. Shared networking still used by prod must be preserved.

```bash
cd infra/terraform/environments/dev
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

The dev defaults create:

```text
VPC CIDR: 10.70.0.0/16
ALB: soh-api-dev-alb
Target group: soh-api-dev-tg
Launch template: soh-api-dev-lt
ASG: soh-api-dev-asg
Origin domain: soh-api-dev.thomabio.com
Release type: dev
EC2 instance type: t3.medium
RDS: soh-api-dev-mysql, MySQL 8.0, db.t3.small, single-AZ
RDS database: thomastone
```

Dev uses a single NAT Gateway by default for cost control.

## Prod Infrastructure

```bash
cd infra/terraform/environments/prod
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

The prod defaults create:

```text
VPC: existing development VPC soh-api-dev-vpc
Public subnets: soh-api-dev-public-1, soh-api-dev-public-2
Private app subnets: soh-api-dev-private-app-1, soh-api-dev-private-app-2
Private DB subnets: soh-api-dev-private-db-1, soh-api-dev-private-db-2
ALB: soh-api-prod-alb
Target group: soh-api-prod-tg
Launch template: soh-api-prod-lt
ASG: soh-api-prod-asg
Origin domain: api.soh.thomabio.com
Release type: prod
EC2 instance type: t3.medium
RDS: soh-api-prod-mysql, MySQL 8.4.10, db.t3.small, single-AZ
RDS database: thomastone
```

Prod intentionally reuses the development VPC and its existing public, private app, and private DB subnets. Do not create a separate prod VPC unless the deployment policy is explicitly changed. Production Terraform apply is workflow-dispatch only and should use GitHub Environment approval through `production-infra`.
If an older prod VPC was already created by Terraform, review the prod plan before apply and migrate or remove state intentionally; do not approve an unexpected VPC/subnet/NAT destroy plan during production deployment.
Prod RDS has deletion protection enabled and requires a final snapshot on destroy unless intentionally changed.

## API Deployment Flow

Development API deployment:

1. Push `dev` branch.
2. GitHub Actions runs `deploy-api-dev.yml`.
3. Gradle/Maven auto-detect builds a Spring Boot `app.jar`. Gradle deploy builds skip `test` and `asciidoctor` because this project wires REST Docs generation into `bootJar`; run full tests separately before release approval.
4. `SOH_API_ENV_DEV` creates `.env`.
5. Uploads `s3://denti-backends/soh/dev/app.jar`.
6. Uploads `s3://denti-backends/soh/dev/.env`.
7. Waits for any existing `soh-api-dev-asg` Instance Refresh to finish, then starts a new refresh with `MinHealthyPercentage=0` so the single-instance dev API can recover when the existing target is already unhealthy.
8. New EC2 instances run User Data and download `app.jar` and `.env` from S3.
9. systemd starts `soh-api-dev`.
10. Check `https://soh-dev.thomabio.com/api/actuator/health`.

Production API deployment:

1. Push `prod` branch.
2. GitHub Actions runs `deploy-api-prod.yml`.
3. Gradle/Maven auto-detect builds a Spring Boot `app.jar`. Gradle deploy builds skip `test` and `asciidoctor` because this project wires REST Docs generation into `bootJar`; run full tests separately before release approval.
4. `SOH_API_ENV_PROD` creates `.env`; the workflow rejects datasource URL, username, password, or driver keys so RDS credentials cannot be copied to GitHub or S3.
5. Waits until Terraform has promoted the ASG launch template to the default version and verifies that its User Data contains the Secrets Manager JDBC configuration.
6. Uploads `s3://denti-backends/soh/prod/app.jar`.
7. Uploads the credential-free `s3://denti-backends/soh/prod/.env`.
8. Waits for any existing `soh-api-prod-asg` Instance Refresh to finish, then starts a new refresh.
9. New EC2 instances run User Data, download `app.jar` and `.env`, and inject the RDS managed secret ARN at boot.
10. systemd starts `soh-api-prod`.
11. Check `https://api.soh.thomabio.com/api/actuator/health`.

Production deploy workflow runs are not auto-cancelled by newer prod deploy runs; they queue behind the active run to avoid interrupting an artifact upload or ASG refresh.

## GitHub Actions Workflows

```text
terraform-plan.yml        -> pull_request touching infra/terraform/** and workflow_dispatch
terraform-apply-dev.yml   -> workflow_dispatch only
terraform-apply-prod.yml  -> workflow_dispatch only, environment production-infra
deploy-api-dev.yml        -> dev branch push and workflow_dispatch
deploy-api-prod.yml       -> prod branch push and workflow_dispatch
```

`terraform-plan.yml` temporarily disables the S3 backend file in the ephemeral GitHub Actions checkout and runs against local state with `terraform.tfvars.example`. This keeps PR validation working before the real backend bucket is configured. Treat that PR plan as a syntax/provider sanity check, not as the authoritative remote-state deployment plan. The apply workflows use the S3 backend and the filled `SOH_TERRAFORM_TFVARS_*` secrets.

## Required GitHub Secrets

AWS deployment credentials:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

API env secrets:

```text
SOH_API_ENV_DEV
SOH_API_ENV_PROD
```

Terraform apply tfvars secrets:

```text
SOH_TERRAFORM_TFVARS_DEV
SOH_TERRAFORM_TFVARS_PROD_HCL
```

Each `SOH_TERRAFORM_TFVARS_*` secret should contain the filled content of that environment's `terraform.tfvars.example`. Do not put AWS access keys, DB passwords, JWT secrets, or real `.env` content in these Terraform tfvars secrets.
The Terraform apply workflows mask each tfvars line before Terraform can report a parse error and reject application `.env` keys or sensitive variable names before `terraform init`. Keep `SOH_API_ENV_*` and `SOH_TERRAFORM_TFVARS_*` as separate GitHub Secrets; they are not interchangeable.
`SOH_TERRAFORM_TFVARS_PROD_HCL` should keep `db_engine_version = "8.4.10"` unless the production RDS instance is intentionally upgraded. The production apply workflow rejects other values because short version values such as `8.4` can resolve to an older patch version and make Terraform try an invalid downgrade. Keep `create_route53_record = true` so Terraform preserves the `api.soh.thomabio.com` Route 53 alias record.

The deploy workflows create `.env` from `SOH_API_ENV_DEV` or `SOH_API_ENV_PROD` and upload it to S3. Do not put RDS passwords in GitHub Secrets. Production additionally rejects `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_DATASOURCE_DRIVER_CLASS_NAME` before upload. During EC2 boot, the launch template writes those datasource settings from Terraform's RDS endpoint and managed secret ARN through the AWS Secrets Manager JDBC driver.
The prod deploy workflow accepts dedicated DaeguChain overrides: `DAEGU_CHAIN_APP_KEY_PROD` and `DAEGU_CHAIN_TOKEN_PROD` (with their unqualified secret names as fallbacks). The single GitHub Secret `TOKEN_SERVER_BASE_URL` overrides both `TOKEN_SERVER_BASE_URL` and `DID_SERVER_BASE_URL` in the generated `.env`, so DID provisioning and token relay requests use the same SOH_DID server. Set it to the server's HTTPS base URL without a path (for example, `https://did.thomabio.com`). Other values in `SOH_API_ENV_PROD` are preserved; when this dedicated secret is absent or empty, both existing URL settings are retained. `TOKEN_SERVER_BASE_URL_PROD` is no longer used. Direct DaeguChain URLs and credentials are unchanged. Deploying the updated workflow and API is required to apply the shared address. At least one of `DAEGU_CHAIN_APP_KEY` or `DAEGU_CHAIN_TOKEN` must be present in the generated `.env` for token list/create/transfer APIs.

`SOH_API_ENV_DEV` example:

Store the secret as multiline text. Each `KEY=VALUE` pair must be on its own line; do not paste it as one concatenated line.

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh-dev.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh-dev.thomabio.com
JWT_ACCESS_KEY=<DEV_JWT_ACCESS_SIGNING_KEY_AT_LEAST_32_CHARACTERS>
JWT_REFRESH_KEY=<DEV_JWT_REFRESH_SIGNING_KEY_AT_LEAST_32_CHARACTERS>
DAEGU_CHAIN_APP_KEY=<DEV_DAEGU_CHAIN_APP_KEY>
DAEGU_CHAIN_ID=mitumt
DID_SERVER_BASE_URL=http://43.201.125.82
DID_CREATE_PATH=/did/create
TOKEN_SERVER_BASE_URL=http://43.201.125.82
DAEGU_CHAIN_TOKEN_OWNER_ADDRESS=<DEV_DAEGU_CHAIN_TOKEN_OWNER_ADDRESS>
DAEGU_CHAIN_TOKEN_SYMBOL=MYT
DAEGU_CHAIN_TOKEN_DECIMALS=18
USER_REWARD_TOKEN_TRANSFER_ENABLED=false
```

`SOH_API_ENV_PROD` example:

Store the secret as multiline text. Each `KEY=VALUE` pair must be on its own line; do not paste it as one concatenated line.

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh.thomabio.com
JWT_ACCESS_KEY=<PROD_JWT_ACCESS_SIGNING_KEY_AT_LEAST_32_CHARACTERS>
JWT_REFRESH_KEY=<PROD_JWT_REFRESH_SIGNING_KEY_AT_LEAST_32_CHARACTERS>
DAEGU_CHAIN_APP_KEY=<PROD_DAEGU_CHAIN_APP_KEY>
DAEGU_CHAIN_ID=mitumt
DID_SERVER_BASE_URL=<PROD_DID_SERVER_BASE_URL>
DID_CREATE_PATH=/did/create
TOKEN_SERVER_BASE_URL=<PROD_TOKEN_SERVER_BASE_URL>
TOKEN_RECLAIM_PATH=/token/retrieve
DAEGU_CHAIN_TOKEN_OWNER_ADDRESS=<PROD_DAEGU_CHAIN_TOKEN_OWNER_ADDRESS>
DAEGU_CHAIN_TOKEN_OWNER_PRIVATE_KEY=<PROD_DAEGU_CHAIN_TOKEN_OWNER_PRIVATE_KEY>
DAEGU_CHAIN_TOKEN_SYMBOL=MYT
DAEGU_CHAIN_TOKEN_DECIMALS=18
USER_REWARD_TOKEN_TRANSFER_ENABLED=true
```

Do not commit real `.env` files. GitHub Actions creates `.env`, uploads it to S3, and EC2 downloads it through the instance profile.
Both deploy workflows reject an environment file unless `JWT_ACCESS_KEY` and `JWT_REFRESH_KEY` are present and each contains at least 32 characters. These signing keys have no repository fallback; rotate any value that has ever appeared in source control or logs.
Terraform passes `db_address`, `db_port`, `db_name`, and `db_master_user_secret_arn` to the launch template. EC2 rewrites `SPRING_DATASOURCE_URL` to `jdbc-secretsmanager:mysql://...`, sets `SPRING_DATASOURCE_USERNAME` to the secret ARN, clears `SPRING_DATASOURCE_PASSWORD`, and starts the app with `com.amazonaws.secretsmanager.sql.AWSSecretsManagerMySQLDriver`. The launch template resource promotes its managed latest version to the default version, and the production deploy workflow requires the ASG's explicit version to match that default before refreshing instances. The EC2 instance profile must keep `secretsmanager:DescribeSecret` and `secretsmanager:GetSecretValue` on that RDS managed secret. When Secrets Manager rotates the RDS password, the JDBC driver refreshes cached credentials for new DB connections, so GitHub Secrets do not need to be edited.
DaeguChain API requests use `DAEGU_CHAIN_APP_KEY` for every outbound request body field named `token`; keep app keys and any private keys only in environment secrets.
Mobile/tablet DaDaegu login uses `DADAEGU_LOGIN_ENABLED`, `DADAEGU_LOGIN_SITE_ID`, `DADAEGU_LOGIN_RSA_PRIVATE_KEY`, and optional `DADAEGU_LOGIN_REQUIRED_VC` (default `DaeguMasterVC`). The public `/login/dadaegu/config` response exposes only readiness, site ID, and required VC; the PKCS#8 RSA private key must remain only in the backend environment secret. `/login/dadaegu` decrypts the encrypted master-VC claims and first resolves the external DaDaegu DID through `dadaegu_user_identity`; for a first-time binding it may match an existing SOH user by normalized phone/name/birth date. Existing users receive normal SOH login tokens immediately. New users receive a 10-minute, one-use onboarding token and complete `POST /login/dadaegu/signUp` with only `realOrganization` and all required service-agreement IDs. The server then creates the SOH-only account, internal DaeguChain DID, reward wallet, agreement consents, and external identity mapping in one transaction before issuing login tokens. Raw onboarding tokens are never stored, and encrypted callback payloads plus all token/private-key/password fields are masked in application logs.

DaDaegu onboarding uses the auto-created `dadaegu_signup_session` and `dadaegu_user_identity` tables. The signup session stores a SHA-256 token hash with expiry/consumption timestamps; expired rows are removed when a new session is issued. External DaDaegu DID values must remain separate from the user's internal reward DID and wallet provisioning state.

Production deploys may override those values without replacing the shared `SOH_API_ENV_PROD` payload by using dedicated Secrets: `DADAEGU_LOGIN_ENABLED_PROD`, `DADAEGU_LOGIN_SITE_ID_PROD`, `DADAEGU_LOGIN_RSA_PRIVATE_KEY_PROD`, and optional `DADAEGU_LOGIN_REQUIRED_VC_PROD`. When the enabled override is `true`, the workflow rejects the deployment unless both the site ID and RSA private key are present.
Production JWT signing keys are also supplied through the dedicated `JWT_ACCESS_KEY_PROD` and `JWT_REFRESH_KEY_PROD` Secrets. The prod workflow upserts them into the generated `.env`, so rotating JWT keys does not require replacing the multiline `SOH_API_ENV_PROD` Secret. Both values must be different, previously unexposed values containing at least 32 characters; rotation invalidates existing login tokens.
`DID_SERVER_BASE_URL` must point to the reachable DID service used by `/did/create`; development currently uses `http://43.201.125.82`. `TOKEN_SERVER_BASE_URL` must point to the same reachable token server for `/token/create`, `/token/transfer`, `/token/retrieve`, and `/token/token_list`; do not leave it at `http://localhost:5000` on deployed API servers. User signup DID provisioning sends `label` with the user's login identifier, stores the returned `did:key`, and login checks the SOH user DID value and DID issued status without VC-JWT credential verification. The DID response address and private key are not reused as reward-wallet credentials; local and DaDaegu signups both provision reward-wallet address/key pairs through `/mitum/com/acc_create` and activate them with `/mitum/com/acc_faucet`.
Normal password-based signup and DID signup both complete only after a Daegu DID and reward wallet address are stored. If an older user reaches a reward request with a failed or missing DID/wallet, the reward service retries DID and wallet provisioning before token transfer; provisioning failures remain explicit instead of leaving a newly registered user in a partially configured state.
Oral-exercise reward reclaim uses the token server's configurable `TOKEN_RECLAIM_PATH` (default `/token/retrieve`). It sends the stored reward wallet address as `holder` and the configured token owner as both `sender` and `receiver`, so DaDaegu external DIDs are never submitted as `user_DID`. `DAEGU_CHAIN_TOKEN_OWNER_PRIVATE_KEY` is required only in the backend environment and is masked from API audit logs; SOH must not read, log, or persist user DID private keys for this reclaim flow. For a legacy locally issued wallet, the DID server resolves the separate chain-wallet key from its protected key store, sends it only to the official DaeguChain `/token/approve` endpoint, and never returns or logs it.

`DAEGU_CHAIN_APP_KEY` and `DAEGU_CHAIN_TOKEN` are different credentials. The token-server proxy (`TOKEN_SERVER_BASE_URL`, including reward issue/reclaim) uses the app key, while direct DaeguChain `/mitum/...` calls (account creation, DID, and token approval) use the user token. Do not substitute the app key into direct API request `token` fields. `/mitum/com/acc_create` returns a key pair but does not make the address an on-chain sender account, so SOH activates raw wallets with `/mitum/com/acc_faucet`. A contract token approval is not attempted during login because DaeguChain returns `P06D504` until that wallet has a token balance state for the contract. The wallet signing key is encrypted at rest with AES-256-GCM using the dedicated Base64 `DAEGU_CHAIN_WALLET_ENCRYPTION_KEY`; production receives it from `DAEGU_CHAIN_WALLET_ENCRYPTION_KEY_PROD`. Legacy encrypted 64-character raw or `0x`-prefixed hexadecimal keys originate from the Ed25519 DID flow and are incompatible with `/mitum/token/approve`; SOH replaces those reward wallets with newly activated `/mitum/com/acc_create` address/key pairs rather than transforming the DID key. After a reward transfer creates the contract balance state, SOH approves only that contract and rechecks approval before reclaim. Plaintext signing keys never enter the database or audit logs, `holder_pkey` remains masked, and upstream error messages that echo sensitive request values are sanitized before storage or propagation. Super admins can reset a user's oral-exercise progress, reward transactions, and reward wallet through `POST /admin/user/test-data/reset`; the request must contain the exact target login ID as confirmation. Every `TOKEN_TRANSFERRED` reward is reclaimed before deletion, and any reclaim failure aborts the reset while preserving successful reclaim records for idempotent retry. The login account, profile, organization, and DaDaegu identity mapping remain intact, so the next login or wallet lookup can provision a newly activated wallet when its signing key is missing or incompatible.
When `USER_REWARD_TOKEN_TRANSFER_ENABLED=true`, oral-exercise video rewards are transferred through DaeguChain token contracts by reward token name. Development keeps this disabled by default so token transfer outages do not block exercise completion.
Reward issuance addresses the stored reward wallet directly. The external `/token/transfer` request intentionally omits `user_DID`, because that field makes the token server resolve only DIDs issued in its own local DID database and rejects DaDaegu-issued DIDs before sending the chain transaction.

## GitHub Actions IAM User Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UploadSohApiArtifacts",
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/*",
        "arn:aws:s3:::denti-backends/soh/prod/*"
      ]
    },
    {
      "Sid": "ListSohApiArtifactPrefixes",
      "Effect": "Allow",
      "Action": ["s3:ListBucket", "s3:GetBucketLocation"],
      "Resource": "arn:aws:s3:::denti-backends",
      "Condition": {
        "StringLike": {
          "s3:prefix": ["soh/dev/*", "soh/prod/*"]
        }
      }
    },
    {
      "Sid": "RefreshSohApiAutoScalingGroups",
      "Effect": "Allow",
      "Action": [
        "autoscaling:StartInstanceRefresh",
        "autoscaling:DescribeInstanceRefreshes",
        "autoscaling:DescribeAutoScalingGroups"
      ],
      "Resource": [
        "arn:aws:autoscaling:ap-northeast-2:160885266674:autoScalingGroup:*:autoScalingGroupName/soh-api-dev-asg",
        "arn:aws:autoscaling:ap-northeast-2:160885266674:autoScalingGroup:*:autoScalingGroupName/soh-api-prod-asg"
      ]
    }
  ]
}
```

If `aws_region` changes, update the Auto Scaling ARNs as well.

Terraform plan/apply uses the same AWS credentials in the current workflows. That principal also needs permissions for managed infrastructure resources, including EC2/VPC, ELBv2, IAM, Auto Scaling, Route 53 when enabled, RDS, and Secrets Manager. Scope these permissions to `soh-api-*`, the configured VPC resources, and the Terraform state bucket where practical.

## EC2 Instance Role Policies

Dev EC2 role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadSohDevApiArtifacts",
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/app.jar",
        "arn:aws:s3:::denti-backends/soh/dev/.env"
      ]
    },
    {
      "Sid": "WriteSohDevRuntimeUploads",
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/*"
      ]
    },
    {
      "Sid": "DenySohDevDeployArtifactOverwrite",
      "Effect": "Deny",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/app.jar",
        "arn:aws:s3:::denti-backends/soh/dev/.env"
      ]
    },
    {
      "Sid": "SynthesizeTtsSpeech",
      "Effect": "Allow",
      "Action": ["polly:SynthesizeSpeech"],
      "Resource": "*"
    }
  ]
}
```

Prod EC2 role:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadSohProdApiArtifacts",
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/prod/app.jar",
        "arn:aws:s3:::denti-backends/soh/prod/.env"
      ]
    },
    {
      "Sid": "WriteSohProdRuntimeUploads",
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/prod/*"
      ]
    },
    {
      "Sid": "DenySohProdDeployArtifactOverwrite",
      "Effect": "Deny",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/prod/app.jar",
        "arn:aws:s3:::denti-backends/soh/prod/.env"
      ]
    },
    {
      "Sid": "SynthesizeTtsSpeech",
      "Effect": "Allow",
      "Action": ["polly:SynthesizeSpeech"],
      "Resource": "*"
    }
  ]
}
```

Terraform also attaches `AmazonSSMManagedInstanceCore` by default so access can use SSM Session Manager. SSH is not opened by default.

## Security Notes

Browser login refresh tokens are issued as `HttpOnly`, `Secure`, `SameSite=Lax` cookies and are omitted from JSON responses. Access tokens remain header-based so state-changing API authorization does not rely on an ambient cookie. The frontend restores a session through `PUT /login/access-token` and `GET /auth/session`, while `POST /auth/logout` revokes the stored refresh token and expires the cookie.

Cookie defaults are `SOH_REFRESH_TOKEN`, path `/api`, Secure enabled, and SameSite `Lax`. Override them only with `AUTH_REFRESH_COOKIE_NAME`, `AUTH_COOKIE_PATH`, `AUTH_COOKIE_SECURE`, and `AUTH_COOKIE_SAME_SITE`; local plain-HTTP development must explicitly use `AUTH_COOKIE_SECURE=false`.

All `/superadmin/**` endpoints require `ROLE_SUPER_ADMIN`. This rule is evaluated before the broader `/admin/**` administrator rule.

Initial ALB security group ingress allows HTTPS 443 from IPv4/IPv6 public internet. Before production traffic, restrict this to the CloudFront origin-facing managed prefix list where possible.

The EC2 security group only accepts TCP 8080 from the ALB security group. EC2 outbound is broad in the base module for package install, S3, DB, and service dependencies; narrow it later when dependency destinations are finalized.

The RDS security group only accepts TCP 3306 from the EC2 security group. RDS is created in private DB subnets with `publicly_accessible = false`.

## CloudFront API Integration

Existing frontend CloudFront distributions are not managed by this Terraform. Import them later only if you intentionally move CloudFront under Terraform.

Development CloudFront `E14WPL6NG95U7H`:

1. Add origin: `soh-api-dev.thomabio.com`.
2. Origin protocol policy: HTTPS only.
3. Add behavior path pattern: `/api/*`.
4. Allowed methods: GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE.
5. Cache policy: CachingDisabled.
6. Origin request policy: forward Authorization, Content-Type, query string, and other API-required values.

Production API:

1. Use the dedicated backend domain: `api.soh.thomabio.com`.
2. Point the Route 53 record to `soh-api-prod-alb`.
3. Configure the frontend production `VITE_API_BASE_URL` as `https://api.soh.thomabio.com/api`.
4. Ensure backend CORS allows `https://soh.thomabio.com`.
5. A frontend CloudFront `/api/*` behavior is not required for production when the dedicated API domain is used.

If SPA fallback uses CloudFront custom error response 403/404 -> `/index.html` 200, API 403/404 can accidentally become `index.html`. Prefer a CloudFront Function that rewrites only non-API frontend routes:

```js
function handler(event) {
  var request = event.request;
  var uri = request.uri;

  if (uri.startsWith('/api/')) {
    return request;
  }

  if (uri.endsWith('/')) {
    request.uri = uri + 'index.html';
  } else if (!uri.includes('.')) {
    request.uri = '/index.html';
  }

  return request;
}
```

## Operations Commands

### Production restored and temporary backend stopped

The API domain resolves to the original `soh-api-prod-alb` in account `160885266674`. The production workflow and ASG instance refresh completed successfully, with external HTTPS health `200 / UP` and login CORS verified. Actual authenticated login was not retested during restoration. The refresh briefly returned HTTP 502 while the new application started; this was not a zero-downtime deployment. See the [production restoration record](docs/updates/2026-09-30_SOH_변경기록_기존AWS운영재배포.docx).

The temporary manual service on `54.180.133.42`, `soh-api-temp`, is stopped to prevent duplicate scheduled jobs and remains disabled at boot. Files in `/home/ec2-user/soh-api-temp` are retained for recovery. Nginx configuration `/etc/nginx/conf.d/soh-api-temp.conf` still provides HTTPS and forwards `/api/` requests to port 8080 without removing the prefix, but the temporary API process is no longer running. Frontend configuration remains unchanged.

The temporary server's Let's Encrypt certificate is retained under `/etc/letsencrypt/live/api.soh.thomabio.com/`. `soh-certbot-renew.timer` remains active and checks renewal daily, but successful renewal must not be assumed now that DNS points to the production ALB. Do not copy the certificate private key into the repository. Stop the timer and remove temporary secrets when retiring the temporary server.

The retained protected temporary configuration uses ordinary MySQL JDBC with TLS and a credential copied from the existing RDS managed secret, as approved only for that temporary run. No AWS access keys were deployed there. Restored production uses Secrets Manager JDBC; static datasource settings must not be added to GitHub/S3 environment artifacts. Before restarting the temporary runtime, verify its DB credential, network access, and known AWS upload, Polly, and CloudWatch limitations.

The temporary DB route and security-group rule restricted to `54.180.133.42/32` were not changed during restoration; remove both when this runtime is permanently retired. Base configuration and rollback identifiers are recorded in [the temporary deployment record](docs/updates/2026-09-29_SOH_변경기록_임시서버_수동배포준비.docx), with the earlier HTTPS setup in [the HTTPS connection record](docs/updates/2026-09-29_SOH_변경기록_임시서버_HTTPS연결.docx). Coordinate scheduled jobs before any temporary-server failback.

S3 artifact check:

```bash
aws s3 ls s3://denti-backends/soh/dev/ --region ap-northeast-2
aws s3 ls s3://denti-backends/soh/prod/ --region ap-northeast-2
```

EC2 User Data logs:

```bash
sudo cat /var/log/userdata.log
sudo cat /var/log/cloud-init-output.log
```

systemd:

```bash
sudo systemctl status soh-api-dev
sudo systemctl status soh-api-prod
```

App logs:

```bash
tail -f /var/www/soh-api/app.log
tail -f /var/www/soh-api/error.log
```

Health checks:

```bash
curl -i http://localhost:8080/api/actuator/health
curl -i https://soh-dev.thomabio.com/api/actuator/health
curl -i https://api.soh.thomabio.com/api/actuator/health
```

## Validation

Local Terraform validation can use backend disabled until the state bucket placeholder is replaced:

```bash
terraform fmt -recursive infra/terraform
cd infra/terraform/environments/dev && terraform init -backend=false && terraform validate
cd ../prod && terraform init -backend=false && terraform validate
```

Local API build:

```bash
cd api_server
./gradlew clean bootJar
```

If tests or REST Docs require external services, document the reason and use a deployment build variant such as `./gradlew clean bootJar -x test -x asciidoctor` only after confirming the project task name.

## Member Real Organization

User registration APIs (`POST /login/signUp`, `POST /login/signUp/did`, `POST /login/dadaegu/signUp`) require `realOrganization` with one of `소화성당`, `대구1`, `대구2`, `대구3`, or `기타_천안`. The selected value is stored in nullable `user.real_organization` so pre-existing users and administrator bulk-upload records remain compatible. Deploy this API before the frontend that offers `소화성당` as the first institution; the previous `소하성당` spelling remains accepted for already-open signup clients during rollout, and existing institution records are preserved; no schema or Secret change is required.

### Member deletion and token reclaim

`DELETE /admin/user` completes only after all outstanding transferred rewards have been reclaimed. Current reward wallets approve the token owner's contract allowance before each reclaim, including optional-video tokens. Legacy wallets with missing or DID-only signing keys rely on the token server's existing wallet-key lookup; failed authorization or transfer preserves the member, identity mapping, and outstanding reward records. Previously successful reclaims are skipped on retry using their idempotency keys. Never clear rewards or delete a member to bypass a reclaim failure. Clients must check the response body's `rt == 200`, because API errors may also use HTTP 200.

User login uses `POST /login` with `userType=user` and verifies the login ID and BCrypt password without requiring an issued DID. The legacy ID-only `POST /login/did` endpoint has been removed; mobile and tablet users can still use the separate DaDaegu integration at `POST /login/dadaegu`. Administrator login behavior is unchanged.

Both user registration APIs require `userGender` (`M` or `W`) and `userBirthDate`. Standard user registration additionally requires `userPassword`, `findPwdQuestionId`, and `findPwdAnswer`; the question list is available from `GET /password/questions`. The legacy DID registration endpoint remains for compatibility but its former ID-only login endpoint is no longer available. `POST /login/find-id` returns the login identifier only when name, normalized phone number, and birth date all match.

The `dev` and `prod` profiles upsert the nine legacy recovery questions with stable IDs `1` through `9` during application startup. This keeps existing `user.find_pwd_question_id` references usable and safely restores missing question rows in a newly provisioned database.

- The existing `user.organizationId` relationship is unchanged: newly registered users still belong to the organization managed by `tokenadmin`.
- `GET /user/info` returns `realOrganization` for the user profile page.
- `GET /admin/user` returns `realOrganization` in each user-list item for administrator and super-administrator views.
- Super-administrator progress, DID reward, and DaeguChain log endpoints load visible users without Criteria Query null precedence, then sort by `realOrganization` with unassigned users last. This avoids a Hibernate `UnsupportedOperationException` that otherwise makes all three administrator pages appear empty even when production data exists.
- Production and development profiles use Hibernate `ddl-auto: update`, so application startup adds the nullable column. Verify the generated schema change and the member-registration flow after deployment.

## Oral Analysis and Personalized Content

All authenticated SOH users can use plaque analysis, gingivitis analysis, questionnaires, and personalized content regardless of subscription plan. The frontend uses the same user routes for every plan, and the backend does not apply the former `GROWTH`/`MIDSIZE` personalized-content gate.

The user content menu always opens the personalized view and exposes a visible `Personalized Contents / All Contents` switch. The oral-status timeline distinguishes plaque and gingivitis records with the same green tooth and red heartbeat visual language as the analysis chooser, and labels each record as a plaque or gingivitis detection result.

Gingivitis analysis also exposes the Denti-K-compatible contract while preserving the legacy `/oralCheck/gingivitis` endpoint:

```text
POST /gingivitis-analyses
GET  /gingivitis-analyses/{analysisId}
GET  /gingivitis-condition
```

The new endpoints require a valid logged-in user token and validate that result lookups belong to the requesting user. Plaque AI `contents_type` and `plaque_contents` values are retained in the SOH response and used to resolve recommendations. If the AI returns direct content IDs, those take precedence; otherwise SOH resolves content through the existing oral-status mapping.

SOH now includes the Denti-K-compatible `content_curation_rule` entity and table contract. `analysis_type` accepts `QUESTIONNAIRE`, `GINGIVITIS`, or `PLAQUE`; `result_key` uses questionnaire A-K, gingivitis S/G/A/D, or plaque result grades. Active rules are ordered by `curation_rank` and `contents_id`, and duplicate `(analysis_type, result_key, contents_id)` rows are prohibited. Rule rows must reference SOH's own `contents.contents_id`; Denti-K IDs must not be copied without an explicit content-ID mapping. An analysis can legitimately show the completed/no-match state when the table exists but has no matching active rows. This change adds the DB schema contract but does not change Secrets, AWS resources, or deployment workflows.

## First Login Health Survey

Template `2026-09-30-v3` appends only sections 1–4 from `1. 삼킴기능 사전-사후 통합 자가평가문진표_v2(중복제거).docx` to the existing seven sections: supplementary swallowing symptoms (8; 6 questions / 30 points), repeated swallowing ability (9; 6 / 30), tongue function (10; 6 / 30), and oral-health awareness (11; 7 / 35). There are 11 sections and 72 questions in total. The 25 new answers are required single choices stored as 1–5, independently summed without reversal or combining scales. Existing EAT-10 stored values remain 0–4. Researcher before/after interpretation and safety-record sections are not included; no pre/post assessment workflow or medical diagnosis is generated.

Scored sections expose nullable `scoreMax`; dental section 7 has no total. `surveyScores` uses section-number keys 1–6 and 8–11. Existing answers, completion status and first submission date are not rewritten. Existing completed users remain completed, but explicit edits require the newly added answers. Legacy v1/v2 drafts may still be saved; submitting or editing with an old template version asks the client to save a draft and reopen the survey. Revision protection remains in place. DTO limits are 72 answers and tab 1–11; template validation also checks actual section/question bounds.

For this expansion, deploy the frontend that accepts both seven- and eleven-section templates first, then deploy the v3 API template. No new endpoint is required; the frontend tolerates absent `scoreMax` from the earlier API. This avoids breaking the previous frontend's seven-section guard during rollout. Once v3 answers exist, do not revert to the old 47-answer API or seven-only frontend; preserve the expanded data contract and use a compatible corrective deployment. Existing setup instructions below for the original status/admin endpoints still require API-first deployment.

- Newly registered users (local, compatibility DID, and Dadaegu signup) must complete `/user/onboarding-survey` before entering protected user pages. Existing users remain exempt: their nullable `user.onboarding_survey_required` stays NULL; only new signup code writes true.
- The separate `onboarding_survey` table stores one immutable response per authenticated user (`user_id` primary key), template version, JSON answers, six section score totals, and submission timestamp. Production `ddl-auto: update` adds the nullable user column and the new table at application startup. No existing answers or membership rows are rewritten.
- Authenticated user APIs: `GET /onboarding-survey/status` returns `required`/`completed`; `GET /onboarding-survey/template` supplies the versioned Korean template; `POST /onboarding-survey/submit` accepts `{version, answers: {questionKey: [stringValue]}}`. All URLs retain the `/api` mount.
- The PDF-based template has 47 questions across EAT-10, EDSQ, FRAIL, aspiration risk, SARC-F, MNA-SF, and dental use. Dental visit frequency is required only when `dental_1 = "1"`; otherwise it is omitted from storage (46 applicable questions). Explicit unknown/never-visited month answers and an exclusive no-difficulty option avoid forcing inaccurate dental histories.
- Both client and server reject unanswered, duplicate, out-of-range, malformed-month, and future-month answers. Zero is a valid answer. The server returns HTTP 400 with `questionKey` for input errors; the client scrolls/focuses the first invalid question and blocks submission. Legacy API errors are also checked through the `rt` envelope.
- User-row locking plus the response primary key prevents duplicate submissions. Retry after a completed save returns success without overwriting answers. Failed saves keep the member required; draft answers live only in browser memory, so reload/closing before submission clears the draft.
- Deploy the backend first and wait for ASG refresh/health, then deploy the frontend. No Secret, infrastructure, reward, or existing questionnaire contract changes are required. The new controller is outside the request-body logging pointcut; survey answers are not copied to system/error audit tables.
- Scores are stored as section sums only; this feature does not add diagnostic interpretations, rewards, or an administrator results screen. The source PDF's attribution and MNA-SF usage note remain recorded in the change document.
