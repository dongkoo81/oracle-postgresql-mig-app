# Terraform Infrastructure for Oracle to PostgreSQL Migration

## 개요

Oracle 19c 기반 MES 애플리케이션 실습 환경을 Terraform으로 자동 구성합니다.

## 🎯 실습 시나리오

### 사용자가 하는 것:
1. Terraform 코드 다운로드 (zip 또는 git clone)
2. `terraform apply` 실행
3. VSCode Remote SSH로 **on-premises-app** 접속
4. Oracle DB 초기화 및 애플리케이션 빌드/실행

### Terraform이 자동으로 하는 것:
- ✅ VPC + Subnets 생성
- ✅ Oracle 19c EC2 생성 (AMI 사용)
- ✅ 앱 서버 EC2 생성
- ✅ **소스코드 자동 clone** (https://github.com/dongkoo81/oracle-postgresql-migration.git)
- ✅ Java 17, Git, sqlplus, psql 설치
- ✅ SSH 키 생성

## 인프라 구성

### VPC
- **이름**: vpc-oracle-postgresql
- **CIDR**: 146.168.0.0/16
- **Public Subnets**: 3개 (146.168.0.0/24, 146.168.1.0/24, 146.168.2.0/24)
- **Private Subnets**: 3개 (146.168.10.0/24, 146.168.11.0/24, 146.168.12.0/24)
- **Internet Gateway**: 1개
- **NAT Gateway**: 1개 (Public Subnet 1에 배치)

### Security Group
- **이름**: sg-oracle-postgresql
- **규칙**: 모든 TCP 포트 허용 (0-65535)

### EC2 Instances (모두 Private Subnet에 배치)
1. **on-premises-app** (실습용 앱 서버)
   - AMI: Amazon Linux 2023 (최신)
   - Instance Type: r6i.xlarge
   - 자동 설치: Java 17, Git, sqlplus, psql
   - 자동 clone: 프로젝트 소스코드 → `/home/ec2-user/projects/oracle-apg-mig`
   - IAM Role: SSM 접근 권한
   - Subnet: private-subnet-1

2. **cloud-app**
   - AMI: Amazon Linux 2023 (최신)
   - Instance Type: r6i.xlarge
   - IAM Role: SSM 접근 권한
   - Subnet: private-subnet-2

3. **onprem-oracle-ee-19c**
   - AMI: ami-05b733ff1080c095f (Oracle EE 19c)
   - Instance Type: r6i.xlarge
   - IAM Role: SSM 접근 권한
   - Subnet: private-subnet-3

### Aurora PostgreSQL
- **클러스터명**: cloud-aurora-postgresql
- **엔진**: Aurora PostgreSQL 17.5
- **인스턴스**: db.r6i.xlarge (Writer 1대)
- **파라미터 그룹**: 
  - Cluster: cloud-aurora-postgresql-pg17
  - Instance: cloud-aurora-postgresql-instance-pg17
- **DB Subnet Group**: 3개 Private Subnet 사용
- **초기 데이터베이스**: mesdb
- **마스터 사용자**: postgres
- **백업 보관**: 7일

### SSH Key Pair
- Terraform이 자동으로 RSA 4096 키페어 생성
- Private Key 저장 위치: `terraform/keys/oracle-postgresql-key.pem`
- 모든 EC2 인스턴스에 동일한 키페어 적용

## 사전 요구사항

1. **Terraform 설치**
   ```bash
   # macOS
   brew install terraform

   # Linux
   wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
   unzip terraform_1.6.0_linux_amd64.zip
   sudo mv terraform /usr/local/bin/
   ```

2. **AWS CLI 설치 및 구성**
   ```bash
   aws configure
   # AWS Access Key ID, Secret Access Key, Region 입력
   ```

3. **AWS Session Manager Plugin 설치** (VSCode Remote-SSH용)
   - [설치 가이드](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html)

## 사용 방법

### 1. Terraform 초기화
```bash
cd terraform
terraform init
```

### 2. 실행 계획 확인
```bash
terraform plan
```

### 3. 인프라 배포
```bash
terraform apply
```

입력 프롬프트에서 `yes` 입력하여 배포 진행.

### 4. 배포 결과 확인
```bash
terraform output
```

출력 예시:
```
vpc_id = "vpc-xxxxx"
on_premises_app_private_ip = "10.0.10.123"
cloud_app_private_ip = "10.0.11.234"
onprem_oracle_private_ip = "10.0.12.345"
private_key_path = "terraform/keys/oracle-postgresql-key.pem"
```

### 5. VSCode Remote-SSH 설정
```bash
cd scripts
./setup-local-ssh.sh
```

이 스크립트는 자동으로:
- Terraform output에서 EC2 정보 추출
- `~/.ssh/config` 파일에 SSH 설정 추가
- SSM Session Manager를 ProxyCommand로 사용

### 6. EC2 접속 방법

#### 방법 1: SSH (터미널)
```bash
ssh on-premises-app
ssh cloud-app
ssh onprem-oracle
```

#### 방법 2: VSCode Remote-SSH (추천)
1. VSCode에서 `F1` 또는 `Cmd+Shift+P`
2. `Remote-SSH: Connect to Host...` 선택
3. `on-premises-app` 선택
4. 접속 후 `/home/ec2-user/projects/oracle-apg-mig` 폴더 열기

#### 방법 3: AWS SSM Session Manager (직접)
```bash
aws ssm start-session --target <instance-id> --region ap-northeast-2
```

---

## 📚 실습 가이드

### 1단계: VSCode로 on-premises-app 접속
```bash
# VSCode Remote-SSH로 접속 후
cd /home/ec2-user/projects/oracle-apg-mig
ls -la
```

### 2단계: Oracle DB 초기화
```bash
# Oracle 접속 정보 확인 (Terraform output에서)
# 예: 10.0.12.123:1521/oracle19c

# 1. 사용자 생성
sqlplus system/system@10.0.12.123:1521/oracle19c @sql/01_create_user.sql

# 2. 테이블 생성
sqlplus mesuser/mespass@10.0.12.123:1521/oracle19c @sql/schema/02_create_tables.sql

# 3. 프로시저/트리거 생성
sqlplus mesuser/mespass@10.0.12.123:1521/oracle19c @sql/procedures/03_create_procedures.sql

# 4. 샘플 데이터 삽입
sqlplus mesuser/mespass@10.0.12.123:1521/oracle19c @sql/data/04_insert_sample_data.sql
```

### 3단계: 애플리케이션 빌드
```bash
# Gradle 빌드
./gradlew clean build -x test
```

### 4단계: 애플리케이션 실행
```bash
# 백그라운드 실행
nohup java -jar build/libs/mes-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 로그 확인
tail -f app.log
```

### 5단계: 애플리케이션 접속
```
http://<on-premises-app-private-ip>:8080
```

브라우저에서 접속하려면 SSH 터널링 필요:
```bash
ssh -L 8080:localhost:8080 on-premises-app
# 로컬 브라우저에서 http://localhost:8080 접속
```

---

### 6. 서버 간 SSH 접속
Private Key를 사용하여 서버 간 접속:
```bash
# on-premises-app에서 cloud-app으로 접속
ssh -i /path/to/oracle-postgresql-key.pem ec2-user@<cloud-app-private-ip>

# on-premises-app에서 oracle로 접속
ssh -i /path/to/oracle-postgresql-key.pem ec2-user@<oracle-private-ip>
```

### 7. Aurora PostgreSQL 접속
```bash
# EC2에서 Aurora 접속
psql -h <aurora-endpoint> -U postgres -d mesdb

# 또는 connection string 사용
psql "postgresql://postgres:PostgresPass123!@<aurora-endpoint>:5432/mesdb"

# Terraform output에서 endpoint 확인
terraform output aurora_cluster_endpoint
terraform output -raw aurora_connection_string
```

### 8. 인프라 삭제
```bash
terraform destroy
```

## 파일 구조

```
terraform/
├── main.tf              # Provider, Key Pair 설정
├── variables.tf         # 변수 정의
├── vpc.tf              # VPC, Subnets, IGW, NAT
├── security.tf         # Security Group
├── iam.tf              # IAM Role (SSM)
├── ec2.tf              # EC2 Instances
├── rds.tf              # Aurora PostgreSQL
├── outputs.tf          # Output 값
├── userdata/
│   └── on-premises-app.sh   # on-premises-app 초기화 스크립트
├── scripts/
│   └── setup-local-ssh.sh   # 로컬 SSH 설정 스크립트
├── keys/               # 생성된 Private Key 저장 (terraform apply 후)
│   └── oracle-postgresql-key.pem
└── README.md
```

## 주요 특징

1. **Private Subnet 배치**: 모든 EC2가 Private Subnet에 있어 보안 강화
2. **NAT Gateway**: Private Subnet에서 외부 인터넷 접근 (패키지 설치 등)
3. **SSM Session Manager**: Public IP 없이 EC2 접속 가능
4. **자동 키페어 생성**: Terraform이 SSH 키페어 자동 생성 및 관리
5. **자동 소프트웨어 설치**: on-premises-app에 git, sqlplus, psql 자동 설치

## 비용 예상

- **NAT Gateway**: ~$0.045/hour (~$32/month)
- **EC2 r6i.xlarge**: ~$0.252/hour × 3 = ~$544/month
- **Aurora PostgreSQL db.r6i.xlarge**: ~$0.58/hour (~$420/month)
- **EBS gp3**: ~$0.08/GB/month
- **총 예상 비용**: ~$1,000-1,100/month

테스트 후 반드시 `terraform destroy`로 리소스 삭제하세요.

## 트러블슈팅

### SSM Session Manager 연결 실패
- EC2 인스턴스가 완전히 시작될 때까지 2-3분 대기
- IAM Role이 제대로 연결되었는지 확인
- AWS CLI 자격 증명 확인: `aws sts get-caller-identity`

### SSH 연결 실패
- Private Key 권한 확인: `chmod 400 keys/oracle-postgresql-key.pem`
- Session Manager Plugin 설치 확인
- `~/.ssh/config` 파일 확인

### userdata 스크립트 실행 확인
```bash
# SSM으로 접속 후
sudo cat /var/log/cloud-init-output.log
```

## 참고 자료

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [VSCode Remote-SSH](https://code.visualstudio.com/docs/remote/ssh)
