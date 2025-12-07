# HAProxy 이중화 구성 완료 (최종)

## 구성 정보

### HAProxy 인스턴스

| 이름 | Instance ID | Private IP | AZ | 키페어 | 상태 |
|------|-------------|------------|-----|--------|------|
| haproxy-db-1 | i-0f09e919cfe7dbf5b | 10.1.1.134 | ap-northeast-2a | test-ec2-ssh-key | running |
| haproxy-db-2 | i-07efa4b40ab39f358 | 10.1.1.182 | ap-northeast-2c | test-ec2-ssh-key | running |

### 백엔드 구성

**Oracle (온프레미스)**
- 주소: 10.1.5.18:1521
- 데이터베이스: oracle19c
- Weight: 100 (활성)
- 상태: UP

**PostgreSQL (AWS RDS)**
- 주소: target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com:5432
- Weight: 0 (대기)
- 상태: UP

## HAProxy 설정 파일

```bash
# /etc/haproxy/haproxy.cfg (양쪽 동일)

global
    log /dev/log local0
    log /dev/log local1 notice
    user haproxy
    group haproxy
    daemon
    maxconn 4096

defaults
    log     global
    mode    tcp
    option  tcplog
    option  dontlognull
    timeout connect 10s
    timeout client  1m
    timeout server  1m

# Stats page
listen stats
    bind *:8404
    mode http
    stats enable
    stats uri /
    stats refresh 10s
    stats admin if TRUE

# Database proxy
listen database
    bind *:5432
    mode tcp
    option tcp-check
    balance roundrobin
    
    # Oracle backend (initial - weight 100)
    server oracle 10.1.5.18:1521 check weight 100
    
    # PostgreSQL backend (prepared - weight 0)
    server postgresql target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com:5432 check weight 0
```

## SSH 접속

```bash
# HAProxy-1
ssh -i dk.pem ec2-user@10.1.1.134

# HAProxy-2
ssh -i dk.pem ec2-user@10.1.1.182
```

## 상태 확인

```bash
# HAProxy 상태
sudo systemctl status haproxy

# Stats 페이지 (브라우저 또는 curl)
curl http://10.1.1.134:8404
curl http://10.1.1.182:8404

# 백엔드 상태 확인
curl -s http://10.1.1.134:8404 | grep -A 3 'database'
```

## DB 전환 방법

### Oracle → PostgreSQL 전환

```bash
# 1. HAProxy 설정 변경 (양쪽 모두)
ssh -i dk.pem ec2-user@10.1.1.134
sudo vi /etc/haproxy/haproxy.cfg

# 변경:
server oracle 10.1.5.18:1521 check weight 0
server postgresql target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com:5432 check weight 100

# 2. HAProxy 리로드 (무중단)
sudo systemctl reload haproxy

# 3. 상태 확인
curl http://localhost:8404
```

### PostgreSQL → Oracle 롤백

```bash
# 1. HAProxy 설정 복구
sudo vi /etc/haproxy/haproxy.cfg

# 변경:
server oracle 10.1.5.18:1521 check weight 100
server postgresql target-postgresql.cluster-cmjs2qxaojzn.ap-northeast-2.rds.amazonaws.com:5432 check weight 0

# 2. HAProxy 리로드
sudo systemctl reload haproxy
```

## 애플리케이션 설정

### Route53 Private Hosted Zone 사용 시

```yaml
# application.yml
spring:
  datasource:
    # Oracle 연결 (HAProxy 통해)
    url: jdbc:oracle:thin:@db-proxy.internal.local:5432/oracle19c
    username: mesuser
    password: mespass
    driver-class-name: oracle.jdbc.OracleDriver
```

**전환 후 (HAProxy가 PostgreSQL로 라우팅)**
```yaml
# 애플리케이션 코드 변경 없음!
# HAProxy가 자동으로 PostgreSQL로 연결
spring:
  datasource:
    url: jdbc:postgresql://db-proxy.internal.local:5432/mesdb
    username: mesuser
    password: mespass
    driver-class-name: org.postgresql.Driver
```

### 직접 IP 사용 시

```yaml
# application.yml
spring:
  datasource:
    # Oracle 연결 (HAProxy 통해)
    url: jdbc:oracle:thin:@10.1.1.134:5432/oracle19c
    username: mesuser
    password: mespass
    driver-class-name: oracle.jdbc.OracleDriver
```

## Route53 Private Hosted Zone 설정 (선택)

### 1. Private Hosted Zone 생성

