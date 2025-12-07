# Route53 + HAProxy 이중화 구성 가이드

## 개요

Route53 Private Hosted Zone과 CloudWatch Alarm을 활용한 HAProxy 이중화 구성으로 자동 장애조치(Failover)를 구현합니다.

## 구성 정보

### HAProxy 인스턴스

| 이름 | Instance ID | Private IP | AZ | 역할 |
|------|-------------|------------|-----|------|
| haproxy-db-1 | i-0f09e919cfe7dbf5b | 10.1.1.134 | ap-northeast-2a | Primary |
| haproxy-db-2 | i-07efa4b40ab39f358 | 10.1.1.182 | ap-northeast-2c | Secondary |

### Route53 설정

- **Private Hosted Zone**: VPC 내부 전용
- **도메인**: `proxy.mes.db`
- **Routing Policy**: Failover (Primary/Secondary)

### 백엔드 DB

- **Oracle**: 10.1.5.18:1521 (온프레미스)
- **PostgreSQL**: target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com:5432 (대기)
- **HAProxy 리스닝 포트**: 5432

---

## 1단계: CloudWatch Alarm 생성

### 1.1 HAProxy-1 Alarm 생성

**EC2 콘솔 접속**
1. EC2 → Instances (인스턴스)
2. HAProxy-1 인스턴스 선택 (i-0f09e919cfe7dbf5b)
3. **Actions** → **Monitor and troubleshoot** → **Manage CloudWatch alarms**
4. **Create an alarm** 클릭

**설정:**
- **Alarm notification**: 체크 해제 (SNS 불필요)
- **Alarm action**: **No action**
- **Alarm thresholds**:
  - **Group samples by**: `Average`
  - **Type of data to sample**: **Status check failed: instance**
  - **Alarm when**: `>= 1`
  - **Consecutive period**: `2`
  - **Period**: `1 minute`
- **Alarm name**: `haproxy-1-health-alarm`

5. **Create** 클릭

### 1.2 HAProxy-2 Alarm 생성

위 과정을 반복하여 HAProxy-2용 Alarm 생성:
- 인스턴스: i-07efa4b40ab39f358
- Alarm name: `haproxy-2-health-alarm`

---

## 2단계: Route53 Private Hosted Zone 생성

### 2.1 Hosted Zone 생성

**Route53 콘솔 접속**
1. Route53 → **Hosted zones** 클릭
2. **Create hosted zone** 클릭

**설정:**
- **Domain name**: `mes.db`
- **Description**: `MES Application HAProxy Load Balancer`
- **Type**: **Private hosted zone** ✓
- **VPCs to associate**:
  - **Region**: `ap-northeast-2`
  - **VPC ID**: HAProxy가 속한 VPC 선택

3. **Create hosted zone** 클릭

---

## 3단계: Route53 Health Check 생성

### 3.1 HAProxy-1 Health Check

**Route53 콘솔**
1. Route53 → **Health checks** → **Create health check**

**설정:**
- **Name**: `haproxy-1-health-check`
- **What to monitor**: **State of CloudWatch alarm** ✓
- **CloudWatch alarm**:
  - **Region**: `ap-northeast-2`
  - **Alarm**: `haproxy-1-health-alarm` 선택
- **Invert health check status**: 체크 안 함

2. **Create health check** 클릭

### 3.2 HAProxy-2 Health Check

위 과정 반복:
- **Name**: `haproxy-2-health-check`
- **Alarm**: `haproxy-2-health-alarm` 선택

---

## 4단계: Route53 Failover 레코드 생성

### 4.1 Primary 레코드 (HAProxy-1)

**Route53 Hosted Zone 선택**
1. 생성한 `mes.db` Hosted Zone 클릭
2. **Create record** 클릭

**설정:**
- **Record name**: `proxy`
- **Record type**: `A`
- **Value**: `10.1.1.134` (HAProxy-1 IP)
- **TTL**: `60`
- **Routing policy**: `Failover`
- **Failover record type**: `Primary` ✓
- **Health check ID**: `haproxy-1-health-check` 선택
- **Record ID**: `haproxy-primary`

