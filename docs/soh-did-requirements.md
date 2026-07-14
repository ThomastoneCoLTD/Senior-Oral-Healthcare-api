# SOH DID/DaeguChain 요구사항 정의서

작성일: 2026-07-14

## 1. 문서 개요

본 문서는 다음 두 기준을 병합해 SOH 서비스의 DID, VC, 지갑, DaeguChain 토큰 연동 요구사항을 정리한다.

- 첨부 문서: `요구사항 정의서_did_server.hwpx`
- 현재 SOH API 코드: Spring Boot 기반 `Senior-Oral-Healthcare-api`

첨부 문서는 Flask 기반 DID 서버 자체의 요구사항을 포함한다. 현재 SOH API는 DID 서버를 직접 대체하지 않고, DID 서버와 DaeguChain/token server를 호출해 회원가입, DID 로그인, 구강운동 리워드 지급/회수, 관리자 토큰 생성 기능을 제공한다. 따라서 본 문서는 요구사항을 아래 세 범위로 구분한다.

- DID 서버 요구사항: 외부 DID 서버가 제공해야 하는 API 및 저장 책임
- SOH API 요구사항: 현재 Spring Boot 서버가 구현하거나 구현해야 하는 연동, 저장, 검증 책임
- 통합 요구사항: DID 서버, DaeguChain, token server, SOH API, 프론트엔드가 함께 만족해야 하는 동작

## 2. 시스템 개요

SOH DID/DaeguChain 연동은 사용자의 DID를 생성하고, 로그인 credential을 발급/검증하며, 구강운동 콘텐츠 리워드 토큰을 지급/회수하는 기능을 제공한다.

주요 구성요소는 다음과 같다.

| 구성요소 | 책임 |
| --- | --- |
| SOH Frontend | DID 회원가입, DID 로그인, 지갑주소 조회/발급, 구강운동 리워드 UI 제공 |
| SOH API | 사용자 저장, DID 생성 연동, VC 발급/검증 연동, 지갑주소 저장, 토큰 지급/회수 처리 |
| DID Server | DID 생성, DID Document/키 관리, VC 발급, VC 검증 제공 |
| DaeguChain API | 계정 생성, 토큰 생성/조회/전송/승인/소각/민팅 등 D-chain API 제공 |
| Token Server | SOH 리워드 토큰 생성, 토큰 목록 조회, 토큰 전송 API 제공 |
| DB | 사용자 DID/credential 상태, reward wallet, reward transaction, 구강운동 진행 정보 저장 |

## 3. 주요 사용자

| 사용자 | 설명 |
| --- | --- |
| 일반 사용자 | DID 회원가입/로그인 후 구강운동 콘텐츠를 시청하고 리워드 토큰을 받는 대상 |
| 관리자 | 구강운동 리워드 토큰을 생성하고 토큰 목록/계약 주소를 확인하는 대상 |
| SOH API | DID 서버, DaeguChain API, token server를 호출하는 서버 애플리케이션 |
| DID 서버 | DID/VC 관련 암호화 작업과 DID 문서/키 저장을 담당하는 외부 서비스 |
| DaeguChain/token server | 계정 생성, 토큰 생성, 토큰 전송, 토큰 목록 조회를 처리하는 외부 서비스 |

## 4. 시스템 범위

### 4.1 포함 범위

- DID 회원가입
- DID 서버 `/did/create` 연동
- DID 서버 `/did/issue-vc` 연동
- DID 서버 `/did/verify-vc` 연동
- SOH 사용자 `daeguDid`, credential JWT, credential 상태 저장
- reward wallet DID 및 wallet address 저장
- DID 응답에 지갑주소가 없을 때 DaeguChain 계정 생성으로 wallet address 보강
- DID 기반 사용자 로그인
- DaeguChain DID 관리 API 프록시 일부
- DaeguChain common account API 프록시
- DaeguChain token-20 API 프록시
- 관리자 구강운동 리워드 토큰 생성/목록 조회
- 구강운동 필수/선택 영상 토큰 지급
- 필수 5개 구강운동 토큰 수령 후 회수/리워드 처리
- 환경변수 기반 외부 연동 설정

