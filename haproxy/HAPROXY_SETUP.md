# HAProxy 이중화 구성 완료

## 구성 정보

### HAProxy 인스턴스

| 이름 | Instance ID | Private IP | AZ | 상태 |
|------|-------------|------------|-----|------|
| haproxy-db-1 | i-0a9f5cb0853f7ab1b | 10.1.1.132 | ap-northeast-2a | running |
| haproxy-db-2 | i-0fd4d4154758e8cc5 | 10.1.1.181 | ap-northeast-2c | running |

### 네트워크 구성

- **VPC**: my-vpc (vpc-083e1cd1c3147138a, 10.1.0.0/16)
- **Subnet 1**: my-private-subnet-app-a (subnet-03dd67f1a36e1dbc8, AZ-a)
- **Subnet 2**: my-private-subnet-app-c (subnet-0984f7255cf3af733, AZ-c)
- **Security Group**: haproxy-db-sg (sg-0c4441fc57913af64)

### 보안 그룹 규칙

**Inbound**:
- TCP 5432 (PostgreSQL) from 10.1.0.0/16
- TCP 22 (SSH) from 10.1.0.0/16
- TCP 8404 (HAProxy Stats) from 10.1.0.0/16

**Outbound**: All traffic

## HAProxy 설정

### 현재 백엔드 구성

```
listen database
    bind *:5432
    mode tcp
    option tcp-check
    balance roundrobin
    
    # Oracle backend (active)
    server oracle 10.1.5.18:1521 check weight 100
    
    # Aurora PG backend (prepared, inactive)
    # server aurora <aurora-endpoint>:5432 check weight 0
```

### Stats 페이지

- **URL**: http://<haproxy-ip>:8404
- 실시간 백엔드 상태 모니터링

## 이중화 구성 방법

### Option 1: Route53 Failover (권장)

```bash
# 1. Private Hosted Zone 생성
aws route53 create-hosted-zone \
  --name internal.local \
  --vpc VPCRegion=ap-northeast-2,VPCId=vpc-083e1cd1c3147138a \
  --caller-reference $(date +%s) \
  --hosted-zone-config PrivateZone=true

# 2. Health Check 생성 (Primary)
aws route53 create-health-check \
  --health-check-config \
    IPAddress=10.1.1.132,Port=5432,Type=TCP,ResourcePath=/

# 3. Failover 레코드 생성
# Primary: 10.1.1.132 (haproxy-db-1)
# Secondary: 10.1.1.181 (haproxy-db-2)
```

**DNS 이름**: `db-proxy.internal.local`

### Option 2: NLB (가장 간단)

```bash
# 1. NLB 생성
aws elbv2 create-load-balancer \
  --name haproxy-nlb \
  --type network \
  --scheme internal \
  --subnets subnet-03dd67f1a36e1dbc8 subnet-0984f7255cf3af733

# 2. Target Group 생성
aws elbv2 create-target-group \
  --name haproxy-tg \
  --protocol TCP \
  --port 5432 \
  --vpc-id vpc-083e1cd1c3147138a \
  --health-check-protocol TCP

# 3. HAProxy 인스턴스 등록
aws elbv2 register-targets \
  --target-group-arn <target-group-arn> \
  --targets Id=i-0a9f5cb0853f7ab1b Id=i-0fd4d4154758e8cc5
```

## 사용 방법

### 1. 애플리케이션 설정 (마이그레이션 중)

```yaml
# application.yml
spring:
  datasource:
    # Route53 사용 시
    url: jdbc:postgresql://db-proxy.internal.local:5432/mesdb
    
    # 또는 NLB 사용 시
    url: jdbc:postgresql://<nlb-dns-name>:5432/mesdb
    
    username: mesuser
    password: mespass
```

### 2. DB 전환 절차

```bash
# Phase 1: Oracle 사용 (현재)
# HAProxy 설정: oracle weight 100, aurora weight 0

# Phase 2: Aurora PG로 전환
# 1. HAProxy 설정 수정 (양쪽 인스턴스)
sudo vi /etc/haproxy/haproxy.cfg

# 변경:
server oracle 10.1.5.18:1521 check weight 0
server aurora <aurora-endpoint>:5432 check weight 100

# 2. HAProxy 리로드 (무중단)
sudo systemctl reload haproxy

# 3. Stats 페이지에서 확인
curl http://10.1.1.132:8404
```

### 3. HAProxy 상태 확인

```bash
# SSH 접속 (Session Manager 사용)
aws ssm start-session --target i-0a9f5cb0853f7ab1b

# HAProxy 상태
sudo systemctl status haproxy

# 설정 검증
sudo haproxy -c -f /etc/haproxy/haproxy.cfg

# 로그 확인
sudo journalctl -u haproxy -f

# Stats 페이지
curl http://localhost:8404
```

## 마이그레이션 완료 후

### HAProxy 제거

```bash
# 1. 애플리케이션 설정 변경 (Aurora 직접 연결)
spring:
  datasource:
    url: jdbc:postgresql://<aurora-endpoint>:5432/mesdb

# 2. HAProxy 인스턴스 종료
aws ec2 terminate-instances \
  --instance-ids i-0a9f5cb0853f7ab1b i-0fd4d4154758e8cc5

# 3. Route53/NLB 제거
# 4. Security Group 제거
```

## 비용

- **EC2 (t3.micro)**: $0.0104/시간 × 2 = $0.0208/시간 (~$15/월)
- **Route53 Private Hosted Zone**: $0.50/월
- **NLB (선택)**: $0.0225/시간 (~$16/월)

**총 비용 (Route53 사용)**: ~$15.5/월
**총 비용 (NLB 사용)**: ~$31/월

## 다음 단계

1. ✅ HAProxy 인스턴스 2대 생성 완료
2. ⏳ Route53 Private Hosted Zone 생성 (또는 NLB)
3. ⏳ Health Check 설정
4. ⏳ 애플리케이션에서 테스트
5. ⏳ Aurora PG 백엔드 추가
6. ⏳ 전환 테스트

## 참고

- HAProxy는 TCP 모드로 동작하여 Oracle/PostgreSQL 프로토콜 차이 무관
- 모든 트래픽이 HAProxy를 경유하므로 레이턴시 +1~5ms
- 마이그레이션 완료 후 HAProxy 제거하여 직접 연결 권장
