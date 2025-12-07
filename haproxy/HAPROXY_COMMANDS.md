# HAProxy 설치 및 이중화 구성 명령어

## 1. EC2 인스턴스 생성

### 보안 그룹 생성
```bash
# 보안 그룹 생성
aws ec2 create-security-group \
  --group-name haproxy-db-sg \
  --description "Security group for HAProxy DB proxy" \
  --vpc-id vpc-083e1cd1c3147138a \
  --region ap-northeast-2

# 보안 그룹 ID: sg-0c4441fc57913af64

# 인바운드 규칙 추가
aws ec2 authorize-security-group-ingress \
  --group-id sg-0c4441fc57913af64 \
  --ip-permissions \
    IpProtocol=tcp,FromPort=5432,ToPort=5432,IpRanges='[{CidrIp=10.1.0.0/16,Description="PostgreSQL from VPC"}]' \
    IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges='[{CidrIp=10.1.0.0/16,Description="SSH from VPC"}]' \
    IpProtocol=tcp,FromPort=8404,ToPort=8404,IpRanges='[{CidrIp=10.1.0.0/16,Description="HAProxy Stats"}]' \
  --region ap-northeast-2
```

### User Data 스크립트 준비
```bash
# /tmp/haproxy-userdata.sh 파일 생성
cat > /tmp/haproxy-userdata.sh << 'EOF'
#!/bin/bash
set -e

# Update system
dnf update -y

# Install HAProxy
dnf install -y haproxy

# Backup original config
cp /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.orig

# Create HAProxy configuration
cat > /etc/haproxy/haproxy.cfg << 'EOFCFG'
global
    log /dev/log local0
    log /dev/log local1 notice
    chroot /var/lib/haproxy
    stats socket /run/haproxy/admin.sock mode 660 level admin
    stats timeout 30s
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

# Database proxy (PostgreSQL port)
listen database
    bind *:5432
    mode tcp
    option tcp-check
    balance roundrobin
    
    # Oracle backend (initial - weight 100)
    server oracle 10.1.5.18:1521 check weight 100
    
    # Aurora PG backend (prepared - weight 0)
    # server aurora <aurora-endpoint>:5432 check weight 0
EOFCFG

# Enable and start HAProxy
systemctl enable haproxy
systemctl start haproxy

# Install telnet for testing
dnf install -y telnet nc

echo "HAProxy installation completed"
EOF

# Base64 인코딩
base64 -w 0 /tmp/haproxy-userdata.sh > /tmp/haproxy-userdata-base64.txt
```

### EC2 인스턴스 생성 (2대)
```bash
# HAProxy 인스턴스 1 (AZ-a)
aws ec2 run-instances \
  --image-id ami-0ad90ede5f7a6f599 \
  --instance-type t3.micro \
  --subnet-id subnet-03dd67f1a36e1dbc8 \
  --security-group-ids sg-0c4441fc57913af64 \
  --user-data file:///tmp/haproxy-userdata.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=haproxy-db-1},{Key=Role,Value=haproxy}]' \
  --region ap-northeast-2

# 결과: i-0a9f5cb0853f7ab1b (10.1.1.132)

# HAProxy 인스턴스 2 (AZ-c)
aws ec2 run-instances \
  --image-id ami-0ad90ede5f7a6f599 \
  --instance-type t3.micro \
  --subnet-id subnet-0984f7255cf3af733 \
  --security-group-ids sg-0c4441fc57913af64 \
  --user-data file:///tmp/haproxy-userdata.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=haproxy-db-2},{Key=Role,Value=haproxy}]' \
  --region ap-northeast-2

# 결과: i-0fd4d4154758e8cc5 (10.1.1.181)
```

### 인스턴스 상태 확인
```bash
aws ec2 describe-instances \
  --filters "Name=tag:Role,Values=haproxy" "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].[InstanceId,PrivateIpAddress,State.Name,Tags[?Key==`Name`].Value|[0]]' \
  --output table \
  --region ap-northeast-2
```

## 2. 이중화 구성

### Option 1: Route53 Failover (권장)

```bash
# 1. Private Hosted Zone 생성
aws route53 create-hosted-zone \
  --name internal.local \
  --vpc VPCRegion=ap-northeast-2,VPCId=vpc-083e1cd1c3147138a \
  --caller-reference $(date +%s) \
  --hosted-zone-config PrivateZone=true

# 결과에서 HostedZoneId 확인 (예: Z1234567890ABC)

# 2. Health Check 생성 (Primary용)
aws route53 create-health-check \
  --health-check-config \
    IPAddress=10.1.1.132,Port=5432,Type=TCP,RequestInterval=30,FailureThreshold=3 \
  --region ap-northeast-2

# 결과에서 HealthCheckId 확인 (예: abc123-def456)

# 3. Primary 레코드 생성
cat > /tmp/route53-primary.json << 'EOF'
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "db-proxy.internal.local",
      "Type": "A",
      "SetIdentifier": "haproxy-primary",
      "Failover": "PRIMARY",
      "TTL": 10,
      "ResourceRecords": [{"Value": "10.1.1.132"}],
      "HealthCheckId": "abc123-def456"
    }
  }]
}
EOF

aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file:///tmp/route53-primary.json

# 4. Secondary 레코드 생성
cat > /tmp/route53-secondary.json << 'EOF'
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "db-proxy.internal.local",
      "Type": "A",
      "SetIdentifier": "haproxy-secondary",
      "Failover": "SECONDARY",
      "TTL": 10,
      "ResourceRecords": [{"Value": "10.1.1.181"}]
    }
  }]
}
EOF

aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file:///tmp/route53-secondary.json

# 5. DNS 테스트
dig db-proxy.internal.local
nslookup db-proxy.internal.local
```

### Option 2: NLB (더 간단)