### 4.2 외부 DID 서버 책임

- DID 생성
- DID Document 생성 및 저장
- 개인키 저장 및 보호
- DID 목록 조회
- DID 문서 조회
- DID 삭제
- VC 발급
- VC 검증
- VP 발급/검증

현재 SOH API는 VP 발급/검증을 직접 호출하지 않는다.

### 4.3 제외 또는 미연동 범위

- SOH API 내부 DID Document 파일 저장
- SOH API 내부 사용자 DID private key 저장
- SOH API에서 DID 목록/문서 조회/삭제를 사용자 기능으로 직접 제공
- SOH API에서 VP 발급/검증을 회원가입/로그인 흐름에 직접 사용
- Flask DID 서버 테스트 웹 화면 구현

## 5. 기능 요구사항

### FR-001 DID 회원가입

SOH API는 사용자가 DID 회원가입을 요청하면 사용자 계정을 생성하고 DID를 발급해야 한다.

| 항목 | 내용 |
| --- | --- |
| SOH API | `POST /login/signUp/did` |
| 입력 | `userLoginIdentifier`, `userName`, `userPhoneNumber`, `userBirthDate`, `userServiceAgreementRequest` |
| 처리 | 로그인 아이디/전화번호 중복 확인, 기본 Daegu 기관 연결, 사용자 저장, DID 발급, credential 발급, 서비스 이용 동의 저장 |
| DID 서버 호출 | `POST {DID_SERVER_BASE_URL}{DID_CREATE_PATH}` |
| DID 생성 요청 | `label`에 `userLoginIdentifier` 전달 |
| 저장 | `user.daeguDid`, `user.daeguDidKey`, `user.daeguDidStatus`, `user.daeguCredentialJwt`, `user.daeguCredentialStatus`, `user_reward_wallet.daeguDid`, `user_reward_wallet.walletAddress` |
| 응답 | 사용자 정보, access token, refresh token, DID 상태, credential 상태, wallet address |
| 실패 | DID 생성 실패 시 회원가입 트랜잭션은 실패해야 한다 |

검수 기준:

- 정상 회원가입 후 `daeguDidStatus=ISSUED`가 저장된다.
- 정상 회원가입 후 `user_reward_wallet.walletAddress`가 저장된다.
- DID 서버 응답에 `address`가 없고 `did:key`만 있어도 DaeguChain 계정 생성 API로 wallet address가 보강된다.

### FR-002 DID 생성

DID 서버는 사용자의 DID를 생성할 수 있어야 하며, SOH API는 이를 호출할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| DID 서버 API | `POST /did/create` |
| 첨부 문서 별칭 | `POST /did/create_account`, `POST /did/signup` |
| 입력 후보 | `label`, `userIdentifier`, `id`, `user_id` |
| DID 서버 처리 | Ed25519 키쌍 생성, `did:key` DID 생성, DID Document 생성, DID 문서/개인키/인덱스 저장, D-chain 지갑 계정 생성 |
| SOH 처리 | DID 서버 응답에서 `did`, `DID`, `address`, `publickey`, `credentialJwt` 등을 추출 |
| 보강 처리 | DID 서버가 wallet address를 반환하지 않으면 SOH API가 DaeguChain common account create API로 wallet address 생성 |

검수 기준:

- DID 서버는 생성된 DID를 응답해야 한다.
- 가능한 경우 DID 서버는 wallet address를 응답해야 한다.
- SOH API는 DID가 비어 있으면 `DID 생성 실패`로 처리해야 한다.

### FR-003 DID 목록 조회

DID 서버는 생성된 DID 목록을 조회할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| DID 서버 API | `GET /did/dids` |
| 응답 | 전체 DID 개수, DID 목록, 생성일, label, fingerprint, account address |
| SOH 현황 | 사용자 기능으로 직접 노출하지 않음 |
| SOH 관련 API | `POST /daegu-chain/did/account-list`는 DaeguChain DID account list 프록시 |