```bash
aws route53 create-hosted-zone \
  --name internal.local \
  --vpc VPCRegion=ap-northeast-2,VPCId=vpc-083e1cd1c3147138a \
  --caller-reference $(date +%s) \
  --hosted-zone-config PrivateZone=true \
  --region ap-northeast-2
```

### 2. Health Check 생성

```bash
aws route53 create-health-check \
  --health-check-config \
    IPAddress=10.1.1.134,Port=5432,Type=TCP,RequestInterval=30,FailureThreshold=3 \
  --region ap-northeast-2
```

### 3. Failover 레코드 생성

**Primary 레코드**
```json
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "db-proxy.internal.local",
      "Type": "A",
      "SetIdentifier": "haproxy-primary",
      "Failover": "PRIMARY",
      "TTL": 10,
      "ResourceRecords": [{"Value": "10.1.1.134"}],
      "HealthCheckId": "<health-check-id>"
    }
  }]
}
```

**Secondary 레코드**
```json
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "db-proxy.internal.local",
      "Type": "A",
      "SetIdentifier": "haproxy-secondary",
      "Failover": "SECONDARY",
      "TTL": 10,
      "ResourceRecords": [{"Value": "10.1.1.182"}]
    }
  }]
}
```

## 마이그레이션 시나리오

### 시나리오 1: 동일 DB 이관 (Oracle → Oracle)

```
[Before]
EC2 앱 → HAProxy → 온프레미스 Oracle

[After]
EC2 앱 → HAProxy → RDS Oracle

장점:
- HAProxy 설정만 변경 (수초)
- 애플리케이션 재시작 불필요
- 빠른 롤백 가능
```

### 시나리오 2: 다른 DB 이관 (Oracle → PostgreSQL)

```
[Before]
온프레미스 앱 (Oracle용) → 온프레미스 Oracle

[After]
EC2 앱 (PostgreSQL용) → Aurora PostgreSQL

주의:
- HAProxy 불필요 (애플리케이션 자체가 다름)
- DNS 변경만으로 충분
```

### 시나리오 3: 점진적 전환

```
[Phase 1]
앱 A, B → HAProxy → 온프레미스 Oracle
앱 C → HAProxy → RDS PostgreSQL

[Phase 2]
앱 A, B, C → HAProxy → RDS PostgreSQL

장점:
- 앱별 순차 전환
- 문제 발생 시 일부만 롤백
```

## 주의사항

### 1. 온프레미스 접근

**Private Hosted Zone은 VPC 내부에서만 사용 가능**
- 온프레미스 앱이 접근하려면: VPN/DX + Route53 Resolver 필요
- 대안: 온프레미스 DNS에 직접 등록

### 2. 프로토콜 차이

**Oracle과 PostgreSQL은 프로토콜이 다름**
- HAProxy는 TCP 레벨이므로 프로토콜 무관
- 하지만 애플리케이션 코드는 변경 필요

### 3. 포트 차이

**Oracle (1521) vs PostgreSQL (5432)**
- HAProxy는 5432 포트로 통일
- 백엔드는 각각의 포트 사용

## 비용

- **EC2 (t3.micro × 2)**: ~$15/월
- **Route53 Private Hosted Zone**: $0.50/월 (선택)
- **총**: ~$15.5/월

## 정리 (마이그레이션 완료 후)

```bash
# 1. HAProxy 인스턴스 종료
aws ec2 terminate-instances \
  --instance-ids i-0f09e919cfe7dbf5b i-07efa4b40ab39f358 \
  --region ap-northeast-2

# 2. 보안 그룹 삭제
aws ec2 delete-security-group \
  --group-id sg-0c4441fc57913af64 \
  --region ap-northeast-2

# 3. Route53 Hosted Zone 삭제 (사용한 경우)
aws route53 delete-hosted-zone --id <hosted-zone-id>
```

## 요약

✅ **HAProxy 이중화 구성 완료**
- 2대의 HAProxy 인스턴스 (AZ-a, AZ-c)
- Oracle 백엔드 (활성, weight 100)
- PostgreSQL 백엔드 (대기, weight 0)

✅ **전환 방법**
- HAProxy 설정 변경 (weight 조정)
- 무중단 리로드 (수초)
- 빠른 롤백 가능

✅ **이중화 옵션**
- Route53 Failover (Primary/Secondary)
- 또는 직접 IP 사용

✅ **사용 사례**
- 동일 DB 이관 시 유용
- 점진적 전환 필요 시
- 빠른 롤백 필요 시