```bash
# 1. NLB 생성
aws elbv2 create-load-balancer \
  --name haproxy-nlb \
  --type network \
  --scheme internal \
  --subnets subnet-03dd67f1a36e1dbc8 subnet-0984f7255cf3af733 \
  --region ap-northeast-2

# 결과에서 LoadBalancerArn 확인

# 2. Target Group 생성
aws elbv2 create-target-group \
  --name haproxy-tg \
  --protocol TCP \
  --port 5432 \
  --vpc-id vpc-083e1cd1c3147138a \
  --health-check-protocol TCP \
  --health-check-port 5432 \
  --health-check-interval-seconds 30 \
  --healthy-threshold-count 3 \
  --unhealthy-threshold-count 3 \
  --region ap-northeast-2

# 결과에서 TargetGroupArn 확인

# 3. HAProxy 인스턴스 등록
aws elbv2 register-targets \
  --target-group-arn <TargetGroupArn> \
  --targets Id=i-0a9f5cb0853f7ab1b Id=i-0fd4d4154758e8cc5 \
  --region ap-northeast-2

# 4. Listener 생성
aws elbv2 create-listener \
  --load-balancer-arn <LoadBalancerArn> \
  --protocol TCP \
  --port 5432 \
  --default-actions Type=forward,TargetGroupArn=<TargetGroupArn> \
  --region ap-northeast-2

# 5. NLB DNS 확인
aws elbv2 describe-load-balancers \
  --load-balancer-arns <LoadBalancerArn> \
  --query 'LoadBalancers[0].DNSName' \
  --output text \
  --region ap-northeast-2
```

## 3. HAProxy 관리 명령어

### SSH 접속 (Session Manager)
```bash
# HAProxy-1 접속
aws ssm start-session --target i-0a9f5cb0853f7ab1b --region ap-northeast-2

# HAProxy-2 접속
aws ssm start-session --target i-0fd4d4154758e8cc5 --region ap-northeast-2
```

### HAProxy 상태 확인
```bash
# 서비스 상태
sudo systemctl status haproxy

# 설정 검증
sudo haproxy -c -f /etc/haproxy/haproxy.cfg

# 버전 확인
haproxy -v

# 로그 확인
sudo journalctl -u haproxy -f

# Stats 페이지 확인
curl http://localhost:8404
```

### HAProxy 설정 변경 (DB 전환 시)
```bash
# 1. 설정 파일 수정
sudo vi /etc/haproxy/haproxy.cfg

# 변경 내용:
# Oracle 비활성화, Aurora 활성화
server oracle 10.1.5.18:1521 check weight 0
server aurora <aurora-endpoint>:5432 check weight 100

# 2. 설정 검증
sudo haproxy -c -f /etc/haproxy/haproxy.cfg

# 3. 무중단 리로드
sudo systemctl reload haproxy

# 4. 상태 확인
curl http://localhost:8404
```

### 백엔드 테스트
```bash
# Oracle 연결 테스트
nc -zv 10.1.5.18 1521
telnet 10.1.5.18 1521

# HAProxy를 통한 연결 테스트
nc -zv localhost 5432
telnet localhost 5432

# Stats 확인
curl http://localhost:8404 | grep -A 5 "database"
```

## 4. 애플리케이션 설정

### Route53 사용 시
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://db-proxy.internal.local:5432/mesdb
    username: mesuser
    password: mespass
    driver-class-name: org.postgresql.Driver
```

### NLB 사용 시
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://<nlb-dns-name>:5432/mesdb
    username: mesuser
    password: mespass
    driver-class-name: org.postgresql.Driver
```

### 직접 IP 사용 (테스트)
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://10.1.1.132:5432/mesdb
    username: mesuser
    password: mespass
    driver-class-name: org.postgresql.Driver
```

## 5. 모니터링

### HAProxy Stats 페이지
```bash
# 브라우저에서 접속 (VPC 내부에서)
http://10.1.1.132:8404
http://10.1.1.181:8404

# 또는 curl로 확인
curl http://10.1.1.132:8404
```

### CloudWatch 메트릭 (NLB 사용 시)
```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/NetworkELB \
  --metric-name HealthyHostCount \
  --dimensions Name=TargetGroup,Value=<target-group-name> \
  --start-time 2025-12-03T00:00:00Z \
  --end-time 2025-12-03T23:59:59Z \
  --period 300 \
  --statistics Average \
  --region ap-northeast-2
```

## 6. 정리 (마이그레이션 완료 후)

```bash
# 1. NLB 삭제 (사용한 경우)
aws elbv2 delete-load-balancer --load-balancer-arn <LoadBalancerArn>
aws elbv2 delete-target-group --target-group-arn <TargetGroupArn>

# 2. Route53 레코드 삭제 (사용한 경우)
aws route53 delete-hosted-zone --id Z1234567890ABC

# 3. EC2 인스턴스 종료
aws ec2 terminate-instances \
  --instance-ids i-0a9f5cb0853f7ab1b i-0fd4d4154758e8cc5 \
  --region ap-northeast-2

# 4. 보안 그룹 삭제
aws ec2 delete-security-group \
  --group-id sg-0c4441fc57913af64 \
  --region ap-northeast-2
```

## 요약

### 생성된 리소스
- HAProxy EC2 인스턴스: 2대 (10.1.1.132, 10.1.1.181)
- 보안 그룹: haproxy-db-sg
- 포트: 5432 (DB), 8404 (Stats)

### 이중화 방식
- **Route53 Failover**: Primary/Secondary 자동 전환
- **NLB**: 자동 Health Check + 로드밸런싱

### 전환 방법
1. HAProxy 설정 변경 (weight 조정)
2. `sudo systemctl reload haproxy` (무중단)
3. Stats 페이지에서 확인