검수 기준:

- DID 서버에서 DID 목록과 account address가 조회되어야 한다.
- SOH API에서 해당 기능이 필요하면 별도 사용자/관리자 API 정책을 정의해야 한다.

### FR-004 DID 문서 조회

DID 서버는 DID 또는 fingerprint 기준으로 DID Document를 조회할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| DID 서버 API | `POST /did/resolve` |
| 입력 | DID 또는 fingerprint |
| 예외 | 입력값 없음: 400, DID 문서 없음: 404 |
| SOH 현황 | 직접 연동 없음 |

검수 기준:

- DID 서버는 DID Document를 반환해야 한다.
- SOH 로그인 검증에 필요한 공개키 조회는 DID 서버 `/did/verify-vc` 내부에서 처리한다.

### FR-005 DID 삭제

DID 서버는 DID와 관련된 DID Document 및 개인키 파일을 삭제할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| DID 서버 API | `POST /did/delete` |
| 처리 | DID Document 파일 삭제, 개인키 파일 삭제, index 제거 |
| SOH 현황 | 직접 연동 없음 |

검수 기준:

- 삭제 대상이 없으면 404를 반환해야 한다.
- SOH 사용자 탈퇴와 DID 삭제를 연동할지는 별도 정책으로 정의해야 한다.

### FR-006 로그인 VC 발급

SOH API는 회원가입 또는 credential 재발급 시 DID 서버를 통해 로그인용 VC-JWT를 발급해야 한다.

| 항목 | 내용 |
| --- | --- |
| DID 서버 API | `POST /did/issue-vc` |
| SOH API | `POST /daegu-chain/did/issue-login-user-credential` |
| 입력 | SOH 사용자 access token |
| DID 서버 요청 | `issuer=user.daeguDid`, `subject=user.daeguDid`, `claims.id=userLoginIdentifier`, `claims.userIdentifier=userLoginIdentifier`, `ttl`, `aud` |
| 설정 | `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_TEMPLATE_ID`, `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_VALID_DAYS`, 선택적 valid from/until |
| 저장 | `user.daeguCredentialJwt`, `user.daeguCredentialStatus=ISSUED`, 유효 시작/종료일 |

검수 기준:

- DID 서버가 `vc_jwt` 또는 `credentialJwt` 또는 `jwt`를 응답하면 SOH는 이를 저장해야 한다.
- credential JWT가 비어 있으면 발급 실패로 처리해야 한다.

### FR-007 로그인 VC 검증

SOH API는 DID 로그인 시 저장된 VC-JWT를 DID 서버에서 검증해야 한다.

| 항목 | 내용 |
| --- | --- |
| SOH API | `POST /login/did` |
| DID 서버 API | `POST /did/verify-vc` |
| 입력 | `userLoginIdentifier` |
| 처리 | 사용자 조회, DID 발급 상태 확인, credential JWT 존재 확인, DID 서버 검증 호출 |
| 검증 | `valid=true`, issuer/subject DID 일치, credential subject가 로그인 아이디와 일치 |
| 실패 | DID 미발급 또는 검증 실패 시 로그인 거부 |

검수 기준:

- VC가 유효하고 subject가 로그인 아이디와 일치하면 로그인 토큰을 발급한다.
- VC가 없으면 SOH API는 재발급을 시도하고, 실패하면 로그인을 거부한다.

### FR-008 VP 발급

DID 서버는 holder DID의 개인키로 VC 목록을 포함한 VP JWT를 발급할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| DID 서버 API | `POST /did/present-vp` |
| 입력 | `holder`, `vc_jwts`, `aud`, `ttl` |
| 응답 | `vp_jwt`, `exp` |
| SOH 현황 | 현재 회원가입/로그인/리워드 흐름에서는 미연동 |

검수 기준:

- DID 서버는 VP JWT를 반환해야 한다.
- SOH에서 VP 기반 인증을 사용할 경우 별도 API와 저장 정책을 추가해야 한다.