3. **Create records** 클릭

### 4.2 Secondary 레코드 (HAProxy-2)

**같은 이름으로 추가 레코드 생성**
1. **Create record** 클릭

**설정:**
- **Record name**: `proxy` (동일)
- **Record type**: `A`
- **Value**: `10.1.1.182` (HAProxy-2 IP)
- **TTL**: `60`
- **Routing policy**: `Failover`
- **Failover record type**: `Secondary` ✓
- **Health check ID**: `haproxy-2-health-check` 선택
- **Record ID**: `haproxy-secondary`

2. **Create records** 클릭

---

## 5단계: 애플리케이션 설정 변경

### 5.1 application.yml 수정

**파일 경로**: `/home/ec2-user/projects/autoever/src/main/resources/application.yml`

**변경 전:**
```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//10.1.5.18:1521/oracle19c
    username: mesuser
    password: mespass
    driver-class-name: oracle.jdbc.OracleDriver
```

**변경 후:**
```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//proxy.mes.db:5432/oracle19c
    username: mesuser
    password: mespass
    driver-class-name: oracle.jdbc.OracleDriver
```

### 5.2 애플리케이션 재시작

```bash
# 프로젝트 디렉토리로 이동
cd /home/ec2-user/projects/autoever

# 기존 프로세스 종료
pkill -f "mes-0.0.1-SNAPSHOT.jar"

# 재빌드
./gradlew clean build -x test

# 백그라운드 실행
nohup java -jar build/libs/mes-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 로그 확인
tail -f app.log
```

---

## 구성 완료 확인

### DNS 조회 테스트

```bash
# VPC 내부 인스턴스에서 실행
nslookup proxy.mes.db

# 예상 결과:
# Name:   proxy.mes.db
# Address: 10.1.1.134
```

### 애플리케이션 접속 테스트

```bash
# 애플리케이션 접속
curl http://localhost:8080

# DB 연결 확인 (로그)
tail -f app.log | grep -i "hikari"
```

### Health Check 상태 확인

**Route53 콘솔**
- Route53 → Health checks
- 두 Health Check 모두 **Healthy** 상태 확인

**CloudWatch 콘솔**
- CloudWatch → Alarms
- 두 Alarm 모두 **OK** 상태 확인

---

## 장애조치 테스트

### 1. HAProxy-1 중지

```bash
# EC2 콘솔에서 HAProxy-1 인스턴스 중지
# 또는 SSH 접속 후
sudo systemctl stop haproxy
```

### 2. 자동 전환 확인 (2-3분 소요)

**CloudWatch Alarm**
- `haproxy-1-health-alarm` 상태: **In alarm**

**Route53 Health Check**
- `haproxy-1-health-check` 상태: **Unhealthy**

**DNS 조회**
```bash
nslookup proxy.mes.db
# 결과: 10.1.1.182 (HAProxy-2로 전환)
```

**애플리케이션**
- 계속 정상 동작 (자동으로 HAProxy-2 사용)

### 3. HAProxy-1 복구

```bash
# HAProxy-1 재시작
sudo systemctl start haproxy

# 또는 EC2 인스턴스 시작
```

2-3분 후 자동으로 Primary(HAProxy-1)로 복귀

---

## 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│                    VPC (10.1.0.0/16)                    │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │         Route53 Private Hosted Zone              │  │
│  │              proxy.mes.db                        │  │
│  │                                                  │  │
│  │  Primary (10.1.1.134) ← Health Check 1          │  │
│  │  Secondary (10.1.1.182) ← Health Check 2        │  │
│  └──────────────────────────────────────────────────┘  │
│                        ↓                                │
│  ┌─────────────────────────────────────────────────┐   │
│  │  CloudWatch Alarms                              │   │
│  │  - haproxy-1-health-alarm (StatusCheckFailed)   │   │
│  │  - haproxy-2-health-alarm (StatusCheckFailed)   │   │
│  └─────────────────────────────────────────────────┘   │
│                        ↓                                │
│  ┌──────────────┐              ┌──────────────┐        │
│  │  HAProxy-1   │              │  HAProxy-2   │        │
│  │  10.1.1.134  │              │  10.1.1.182  │        │
│  │  (Primary)   │              │  (Secondary) │        │
│  │  AZ-a        │              │  AZ-c        │        │
│  └──────┬───────┘              └──────┬───────┘        │
│         │                             │                 │
│         └──────────┬──────────────────┘                 │
│                    ↓                                    │
│         ┌──────────────────────┐                        │
│         │   Oracle Database    │                        │
│         │   10.1.5.18:1521     │                        │
│         │   (온프레미스)        │                        │
│         └──────────────────────┘                        │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │  MES Application (Spring Boot)                  │   │
│  │  jdbc:oracle:thin:@//proxy.mes.db:5432/oracle19c│   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

