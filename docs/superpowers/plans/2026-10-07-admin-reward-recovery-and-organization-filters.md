# 관리자 지급 복구와 기관 필터 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 슈퍼관리자가 실제 거래 증명을 검증해 기존 지급을 복구하고 7개 조회 페이지에서 기관별 사용자를 볼 수 있게 한다.

**Architecture:** 기존 RewardTransferReceipt 검증과 관리자 토큰 지급 내역을 확장한다. 복구는 preview/confirm 단계로 대구체인에서 증명을 직접 조회하며 토큰을 보내지 않는다. 기관 필터는 전체 응답 화면에서는 정확 일치 필터를, 서버 페이지 목록에서는 조회/count 전 필터를 적용한다.

**Tech Stack:** Java 17, Spring Boot/JPA, React/TypeScript/Chakra UI, Gradle, Vitest.

**Spec:** 사용자가 승인한 대화의 거래 확인·복구 설계와 7개 페이지 기관 드롭다운 요청. 기존 작업 브랜치와 사용자 변경을 보존한다.

## Global Constraints

- 슈퍼관리자 DB 권한을 확인한다. 일반 기관 관리자에게 다른 기관 접근 권한을 추가하지 않는다.
- 토큰 재전송·회수·발행·운영 DB 변경 없이 기능을 구현하고 검증한다.
- 관리자/수신 주소·계약·수량·fact hash·height·in_state가 모두 일치해야 한다.
- 거래의 블록 시각이 기존 전송 시점 이전이면 복구하지 않는다. 식별 정보가 부족하면 검토 상태를 유지한다.
- 복구 증명은 한 지급 건만 점유한다. DB의 증명 기본키와 트랜잭션 잠금으로 중복 반영을 막는다.
- 기관 기본값 전체, 기관 미지정 옵션, 정확 일치, 기존 검색/페이지/실패 kind 유지.
- prod push/배포 보류는 유지한다. 사용자 변경을 stage하지 않는다.

## Review Focus

- 증명 조회 실패·거절·다른 계약/수신자이면 상태와 내부 잔액을 바꾸지 않는다.
- 미리보기 후 지갑/지급 데이터 변경, 취소/회수 상태이면 확정을 거절한다.
- 같은 해시의 다른 지급 점유와 동시 클릭을 차단한다. 동일 완료 건 재요청은 잔액을 다시 올리지 않는다.
- 기관 미지정/null/공백, 기관명 유사 문자열, 페이지 크기/검색의 조합을 검증한다.
- 오래된 UI 응답이 다른 선택 건/수정된 해시에 적용되지 않도록 확인한다.

## Tasks

### Task 1: 거래 증명 복구 API와 저장 보호
- [x] 권한, preview, 성공/불일치/타임아웃, 해시 재사용, 중복 확정, 이전 블록, 지갑 변경 테스트 작성 및 RED 확인.
- [x] AdminRewardTransferRecoveryService, recovery DTO/controller, RewardTransferRecoveryEvidence/entity/repository 구현.
- [x] 기존 지급·지갑·사용자 잠금 순서를 유지하고 조회 중 DB 잠금을 해제한다. 확정은 재검증 후 원자적으로 반영한다.
- [x] 관련 Gradle 테스트 및 bootJar 검증.

### Task 2: 거래 증명 복구 관리자 화면
- [x] recovery helper/API 계약과 응답 경합 테스트 RED 확인.
- [x] AdminTokenManagementPage의 검토 필요 건 버튼 및 거래 확인/확정 모달 추가.
- [x] 성공 후 기존 목록 갱신. 프론트 테스트/ESLint/build와 viewport 확인.

### Task 3: 기관 필터
- [x] 기관 정확 일치/미지정/페이지 이전 필터 테스트 RED 확인.
- [x] 전체 사용자 기관 옵션 API와 사용자 목록 QueryDSL 필터, 로그 기관 정보 추가.
- [x] 사용자 관리/진도/DID 리워드/대구체인 로그에 공통 드롭다운 적용. 기존 시청·실패·설문 필터 보존 및 검증.
- [x] API/프론트 영향 테스트와 build 확인.

### Task 4: 통합 검증과 기록
- [x] 전체 관련 테스트/빌드, UI mock smoke, diff 및 권한/중복 보호 리뷰.
- [x] README/AGENTS 관련 계약과 dated DOCX 기록. 실제 운영 지급 복구는 별도 실행하지 않음.
- [x] 이번 파일만 각 저장소 로컬 커밋. push/배포 상태 및 미확인 실제 거래를 보고.

## 실행 메모

- 승인된 기존 복구 설계를 구현한다. 기관 옵션/정확 일치/기존 페이지 보존은 요청된 기관별 조회의 구현 선택이다.
- 파일 영역이 겹치지 않는 기관 필터/복구 UI를 dispatching-parallel-agents 스킬로 분리한다. root는 복구 API/문서/통합 검증을 맡는다.
- 기존 prod 체크아웃에서 관련 파일만 수정한다. 브랜치 전환/병합과 기존 사용자 파일 변경은 하지 않는다.

## 검증 결과

- Java17 전체 테스트 487개, 실패·오류·건너뛰기 0, bootJar 통과. 관련 테스트 79개도 통과.
- Frontend Vitest 17파일 82개, TypeScript/Vite build 통과. 관련 ESLint 오류 0, 기존 경고 5.
- 복구 PC/태블릿/휴대폰과 기관 7화면 PC/휴대폰 mock smoke 통과. 운영 요청·데이터 변경 없음.
- 코드 리뷰 3항목은 RED 재현 후 수정·재검토 완료. 자동/수동 증명 공용 점유, tx 내부 완료 DTO, 기관 늦은 응답 보호.
- DOCX 4페이지를 숨김 Word PDF 및 packaged PNG renderer로 확인. Windows 번들 LibreOffice 없음. 양쪽 DOCX 동일.
- 기존 prod 브랜치 로컬 커밋. 이전 push/배포 보류 유지. 실제 fact_hash는 제공자로부터 확보해야 함.