### FR-009 VP 검증

DID 서버는 VP JWT와 포함된 VC 목록을 검증할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| DID 서버 API | `POST /did/verify-vp` |
| 입력 | `vp_jwt`, `aud` |
| 응답 | VP 유효 여부, holder, VC별 검증 결과, payload |
| SOH 현황 | 현재 미연동 |

검수 기준:

- DID 서버는 VP와 내부 VC 검증 결과를 함께 반환해야 한다.

### FR-010 DaeguChain DID 관리 프록시

SOH API는 DaeguChain DID 관리 API 일부를 프록시해야 한다.

| SOH API | 외부 경로 |
| --- | --- |
| `POST /daegu-chain/did/project-list` | `/mitum/did/projects` |
| `POST /daegu-chain/did/regist-project` | `/mitum/did/regist_project` |
| `POST /daegu-chain/did/template-list` | `/mitum/did/templates` |
| `POST /daegu-chain/did/edit-template` | `/mitum/did/edit_template` |
| `POST /daegu-chain/did/account-list` | `/mitum/did/accounts` |
| `POST /daegu-chain/did/account-create` | DID 서버 `/did/create` |
| `POST /daegu-chain/did/get-key` | `/mitum/did/get_key` |
| `POST /daegu-chain/did/issue-credential` | `/mitum/did/issue` |
| `POST /daegu-chain/did/disclosure` | `/mitum/did/disclosure` |
| `POST /daegu-chain/did/verification` | `/mitum/did/verification` |
| `POST /daegu-chain/did/revoke-credential` | `/mitum/did/revoke` |
| `POST /daegu-chain/did/qr-code` | `/mitum/did/qrcode` |

검수 기준:

- outbound request body의 `token`은 환경변수 `DAEGU_CHAIN_APP_KEY` 또는 `DAEGU_CHAIN_TOKEN`에서 주입되어야 한다.
- 필수 필드가 없으면 400 계열 오류를 반환해야 한다.

### FR-011 DaeguChain common account 프록시

SOH API는 DaeguChain common account API를 프록시해야 한다.

| SOH API | 외부 경로 |
| --- | --- |
| `POST /daegu-chain/common/account/create` | `/mitum/com/acc_create` |
| `POST /daegu-chain/common/account/faucet` | `/mitum/com/acc_faucet` |
| `POST /daegu-chain/common/account/balance` | `/mitum/com/acc_balance` |
| `POST /daegu-chain/common/account/information` | `/mitum/com/acc_info` |
| `POST /daegu-chain/common/account/transfer` | `/mitum/com/cur_transfer` |

검수 기준:

- 계정 생성 응답에서 `key_pair.address`를 받을 수 있어야 한다.
- DID 서버 응답에 wallet address가 없을 때 SOH는 이 API를 사용해 reward wallet address를 보강한다.

### FR-012 DaeguChain token-20 프록시

SOH API는 DaeguChain token-20 API를 프록시해야 한다.

| SOH API | 외부 경로 |
| --- | --- |
| `POST /daegu-chain/token-20/create` | `/mitum/token/create` |
| `POST /daegu-chain/token-20/upload` | `/mitum/upload/upload_token` |
| `POST /daegu-chain/token-20/list` | `/mitum/token/tokens` |
| `POST /daegu-chain/token-20/total-supply` | `/mitum/token/supply` |
| `POST /daegu-chain/token-20/balance` | `/mitum/token/balance` |
| `POST /daegu-chain/token-20/mint` | `/mitum/token/mint` |
| `POST /daegu-chain/token-20/burn` | `/mitum/token/burn` |
| `POST /daegu-chain/token-20/approve` | `/mitum/token/approve` |
| `POST /daegu-chain/token-20/allowance` | `/mitum/token/allowance` |
| `POST /daegu-chain/token-20/transfer` | `/mitum/token/transfer` |
| `POST /daegu-chain/token-20/transfer-from` | `/mitum/token/transfer_from` |

