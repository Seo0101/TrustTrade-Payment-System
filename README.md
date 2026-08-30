# TrustTrade

# TrustTrade Payment System

기존 팀 프로젝트 **TrustTrade**의 주문/결제 영역을 개인적으로 고도화한 프로젝트입니다.

단순한 결제 승인 기능 구현을 넘어, 실제 결제 시스템에서 발생할 수 있는
**중복 요청, 동시성 문제, 외부 PG 장애, 네트워크 오류, 결과 불확실성**을 고려하여
결제 흐름의 신뢰성과 데이터 정합성을 개선하는 것을 목표로 했습니다.

---

## 1. 프로젝트 개요

TrustTrade는 중고 거래 과정에서 구매자가 상품을 주문하고 결제를 진행할 수 있는 서비스입니다.

기존 주문/결제 기능을 기반으로 다음 문제를 중심으로 구조를 개선했습니다.

- 동일 요청에 의한 중복 주문/결제 생성
- 동일 결제에 대한 중복 승인 요청
- DB 트랜잭션 내부에서 외부 PG API를 호출하는 문제
- PG 응답을 받지 못했을 때 결제 상태를 확정할 수 없는 문제
- 서버와 PG 간 결제 상태 불일치
- 동시 요청에 따른 상태 변경 경쟁
- UNIQUE 제약 충돌 이후 복구 처리
- 결제 승인 결과의 명확한 성공/실패/미확정 상태 분리

---

## 2. 기술 스택

### Backend

- Java
- Spring Boot
- Spring Data JPA
- Hibernate
- MySQL
- Gradle

### Payment

- Toss Payments API
- Java HttpClient

### Database

- MySQL
- JPA Pessimistic Lock
- UNIQUE Constraint

### 기타

- Spring Scheduler
- Thymeleaf / Static HTML, JavaScript
- Flyway

---