## 주요 특징

### 자동 장애조치
- HAProxy 인스턴스 장애 시 자동 전환 (2-3분)
- 애플리케이션 코드 변경 불필요
- DNS 기반 투명한 전환

### 고가용성
- 2개 AZ에 분산 배치 (AZ-a, AZ-c)
- CloudWatch 기반 실시간 모니터링
- Health Check 자동 복구

### Private 환경
- VPC 내부 전용 DNS
- 외부 노출 없음
- 보안 강화

---

## 비용

| 항목 | 수량 | 월 비용 |
|------|------|---------|
| EC2 t3.micro | 2대 | ~$15 |
| Route53 Private Hosted Zone | 1개 | $0.50 |
| Route53 Health Check | 2개 | $1.00 |
| CloudWatch Alarm | 2개 | 무료 (10개까지) |
| **총계** | | **~$16.50** |

---

## 향후 PostgreSQL 전환 시

### HAProxy 설정만 변경

```bash
# HAProxy 설정 변경 (양쪽 모두)
ssh -i dk.pem ec2-user@10.1.1.134
sudo vi /etc/haproxy/haproxy.cfg

# Weight 변경
server oracle 10.1.5.18:1521 check weight 0
server postgresql target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com:5432 check weight 100

# 무중단 리로드
sudo systemctl reload haproxy
```

### 애플리케이션 설정 변경

```yaml
spring:
  datasource:
    url: jdbc:postgresql://proxy.mes.db:5432/mesdb
    username: mesuser
    password: mespass
    driver-class-name: org.postgresql.Driver
```

**장점:**
- 애플리케이션 재배포만으로 전환 완료
- 빠른 롤백 가능 (HAProxy 설정 복구)
- 점진적 전환 가능

---

## 트러블슈팅

### DNS 조회 실패

**증상:**
```bash
nslookup proxy.mes.db
# Server can't find proxy.mes.db: NXDOMAIN
```

**해결:**
1. Route53 Hosted Zone이 올바른 VPC와 연결되었는지 확인
2. 인스턴스가 같은 VPC에 있는지 확인
3. VPC DNS 설정 확인 (enableDnsHostnames, enableDnsSupport)

### Health Check Unhealthy

**증상:**
- Route53 Health Check가 계속 Unhealthy

**해결:**
1. CloudWatch Alarm 상태 확인
2. HAProxy 인스턴스 상태 확인 (EC2 콘솔)
3. HAProxy 프로세스 확인: `sudo systemctl status haproxy`

### 애플리케이션 DB 연결 실패

**증상:**
```
HikariPool-1 - Exception during pool initialization
```

**해결:**
1. DNS 조회 확인: `nslookup proxy.mes.db`
2. HAProxy 포트 확인: `telnet proxy.mes.db 5432`
3. 보안 그룹 확인 (5432 포트 허용)
4. HAProxy 백엔드 상태: `curl http://10.1.1.134:8404`

---

## 참고 문서

- [HAProxy 설정 가이드](./haproxy/HAPROXY_FINAL_SETUP.md)
- [HAProxy 명령어](./haproxy/HAPROXY_COMMANDS.md)
- [프로젝트 README](./README.md)

---

## 작성 정보

- **작성일**: 2025-12-03
- **구성 환경**: AWS ap-northeast-2 (Seoul)
- **테스트 완료**: ✅