검수 기준:

- `contAddr`, sender/receiver, private key, amount 등 필수 입력값은 DTO validation으로 검증해야 한다.
- app key는 환경변수에서 주입되어야 한다.

### FR-013 관리자 리워드 토큰 생성/조회

관리자는 구강운동 리워드 토큰 이름 목록을 확인하고 token server를 통해 토큰을 생성할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| 토큰 옵션 API | `POST /admin/daegu-chain/token/options` |
| 토큰 목록 API | `POST /admin/daegu-chain/token/list` |
| 토큰 생성 API | `POST /admin/daegu-chain/token/create` |
| 허용 토큰명 | `ESSENTIAL_VIDEO_1`~`ESSENTIAL_VIDEO_5`, `OPTIONAL_VIDEO_1`~`OPTIONAL_VIDEO_7` |
| 외부 연동 | token server `/token/token_list`, `/token/create` |

검수 기준:

- 허용되지 않은 토큰명 생성 요청은 거부해야 한다.
- token server 목록 응답에서 계약 주소를 추출해 토큰 옵션에 반영해야 한다.

### FR-014 reward wallet 조회/연결

사용자는 자신의 reward wallet과 포인트 잔액을 조회하고, 지갑이 없으면 연결/생성할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| 조회 API | `GET /user/rewards/wallet` |
| 연결 API | `POST /user/rewards/wallet/connect` |
| 저장 | `user_reward_wallet.user_id`, `daeguDid`, `walletAddress`, `pointBalance` |
| 자동 보정 | wallet row가 없거나 wallet address가 없으면 사용자 DID 또는 신규 DID/account 생성 결과로 보정 |
| 개발 fallback | dev/local profile에서 app key 누락으로 DID 생성 실패 시 로컬 테스트 주소 생성 가능 |

검수 기준:

- 회원가입 직후 wallet address가 조회되어야 한다.
- 지갑주소가 없을 때 연결 API 호출로 wallet address가 저장되어야 한다.

### FR-015 구강운동 토큰 지급

SOH API는 구강운동 콘텐츠 시청/버튼 클릭에 따라 리워드 토큰 또는 로컬 포인트를 지급해야 한다.

| 항목 | 내용 |
| --- | --- |
| 버튼 지급 API | `POST /oral-exercise/rewards/button-click` |
| 완료 지급 | `OralExerciseService`의 영상 완료 처리에서 `rewardOralExerciseCompletion` 호출 |
| 지급 토큰명 | 콘텐츠 sort 기준 `essential_video_1`~`essential_video_5`, `optional_video_1`~`optional_video_7` |
| 중복 방지 | 사용자+토큰명 기반 idempotency key 사용 |
| 외부 전송 | `USER_REWARD_TOKEN_TRANSFER_ENABLED=true`일 때 token server `/token/transfer` 사용 |
| 로컬 기록 | 토큰 전송 disabled일 때 `LOCAL_RECORDED`로 기록 |

검수 기준:

- 같은 영상 토큰은 중복 지급되지 않아야 한다.
- 외부 토큰 전송 실패 시 거래 상태는 실패로 기록되고 보상 실패 응답을 반환해야 한다.

### FR-016 필수 구강운동 토큰 회수

사용자는 필수 구강운동 5개 토큰을 모두 받은 뒤 리워드 회수 처리를 요청할 수 있어야 한다.

| 항목 | 내용 |
| --- | --- |
| API | `POST /oral-exercise/rewards/reclaim` |
| 조건 | `essential_video_1`~`essential_video_5` 수령 완료 |
| 외부 전송 | `USER_REWARD_TOKEN_TRANSFER_ENABLED=true`일 때 token server `/token/transfer`로 `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS`에 회수 전송 |
| 로컬 기록 | token transfer disabled일 때 로컬 reclaim transaction 기록 |
| 중복 방지 | reclaim idempotency key 사용 |

검수 기준:

- 필수 5개 토큰이 없으면 회수 요청은 실패해야 한다.
- 이미 회수된 거래는 skip 처리되어야 한다.
- SOH API는 회수 흐름에서 사용자 DID private key를 읽거나 저장하지 않아야 한다.

### FR-017 `/mitum` prefix 처리

SOH API의 DaeguChain client는 base URL이 `/mitum`으로 끝나는 경우 중복 prefix를 제거해야 한다.

| 항목 | 내용 |
| --- | --- |
| 설정 | `DAEGU_CHAIN_API_BASE_URL` |
| 처리 | API base URL이 `/mitum`으로 끝나고 path가 `/mitum/...`이면 path의 앞 `/mitum`을 제거 |

검수 기준:

- `https://.../mitum` base URL과 `/mitum/token/create` path 조합이 `https://.../mitum/token/create`로 호출되어야 한다.

## 6. 데이터 요구사항

### 6.1 SOH API DB

| 테이블/엔티티 | 주요 필드 | 설명 |
| --- | --- | --- |
| `user` | `daeguDid`, `daeguDidKey`, `daeguDidStatus` | 사용자 DID와 발급 상태 |
| `user` | `daeguCredentialJwt`, `daeguCredentialStatus`, `daeguCredentialValidFrom`, `daeguCredentialValidUntil` | 로그인 VC-JWT와 유효기간 |
| `user_reward_wallet` | `userId`, `pointBalance`, `daeguDid`, `walletAddress` | 사용자 reward wallet 및 포인트 잔액 |
| `user_reward_transaction` | `type`, `status`, `amount`, `coinId`, `tokenContractAddress`, `daeguChainTxHash`, `idempotencyKey` | 리워드 지급/회수 거래 |
| `oral_exercise_content` | `contentSort`, `title`, `videoUrl`, `thumbnailUrl`, `durationSeconds` | 구강운동 콘텐츠 및 토큰명 매핑 기준 |

### 6.2 DID 서버 데이터

첨부 문서 기준 DID 서버는 다음 데이터를 관리해야 한다.

| 데이터 | 설명 |
| --- | --- |
| `DID` 테이블 | DID, private key, public key, account address |
| DID Document 파일 | `data/dids/{fingerprint}.did.json` |
| 개인키 파일 | `data/keys/{fingerprint}.key.json` |
| DID 인덱스 | `data/index.json` |

SOH API는 사용자 DID private key를 저장하지 않는 것을 원칙으로 한다.

### 6.3 토큰 데이터

| 데이터 | 설명 |
| --- | --- |
| token server token list | token name, contract address, symbol, supply, decimals, owner, tx hash |
| reward token name | `ESSENTIAL_VIDEO_1`~`ESSENTIAL_VIDEO_5`, `OPTIONAL_VIDEO_1`~`OPTIONAL_VIDEO_7` |
| reward transaction contract | 지급/회수에 사용한 token contract address |

## 7. 외부 연동 및 환경변수

### 7.1 SOH API 환경변수

| 환경변수 | 설명 |
| --- | --- |
| `DAEGU_CHAIN_BASE_URL` | DaeguChain 기본 URL |
| `DAEGU_CHAIN_API_BASE_URL` | DaeguChain API base URL override |
| `DAEGU_CHAIN_API_VERSION` | API version, 기본 `v2` |
| `DAEGU_CHAIN_ID` | chain id |
| `DAEGU_CHAIN_APP_KEY` | outbound body의 `token` 값으로 사용하는 app key |
| `DAEGU_CHAIN_TOKEN` | app key fallback |
| `DID_SERVER_BASE_URL` | DID 서버 base URL |
| `DID_CREATE_PATH` | DID 생성 path, 기본 `/did/create` |
| `DID_ISSUE_VC_PATH` | VC 발급 path, 기본 `/did/issue-vc` |
| `DID_VERIFY_VC_PATH` | VC 검증 path, 기본 `/did/verify-vc` |
| `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_TEMPLATE_ID` | 로그인 VC audience/template id |
| `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_VALID_DAYS` | 로그인 VC 유효일 |
| `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS` | 리워드 회수 수신 owner address |
| `DAEGU_CHAIN_TOKEN_SYMBOL` | token server 토큰 생성 심볼 |
| `DAEGU_CHAIN_TOKEN_DECIMALS` | token decimals |
| `USER_REWARD_TOKEN_TRANSFER_ENABLED` | 실제 토큰 전송 활성화 여부 |

### 7.2 첨부 문서의 DID 서버 환경변수

첨부 문서의 Flask DID 서버는 다음 설정을 요구한다.

| 환경변수 | 설명 |
| --- | --- |
| `DCHAIN_BASE_URL` | D-chain API Base URL |
| `DCHAIN_API_TOKEN` | D-chain API 인증 토큰 |
| `DCHAIN_CHAIN_NAME` | 체인명 |
| `DCHAIN_OWNER_ADDR` | 관리자 지갑 주소 |
| `DCHAIN_OWNER_PRIVATE` | 관리자 개인키 |
| `DCHAIN_OWNER_DID` | 관리자 DID |
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | DID 서버 MySQL 설정 |
| `APP_LOG_DIR` | 로그 저장 경로 |
| `DID_DATA_DIR` | DID 파일 저장 경로 |

## 8. 비기능 요구사항

### NFR-001 보안

- API 토큰, DB 비밀번호, JWT secret, 관리자 private key는 저장소에 커밋하지 않아야 한다.
- SOH API는 DaeguChain app key를 환경변수에서 읽어 outbound request body의 `token` 필드에 주입해야 한다.
- SOH API는 사용자 DID private key를 읽거나 저장하지 않아야 한다.
- DID 서버가 private key를 저장하는 경우 최소 권한과 암호화 저장 정책을 적용해야 한다.
- 관리자 토큰 생성/전송 API에는 인증/인가 정책을 적용해야 한다.

### NFR-002 장애 처리

- DID 서버 호출 실패 시 SOH 회원가입은 실패해야 한다.
- VC 발급 실패는 credential 상태를 `FAILED`로 표시해야 한다.
- VC 검증 실패 시 DID 로그인은 거부되어야 한다.
- 외부 토큰 전송 실패 시 transaction 상태를 실패로 남겨야 한다.
- 운영 환경에서는 내부 예외 메시지를 그대로 노출하지 않는 방향을 권장한다.

### NFR-003 로그

- DID provisioning 실패는 사용자 ID와 함께 warning log로 기록한다.
- credential 발급 실패는 warning log로 기록한다.
- 외부 API 장애는 추적 가능한 transaction 상태와 로그를 남긴다.

### NFR-004 호환성

- DID 서버는 `/did/create`, `/did/issue-vc`, `/did/verify-vc` path를 제공해야 한다.
- SOH API는 DID 서버 응답 필드명이 `did`, `DID`, `address`, `credentialJwt`, `jwt`, `vc_jwt` 등으로 달라도 가능한 범위에서 추출해야 한다.
- DaeguChain API base URL이 `/mitum`으로 끝나는 환경을 지원해야 한다.

## 9. 원문 요구사항 대비 현재 구현 매핑

| 원문 ID | 원문 요구사항 | 현재 SOH API 상태 | 비고 |
| --- | --- | --- | --- |
| FR-001 | DID 생성 | 연동 구현 | DID 서버 호출 + wallet address 보강 |
| FR-002 | DID 목록 조회 | 외부/부분 | DID 서버 직접 기능. SOH는 DaeguChain account-list 프록시만 보유 |
| FR-003 | DID 문서 조회 | 외부 | SOH 직접 미연동 |
| FR-004 | DID 삭제 | 외부 | SOH 직접 미연동 |
| FR-005 | VC 발급 | 구현 | 로그인 VC 발급 구현 |
| FR-006 | VC 검증 | 구현 | DID 로그인에서 검증 |
| FR-007 | VP 발급 | 외부/미연동 | SOH 직접 미연동 |
| FR-008 | VP 검증 | 외부/미연동 | SOH 직접 미연동 |
| FR-009 | 토큰 생성 | 구현 | token-20 프록시 및 관리자 reward token 생성 |
| FR-010 | 토큰 전송 | 구현 | token-20 프록시 및 reward token transfer |
| FR-011 | 토큰 잔액 조회 | 구현 | token-20 balance |
| FR-012 | 토큰 승인 | 구현 | token-20 approve |
| FR-013 | 위임 전송 및 회수 | 부분 구현 | transfer-from 프록시, reward reclaim 별도 구현 |
| FR-014 | 토큰 목록 조회 | 구현 | token-20 list, admin token list |
| FR-015 | 로컬 토큰 목록 조회 | 부분 대체 | SOH는 token server 목록과 default reward token options 사용 |
| FR-016 | 토큰 발행량 조회 | 구현 | token-20 total-supply |
| FR-017 | 토큰 허용량 조회 | 구현 | token-20 allowance |
| FR-018 | 토큰 추가 발행 | 구현 | token-20 mint |
| FR-019 | 토큰 소각 | 구현 | token-20 burn |
| FR-020 | 토큰 업로드 | 구현 | token-20 upload |
| FR-021 | D-chain 공통 API 프록시 | 부분 구현 | account 계열, block/basic/point/nft/timestamp 등 별도 컨트롤러 포함 |
| FR-022 | `/mitum` Prefix 처리 | 구현 | `DaeguChainClient`에서 base URL 기준 보정 |
| FR-023 | 테스트 웹 화면 | 프론트 별도 | SOH Frontend에서 DID 회원가입/지갑조회/구강운동 UI 제공 |

## 10. 검수 기준

| 항목 | 검수 기준 |
| --- | --- |
| DID 회원가입 | `/login/signUp/did` 호출 후 user DID, credential 상태, wallet address가 저장된다 |
| DID 생성 연동 | DID 서버 `/did/create` 호출 요청에 `label=userLoginIdentifier`가 포함된다 |
| 지갑주소 보강 | DID 서버 응답에 address가 없어도 DaeguChain account create로 wallet address가 저장된다 |
| VC 발급 | `/did/issue-vc` 응답의 VC-JWT가 user에 저장된다 |
| VC 검증 | `/login/did` 호출 시 `/did/verify-vc` 결과가 유효해야 로그인된다 |
| 토큰 생성 | 허용 reward token name으로 관리자 token create가 성공한다 |
| 토큰 지급 | 구강운동 reward transaction이 idempotency key 기준으로 중복 지급되지 않는다 |
| 토큰 회수 | 필수 5개 토큰 수령 후 회수 요청이 성공하고 중복 요청은 skip된다 |
| 보안 | 저장소에 실제 app key, DB 비밀번호, private key가 포함되지 않는다 |
| 환경 | dev/prod 환경에서 DID 서버 URL, app key, token owner address가 분리 설정된다 |

## 11. 미해결 항목 및 개선 요구사항

- DID 서버의 DID 목록/문서 조회/삭제를 SOH 관리자 화면에 노출할지 정책 결정이 필요하다.
- VP 발급/검증을 실제 서비스 인증 흐름에 사용할지 결정이 필요하다.
- DID 서버 private key 저장 방식은 암호화 및 접근 권한 검토가 필요하다.
- token server `/token/transfer`가 SOH reward 지급/회수 payload를 실제 처리하는지 end-to-end 검증이 필요하다.
- 기존 `LOCAL_RECORDED` 리워드 데이터를 실제 토큰 지급/회수 대상으로 보정할지 운영 정책이 필요하다.
- DaeguChain/token server 외부 API timeout 설정을 명시적으로 구성하는 개선이 필요하다.
- 관리자 토큰 생성/전송 계열 API의 권한 정책을 점검해야 한다.
- REST Docs 또는 OpenAPI 문서에 DID 회원가입, reward wallet, token transfer 흐름을 최신 상태로 반영해야 한다.
